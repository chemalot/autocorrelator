/*
This file is part of the AutoCorrelator.

The AutoCorrelator is free software: you can redistribute it and/or
modify it under the terms of the GNU General Public License v3 as
published by the Free Software Foundation.

The AutoCorrelator is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License v3
along with the AutoCorrelator. If not, see <http://www.gnu.org/licenses/>.

*/
package autocorrelator.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import openeye.oechem.*;


/**
 *
 */

public class SdfTransformer
{
   private static final String EXPLAIN=
         "sdfTransformer [-makeHExplicit|makeHImplicit] -trans *.rea -in fn -out fn\n" +
         "  -trans .....file with \\n separated transformations\n" +
         "              or a list of space separated smirks\n" +
         "              or 'neutralize' which applies set of neutralizing transforms\n" +
         "  -scaffold . file with \\n separated list of scaffolds or list of space separated smiles\n" +
         "              R-Groups are marked with [U+n]\n" +
         "              the core will have attachment points with numbers increased by 100 so that the\n" +
         "              core is distinguishable from the R-Group in case of a single attachment point.\n" +
         "  -transformOnce Stop after first succesfull transformation\n" +
         "              Note: transformations always are applied to all sites\n" +
         "  -in.........input file (any OE filetype),  for sdtin use .type.\n" +
         "  -out........output file (any OE filetype), for stdout use .type.\n" +
         "\n";

