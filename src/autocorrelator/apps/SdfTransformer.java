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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import openeye.oechem.*;
import openeye.oedepict.*;

/* Test config in eclipse:
 *
-in ${workspace_loc:autocorrelator}/src/autocorrelator/apps/doc-files/r.csv
-out t.o.sdf
-trans "${workspace_loc:autocorrelator}/src/autocorrelator/apps/doc-files/r.rxn [*:1][c:2]1[#6,#7:3][c:4]([S:5][C:6][C:7]2)[c:8]2[c:9]([*:10])[n:11]1>>[U+101][c:2]1[#6,#7:3][c:4]([S:5][C:6][C:7]2)[c:8]2[c:9]([U+102])[n:11]1.[*:1][U+1].[*:10][U+2] ${workspace_loc:autocorrelator}/src/autocorrelator/apps/doc-files/r.txt"
-scaffold "${workspace_loc:autocorrelator}/src/autocorrelator/apps/doc-files/s.txt ${workspace_loc:autocorrelator}/src/autocorrelator/apps/doc-files/r.mol"
-makeHExplicit -transformOnce
-debug


 */
/**
 *
 */

public class SdfTransformer
{
   private static final String EXPLAIN=
         "sdfTransformer [-makeHExplicit|makeHImplicit] -trans *.rea -in fn -out fn\n" +
         "  -trans .... String containing space separated 'transformations'\n" +
         "              Each 'transormation' can be either ending in .txt for a file\n" +
         "              containing \\n separated smirks;\n" +
         "              or a .rxn file\n" +
         "              or a smirks\n" +
         "              or 'neutralize' which applies a set of neutralizing transforms\n" +
         "  -scaffold . String containing space separated 'scaffolds'. Each 'scaffold' can be:\n" +
         "              or a txt file containing smarts strings with [U+n] atoms marking attachement points\n" +
         "              or a mol file containing MDL query molecules with R groups marking attachement points\n" +
         "              or a smarts with [U+n] atoms marking attachement points\n" +
         "              the core will have attachment points with numbers increased by 10 so that the\n" +
         "              core is distinguishable from the R-Group in case of a single attachment point.\n" +
         "              Note: In Molfile the highest officially supported charge is 15.\n" +
         "  -transformOnce Stop after first succesfull transformation\n" +
         "              Note: transformations always are applied to all sites\n" +
         "  -in.........input file (any OE filetype),  for sdtin use .type.\n" +
         "  -out........output file (any OE filetype), for stdout use .type.\n" +
         "\n";

   private static String neutralTrans = "[O,S,#7,#6;-1;!$(*[*+]):1]>>[*+0:1][H] [#7,#15;+1:1][H]>>[*;+0:1]";

   private static boolean debug = false;

   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = { "-makeHExplicit", "-makeHImplicit", "-transformOnce", "-debug"};
      String[] parms    = {"-trans", "-in", "-out", "-scaffold"};
      String[] reqParms = {"-in", "-out" };

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      debug = cParser.wasGiven("-debug");

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


   private static MyUniMolecularTransform[] getTransformations(String smirksOrFNames) throws IOException
   {  if( smirksOrFNames == null )
         return new MyUniMolecularTransform[0];

      List<MyUniMolecularTransform> reaList = new ArrayList<MyUniMolecularTransform>();

      String[] trans = smirksOrFNames.split("\\s");
      for(String t : trans)
      {  addTransform(t, reaList);
      }

      return reaList.toArray(new MyUniMolecularTransform[reaList.size()]);
   }


   private static void addTransform(String smirksOrFName, List<MyUniMolecularTransform> reaList ) throws IOException
   {  if( new File(smirksOrFName).canRead() )
      {  readTransform(smirksOrFName, reaList);
         return;
      }

      parseTransform( smirksOrFName, reaList );
   }

   private static void parseTransform(String smirks, List<MyUniMolecularTransform> reaList )
   {  if( "neutralize".equals(smirks))
      {  smirks = neutralTrans;
      }


      OEUniMolecularRxn umr = new OEUniMolecularRxn(smirks);
      if( ! umr.IsValid() )
      {  System.err.printf("Invalid transformation: %s\n", smirks);
         return;
      }
      if(umr != null) reaList.add(new MyUniMolecularTransform(umr, ""));
   }

   private static void readTransform(String fName, List<MyUniMolecularTransform> reaList) throws IOException
   {  if( fName.toLowerCase().endsWith("rxn") )
      {  readRXN(fName,reaList);
         return;
      }

      // all but .rxn files are assuemd to be files contianing smirks <space> name
      BufferedReader in =
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


   private static void readRXN(String fName, List<MyUniMolecularTransform> reaList)
   {  oemolistream rFile = new oemolistream(fName);
      OEQMol reaction = new OEQMol();

      // reading reaction
      int opt = OEMDLQueryOpts.ReactionQuery;// | OEMDLQueryOpts.SuppressExplicitH;
      oechem.OEReadMDLReactionQueryFile(rFile, reaction, opt);
      rFile.close();
      rFile.delete();

      OEUniMolecularRxn umr = new OEUniMolecularRxn();
      if (!umr.Init(reaction))
      {  System.err.println("Failed to initialize reaction with :" + fName);
         return;
      }

      reaList.add(new MyUniMolecularTransform(umr, fName));
   }

   /**
    * return array contaiing {@link MyUniMolecularTransform} from reacts plus those
    * found in fName
    * @param smartsOrFNames either a space separated list of smarts or filenames containing
    *              newline separated smiles. The smiles must contain [U+n] to mark the
    *              rGRoups.
    */
   private static MyUniMolecularTransform[] getScaffolds(MyUniMolecularTransform[] reacts, String smartsOrFNames)
         throws IOException
   {  if( smartsOrFNames == null )
         return reacts;

      List<MyUniMolecularTransform> reaList = new ArrayList<MyUniMolecularTransform>();
      reaList.addAll(Arrays.asList(reacts));

      String[] scaffold = smartsOrFNames.split("\\s");
      for(String s : scaffold)
      {  addScaffold(s, reaList);
      }
      return reaList.toArray(new MyUniMolecularTransform[reaList.size()]);
   }

   private static void addScaffold(String smartsOrFName, List<MyUniMolecularTransform> reaList ) throws IOException
   {  String fName = smartsOrFName.toLowerCase();
      if( fName.endsWith(".mol") || fName.endsWith(".txt"))
      {  readScaffold(smartsOrFName, reaList);
         return;
      }

      parseScaffold( smartsOrFName, reaList );
   }

   private static void parseScaffold(String smarts, List<MyUniMolecularTransform> reaList )
   {
      if( ! smarts.contains("[U") )
         throw new Error("Scaffold Smarts must contain at least one [U+]");


      OEUniMolecularRxn umr = new OEUniMolecularRxn(scaffoldToSmirks(smarts));
      if( ! umr.IsValid() )
      {  System.err.printf("Invalid scaffold: %s\n", smarts);
            return;
      }
      if(umr != null) reaList.add(new MyUniMolecularTransform(umr, ""));
   }


   private static void readScaffold(String fName, List<MyUniMolecularTransform> reaList ) throws IOException
   {  if( fName.toLowerCase().endsWith(".mol") )
      {  readMDLScaffold(fName, reaList );
         return;
      }

      if(! fName.toLowerCase().endsWith(".txt") )
          throw new Error("scaffolds can be only in .mol or .txt format: " + fName);

      BufferedReader in =
         new BufferedReader(new FileReader(fName));

      String line;
      Pattern rem = Pattern.compile("^\\s*#");
      while( (line = in.readLine()) != null)
      {  if(rem.matcher(line).find()) continue;
         line = line.trim();
         String[] val = line.split("\\s+",2);
         String scaff = val[0];
         String name = "";
         if( val.length == 2 ) name = val[1];

         String smirks = scaffoldToSmirks(scaff);
         OEUniMolecularRxn umr = new OEUniMolecularRxn(smirks);
//         OEUniMolecularRxn umr = new OEUniMolecularRxn("[R1:1][c:2]1[#6,#7:3][c:4]([C:5][C:6][C:7]2)[c:8]2[c:9]([R2:10])[n:11]1>>[R1:1][c:2]1[#6,#7:3][c:4]([C:5][C:6][C:7]2)[c:8]2[c:9]([R2:10])[n:11]1");
         if( ! umr.IsValid() )
         {  System.err.printf("Invalid scaffold based transformation: %s\n   %s\n", line, smirks);
            continue;
         }
         if(umr != null) reaList.add(new MyUniMolecularTransform(umr, name));
      }
      in.close();
   }

   /**
    * Read MDL query mol file and generate transformation.
    * Recognize Rn groups and supports query features.
    */
   private static void readMDLScaffold(String fName,
         List<MyUniMolecularTransform> reaList)
   {  oemolistream ifs = new oemolistream(fName);
      if(! ifs.IsValid() ) throw new IOError(new Error("Error reading " + fName));
      OEGraphMol mol = new OEGraphMol();
      oechem.OEReadMDLQueryFile(ifs, mol);
      ifs.close();
      ifs.delete();

      // add mapping indexes
      OEAtomBaseIter atIt = mol.GetAtoms();
      while( atIt.hasNext() )
      {  OEAtomBase at = atIt.next();
         at.SetMapIdx(at.GetIdx()+1);
      }
      atIt.delete();

      // init reaction by duplicating input core
      OEGraphMol reaction = new OEGraphMol(mol);
      oechem.OEAddMols(reaction, mol);

      int parts[] = new int[reaction.GetMaxAtomIdx()];
      oechem.OEDetermineComponents(reaction, parts);
      OEPartPredAtom pred = new OEPartPredAtom(parts);

      reaction.SetRxn(true);

      // assign reactant atoms
      pred.SelectPart(1);
      atIt = reaction.GetAtoms(pred);
      while(atIt.hasNext())
         atIt.next().SetRxnRole(OERxnRole.Reactant);
      atIt.delete();

      // assign product atoms
      pred.SelectPart(2);
      atIt = reaction.GetAtoms(pred);
      while(atIt.hasNext())
         atIt.next().SetRxnRole(OERxnRole.Product);
      atIt.delete();

      // look for R groups and add U atoms
      atIt = reaction.GetAtoms(new OEAtomIsInProduct());
      while(atIt.hasNext())
      {  OEAtomBase rAt = atIt.next();
         if(rAt.GetAtomicNum() == 0 && rAt.GetMapIdx() != 0)
         {  if( rAt.HasData("MDLQueryAtomType") ) continue;

            String rName = rAt.GetName();
            if( ! rName.startsWith("R") || rName.length() == 1) continue;
            int rNum = Integer.parseInt(rName.substring(1));

            OEBondBase bd = oechem.OEGetSoleSingleBond(rAt); // ???
            if( bd == null ) continue;

            // delete bond to R
            OEAtomBase coreAt = bd.GetNbr(rAt);
            reaction.DeleteBond(bd);

            // create new U atom, link to core and increment mapping
            OEAtomBase cureU = reaction.NewAtom(OEElemNo.U);
            //printAtomInfo(rAt);
            cureU.SetFormalCharge(rNum+10);
            cureU.SetIntData("MDLQueryAtomCharge", rNum+10);
            cureU.SetRxnRole(OERxnRole.Product);
            reaction.NewBond(coreAt, cureU,1);   // Just single???

            OEAtomBase rU = reaction.NewAtom(OEElemNo.U);
            reaction.NewBond(rU, rAt, 1);
            rU.SetRxnRole(OERxnRole.Product);
            rU.SetFormalCharge(rNum);
            rU.SetIntData("MDLQueryAtomCharge", rNum);
         }
      }
      atIt.delete();

      OE2DMolDisplayOptions opts = new OE2DMolDisplayOptions(600, 300, OEScale.AutoScale);
      opts.SetAtomPropertyFunctor(new OEDisplayAtomMapIdx());
      opts.SetTitleLocation(OETitleLocation.Hidden);

      boolean clearcoords = true;
      oedepict.OEPrepareDepiction(reaction, clearcoords);
      OE2DMolDisplay disp = new OE2DMolDisplay(reaction, opts);

      oedepict.OERenderMolecule("reaction.svg", disp);

      OEQMol qMol = new OEQMol();
      oechem.OEBuildMDLQueryExpressions(qMol,reaction);

      try
      {  OEUniMolecularRxn umr = new OEUniMolecularRxn();
         if(!umr.Init(qMol))
            throw new Error("Invalid qMol: " + fName);
//         if(!umr.Init("[*:1][c:2]1[#6,#7:3][c:4]([N:5][C:6][C:7]2)[c:8]2[c:9]([*:10])[n:11]1>>[U+101][c:2]1[#6,#7:3][c:4]([N:5][C:6][C:7]2)[c:8]2[c:9]([U+102])[n:11]1.[*:1][U+1].[*:10][U+2]"))
//            throw new Error("Invalid qMol: " + fName);

         if( ! umr.IsValid() )
         {  throw new Error("Invalid qMol: " + fName);
         }
         if(umr != null) reaList.add(new MyUniMolecularTransform(umr, fName));
      }finally
      {  qMol.delete();
         reaction.delete();
      }
   }


   /**
    *  Python code to do the smae thing from MDL query mol
    *
     ifs = oemolistream(sys.argv[1])
     mol = OEGraphMol()
     OEReadMDLQueryFile(ifs, mol)

     for atom in mol.GetAtoms():
         atom.SetMapIdx(atom.GetIdx()+1)

     reaction = OEGraphMol(mol)
     OEAddMols(reaction, mol)
     count, parts = OEDetermineComponents(reaction)
     pred = OEPartPredAtom(parts)

     reaction.SetRxn(True)

     pred.SelectPart(1)
     for atom in reaction.GetAtoms(pred):
         atom.SetRxnRole(OERxnRole_Reactant)
     pred.SelectPart(2)
     for atom in reaction.GetAtoms(pred):
         atom.SetRxnRole(OERxnRole_Product)

     for atom in reaction.GetAtoms(OEAtomIsInProduct()):
         if atom.GetAtomicNum() == 0 and atom.GetMapIdx() != 0:
             if atom.HasData("MDLQueryAtomType"):
                 continue
             bond = OEGetSoleSingleBond(atom)
             if bond is None:
                 continue
             print (bond)
             nbr = OEGetSoleNeighbor(atom)
             if nbr is None:
                 continue
             print (nbr)
             reaction.DeleteBond(bond)
             newatom = reaction.NewAtom(OEElemNo_U)
             newatom.SetIsotope(atom.GetIdx())
             newatom.SetRxnRole(OERxnRole_Product)
             reaction.NewBond(newatom, nbr, 1)

             newatom = reaction.NewAtom(0)
             reaction.NewBond(newatom, atom, 1)
             newatom.SetRxnRole(OERxnRole_Product)
             newatom.SetData("MDLQueryAtomType", 2) // any atom
             atom.SetAtomicNum(OEElemNo_U)


     opts = OE2DMolDisplayOptions(600, 300, OEScale_AutoScale)
     opts.SetAtomPropertyFunctor(OEDisplayAtomMapIdx())
     opts.SetTitleLocation(OETitleLocation_Hidden)

     clearcoords = True
     OEPrepareDepiction(reaction, clearcoords)
     disp = OE2DMolDisplay(reaction, opts)

     OERenderMolecule("reaction.svg", disp)
  */


   @SuppressWarnings("unused")
   private static void printAtomInfo(OEAtomBase rAt)
   {  System.err.println("Name: " + rAt.GetName()  );
      OEBaseDataIter dIt = rAt.GetDataIter();
      while( dIt.hasNext())
      {  OEBaseData d = dIt.next();
         System.err.println(d.GetTag() + " " + d.GetDataType() + " " + d.toString());
      }
      dIt.delete();
   }

   private static final Pattern CHARGEPat = Pattern.compile("\\d+");


   private static String scaffoldToSmirks(String scaffoldSma)
   {  // Goal convert [U+1]c1[n,c]c([U+2])ncc1 to
      // [*:7][c:1]1[n,c:2][c:3]([*:8])[n:4][c:5][c:6]1
      //    >>  [U+101][c:1]1[n,c:2][c:3]([U+102])[n:4][c:5][c:6]1.[U+][*:7].[U+2][*:8];

      StringBuilder left = new StringBuilder(scaffoldSma.length()*5);
      StringBuilder right = new StringBuilder(scaffoldSma.length()*5);
      StringBuilder rGrp  = new StringBuilder();

      int mapIdx = 1;
      int cPos=0;
      boolean attachmentFound = false;
      while(cPos < scaffoldSma.length())
      {  String at = getNextAtom(scaffoldSma,cPos);
         if( at.startsWith("[") )
         {  if( at.charAt(1) != 'U' )
            {  left.append(at.substring(0,at.length()-1))
                   .append(':').append(mapIdx).append(']');
               right.append(at.substring(0,at.length()-1))
                    .append(':').append(mapIdx++).append(']');
            }else
            {  int charge = 1;
               if( at.contains("++") ) charge++;
               Matcher mat = CHARGEPat.matcher(at);
               if( mat.find() ) charge = Integer.parseInt(mat.group());

               left.append("[*:") .append(mapIdx)  .append(']');
               right.append("[U+").append(charge+10).append(']');
               rGrp.append(".[*:").append(mapIdx++)  .append(']')
                   .append("[U+") .append(charge)    .append(']');

               attachmentFound = true;
            }
         }else
         {  left .append('[').append(at).append(':').append(mapIdx).append(']');
            right.append('[').append(at).append(':').append(mapIdx++).append(']');
         }

         cPos += at.length();

         if( cPos >= scaffoldSma.length() ) return "";

         String oth = getNextOther(scaffoldSma,cPos);
         left .append(oth);
         right.append(oth);
         cPos += oth.length();
      }

      left.append(">>").append(right).append(rGrp);
      if(! attachmentFound )
         System.err.printf("Warning: No U atoms found in scaffold: %s\n   %s\n",
               scaffoldSma, left);

      if( debug ) System.err.println(left);

      return left.toString();
   }


   private static String getNextAtom(String scaffoldSma, int cPos)
   {  StringBuilder sb = new StringBuilder();

      if(scaffoldSma.charAt(cPos) == '[' )
      {  int nBrackets = 1;
         while(nBrackets > 0)
         {  sb.append(scaffoldSma.charAt(cPos++));
            if( scaffoldSma.charAt(cPos) == ']' ) nBrackets--;
            else if( scaffoldSma.charAt(cPos) == '[' ) nBrackets++;
         }
         sb.append(scaffoldSma.charAt(cPos));
         return sb.toString();
      }

      int len = 1;
      if(    scaffoldSma.regionMatches(true, cPos, "br", 0, 2)
          || scaffoldSma.regionMatches(true, cPos, "cl", 0, 2) )
         len = 2;

      return scaffoldSma.substring(cPos, cPos+len );
   }


   private static String getNextOther(String scaffoldSma, int cPos)
   {  if(   Character.isLetter(scaffoldSma.charAt(cPos))
         || scaffoldSma.charAt(cPos) == '[' ) return "";

      StringBuilder sb = new StringBuilder();
      while( cPos < scaffoldSma.length()
            && ! Character.isLetter(scaffoldSma.charAt(cPos) )
            && scaffoldSma.charAt(cPos) != '[' )
            sb.append(scaffoldSma.charAt(cPos++));

      return sb.toString();
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