   private static String neutralTrans = "[O,S,#7,#6;-1;!$(*[*+]):1]>>[*+0:1][H] [#7,#15;+1:1][H]>>[*;+0:1]";

   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = { "-makeHExplicit", "-makeHImplicit", "-transformOnce"};
      String[] parms    = {"-trans", "-in", "-out", "-scaffold"};
      String[] reqParms = {"-in", "-out" };

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);

      MyUniMolecularTransform[] reacts = getTransformations(cParser.getValue("-trans"));
      if(cParser.wasGiven("-scaffold"))
         reacts = getScaffolds(reacts,cParser.getValue("-scaffold"));
      if( reacts.length == 0 )
      {  System.err.println("Either -trans or -scaffold must be given");
         System.err.println(EXPLAIN);
         System.exit(1);
      }

      String in = cParser.getValue("-in");
      String out = cParser.getValue("-out");
      boolean makeHExplicit = cParser.wasGiven("-makeHExplicit")
                              || in.equals( "neutralize" );

      boolean makeHImplicit = cParser.wasGiven("-makeHImplicit");
      boolean transformOnce = cParser.wasGiven("-transformOnce");

      if( makeHExplicit && makeHImplicit )
      {  System.err.println("makeHImplicit may not be used with makeHExplicit");
         System.exit(1);
      }

      oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);

      StringBuilder reaNames = new StringBuilder(20);
      OEGraphMol mol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {
         if(makeHExplicit) oechem.OEAddExplicitHydrogens(mol);
         if(makeHImplicit) oechem.OESuppressHydrogens(mol, false, false, false);


         reaNames.setLength(0);
         // checking for the smi change is a workaround because oechem.constCall
         // was found not to always return true even when transforming correctly
         String inSmi = oechem.OECreateCanSmiString(mol);
         for(MyUniMolecularTransform rea : reacts)
            if( rea.trans.constCall(mol) || ! inSmi.equals(oechem.OECreateCanSmiString(mol)))
            {  String name = rea.name;
               if( name.length() > 0 ) reaNames.append(name).append(',');
               if( transformOnce )
                  break;
            }

         // remove last  ","
         if( reaNames.length() > 1 ) reaNames.setLength(reaNames.length()-1);

         if( reaNames.length() > 0 )
            oechem.OESetSDData(mol, "transformedBy", reaNames.toString());

         // delete hydrogens that where added by makeHExplicit because they can
         // cause wrong stereo perception in sdf file
         if(makeHExplicit) oechem.OESuppressHydrogens(mol);

         oechem.OEWriteMolecule(ofs, mol);
         mol.Clear();
      }
      ifs.close();
      ofs.close();
      for(MyUniMolecularTransform rea : reacts)
         rea.close();
   }

   /**
    * return array contaiing {@link MyUniMolecularTransform} from reacts plus those
    * found in fName
    * @param smilesOrFile either a space separated list of smiles or a filename cotinaing
    *              newline separated smiles. The smiles must contain [U+n] to mark the
    *              rGRoups.
    */
   private static MyUniMolecularTransform[] getScaffolds(MyUniMolecularTransform[] reacts, String smilesOrFile)
         throws IOException
   {  if( smilesOrFile == null )
         return reacts;

      List<MyUniMolecularTransform> reaList = new ArrayList<MyUniMolecularTransform>();
      reaList.addAll(Arrays.asList(reacts));

      if( smilesOrFile.contains("[U") )  // fName is a list of smirks not a file name
      {  String[] scaff = smilesOrFile.split("\\s");
         for(String s : scaff)
         {  OEUniMolecularRxn umr = new OEUniMolecularRxn(scaffoldToSmirks(s));
            if( ! umr.IsValid() )
            {  System.err.printf("Invalid scaffold: %s\n", s);
               continue;
            }
            if(umr != null) reaList.add(new MyUniMolecularTransform(umr, ""));
         }
      }else
      {  BufferedReader in =
            new BufferedReader(new FileReader(smilesOrFile));

         String line;
         Pattern rem = Pattern.compile("^\\s*#");
         while( (line = in.readLine()) != null)
         {  if(rem.matcher(line).find()) continue;
            line = line.trim();
            String[] val = line.split("\\s+",2);
            String scaff = val[0];
            String name = "";
            if( val.length == 2 ) name = val[1];

            OEUniMolecularRxn umr = new OEUniMolecularRxn(scaffoldToSmirks(scaff));
            if( ! umr.IsValid() )
            {  System.err.printf("Invalid transformation: %s\n", line);
               continue;
            }
            if(umr != null) reaList.add(new MyUniMolecularTransform(umr, name));
         }
         in.close();
      }

      return reaList.toArray(new MyUniMolecularTransform[reaList.size()]);
   }

   private static final int SMIFlags = OESMILESFlag.AtomStereo|OESMILESFlag.BondStereo
                                      |OESMILESFlag.AtomMaps  |OESMILESFlag.Isotopes;

   private static String scaffoldToSmirks(String scaffoldSmi)
   {  // Goal convert [U+1]c1nc([U+2])ncc1 to
      // [*:7][c:1]1[n:2][c:3]([*:8])[n:4][c:5][c:6]1
      //    >>  [U+101][c:1]1[n:2][c:3]([U+102])[n:4][c:5][c:6]1.[U+][*:7].[U+2][*:8];
      OEMolBase mol = new OEGraphMol();
      oechem.OEParseSmiles(mol, scaffoldSmi);
      oechem.OEAssignAromaticFlags(mol);
      if( ! mol.IsValid() || mol.NumAtoms() == 0 )
         System.err.println("Invalid smiles: " + scaffoldSmi);

      // get map of rgroup position to Map Index
      Map<Integer,OEAtomBase> rgPosToAtomMap = new HashMap<Integer,OEAtomBase>();
      OEAtomBaseIter atIt = mol.GetAtoms();
      int atMapIdx = 1;
      while(atIt.hasNext())
      {  OEAtomBase at = atIt.next();
         int charge = at.GetFormalCharge();        // charge is RGroup number

                  if( charge > 0 && at.GetAtomicNum() == OEElemNo.U )
         {  if( rgPosToAtomMap.containsKey(charge) )
               throw new Error(String.format("U+%d found multiple times", charge));

         // Increment charge so that core smiles contains charge + 100
            charge += 100;
            at.SetFormalCharge(charge);

            rgPosToAtomMap.put(charge,at);

         }else
         {  at.SetMapIdx(atMapIdx++);
            at.SetImplicitHCount(0);
         }
      }
      atIt.delete();


      StringBuilder productSmi = new StringBuilder();
      // initialize productSmiles with core smiles
      productSmi.append(oechem.OECreateSmiString(mol,SMIFlags));

      // loop over rgroups and append disconnected fragments to productSmiles
      for(Entry<Integer, OEAtomBase> posAtom : rgPosToAtomMap.entrySet())
      {  int pos = posAtom.getKey();
         OEAtomBase at = posAtom.getValue();
         at.SetAtomicNum(0);
         at.SetFormalCharge(0);
         at.SetMapIdx(atMapIdx);

         // decrement rgroup charge so that rgroup has correct numbering
         productSmi.append(".[U+").append(pos-100).append("][*:").append(atMapIdx).append(']');
         atMapIdx++;
      }

      String eductSmi = oechem.OECreateSmiString(mol,SMIFlags);
      String smirks = eductSmi + ">>" + productSmi;
      System.err.println("ScaffoldTransform: " + smirks);

      return smirks;
   }

   private static MyUniMolecularTransform[] getTransformations(String fName) throws IOException
   {  if( fName == null )
         return new MyUniMolecularTransform[0];

      List<MyUniMolecularTransform> reaList = new ArrayList<MyUniMolecularTransform>();

      if( "neutralize".equals(fName))
      {  fName = neutralTrans;
      }

      if( fName.contains(">>") )
      {  String[] trans = fName.split("\\s");
         for(String t : trans)
         {  OEUniMolecularRxn umr = new OEUniMolecularRxn(t);
            if( ! umr.IsValid() )
            {  System.err.printf("Invalid transformation: %s\n", t);
               continue;
            }
            if(umr != null) reaList.add(new MyUniMolecularTransform(umr, ""));
         }
      }else
      {  BufferedReader in =
            new BufferedReader(new FileReader(fName));

         String line;
         Pattern rem = Pattern.compile("^\\s*#");
         while( (line = in.readLine()) != null)
         {  if(rem.matcher(line).find()) continue;
            line = line.trim();
            String[] val = line.split("\\s+",2);
            String smirks = val[0];
            String name = "";
            if( val.length == 2 ) name = val[1];

            OEUniMolecularRxn umr = new OEUniMolecularRxn(smirks);
            if( ! umr.IsValid() )
            {  System.err.printf("Invalid transformation: %s\n", line);
               continue;
            }
            if(umr != null) reaList.add(new MyUniMolecularTransform(umr, name));
         }
         in.close();
      }

      return reaList.toArray(new MyUniMolecularTransform[reaList.size()]);
   }
}

class MyUniMolecularTransform
{  final OEUniMolecularRxn trans;
   final String name;

   MyUniMolecularTransform(OEUniMolecularRxn trans, String name)
   {  this.trans = trans;
      this.name = name;
   }

   void close() { trans.delete(); }
}