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
-makeHExplicit -firstTransformation
-debug


 */
/**
 *
 */

public class SdfTransformer
{
   private static final String EXPLAIN=
         "sdfTransformer [-makeHExplicit|makeHImplicit] -trans *.rea -in fn -out fn\n" +
         "  -trans .... String containing SPACE separated 'transformations'\n" +
         "              Each 'transormation' can be either ending in .txt for a file\n" +
         "              containing \\n separated smirks' 'name\n" +
         "              or a .rxn file\n" +
         "              or a smirks\n" +
         "              or 'neutralize' which applies a set of neutralizing transforms\n" +
         "  -scaffold . String containing SPACE separated 'scaffolds'. Each 'scaffold' can be:\n" +
         "              a newline separeated .txt file containing smarts' 'name \n" +
         "                  with smarts containing [U+n] atoms marking attachement points\n" +
         "              or a mol file containing MDL query molecules with R groups marking attachement points\n" +
         "              or a smarts with [U+n] atoms marking attachement points\n" +
         "              the core will have attachment points with numbers increased by 10 so that the\n" +
         "              core is distinguishable from the R-Group in case of a single attachment point.\n" +
         "              Note: In Molfile the highest officially supported charge is 15.\n" +
         "  -firstTransformation If multiple transformations are specified with -trans/-scaffold\n" +
         "              stop after the first sucesssfull transformation.\n" +
         "              If not given further reactions are applied to the products of the first transformation.\n" +
         "  -singleReactionSite In molecules with multiple reactive sites: apply the transformations to each\n" +
         "              site individualy returning multiple single site products.\n" +
         "              The default is to exhaustivly apply each transformations to all sites\n" +
         "  -keepHydrogens Do not remove explicit H at the end.\n" +
         "  -in.........input file (any OE filetype),  for sdtin use .type.\n" +
         "  -out........output file (any OE filetype), for stdout use .type.\n" +
         "\n";

   private static String neutralTrans = "[O,S,#7,#6;-1;!$(*[*+]):1]>>[*+0:1][H] [#7,#15;+1:1][H]>>[*;+0:1]";

   private static boolean debug = false;

   private static MyTransFormFactory transFormFactory;

   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = { "-makeHExplicit", "-makeHImplicit", "-transformOnce", "-debug",
                            "-firstTransformation", "-singleReactionSite", "-keepHydrogens"};
      String[] parms    = {"-trans", "-in", "-out", "-scaffold"};
      String[] reqParms = {"-in", "-out" };

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      debug = cParser.wasGiven("-debug");


      transFormFactory = new MyTransFormFactory(cParser.wasGiven("-singleReactionSite"));

      String trans = cParser.getValue("-trans");
      MyTransform[] reacts = getTransformations(trans);
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
                              || (trans != null && trans.equals( "neutralize" ));

      boolean makeHImplicit = cParser.wasGiven("-makeHImplicit");
      // -transformOnce is deprecated use -firstTransformation
      boolean keepH = cParser.wasGiven("-keepHydrogens");
      // -transformOnce is deprecated use -firstTransformation
      boolean firstTransformation = cParser.wasGiven("-transformOnce");
      if( cParser.wasGiven("-firstTransformation") )
          firstTransformation = cParser.wasGiven("-firstTransformation");

      if( makeHExplicit && makeHImplicit )
      {  System.err.println("makeHImplicit may not be used with makeHExplicit");
         System.exit(1);
      }

      oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);

      Set<String> prodSet = new HashSet<String>();
      OEGraphMol mol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {
         if(makeHExplicit) oechem.OEAddExplicitHydrogens(mol);
         if(makeHImplicit) oechem.OESuppressHydrogens(mol, false, false, false);

         List<OEMolBase> reagents = new ArrayList<OEMolBase>(1);
         reagents.add(new OEGraphMol(mol));
         List<OEMolBase> prods = reagents;
         for(MyTransform rea : reacts)
         {  prods = transform(rea, reagents);
            if( firstTransformation && prods.size() > 0 )
               break;
            if( prods.size() > 0)
            {  for(OEMolBase reagent: reagents) reagent.delete();
               reagents = prods;
            } else
            {  prods = reagents;
            }
         }

         prodSet.clear();
         for( OEMolBase tmol: prods)
         {  correctValence(tmol);

            // delete hydrogens that where added by makeHExplicit because they can
            // cause wrong stereo perception in sdf file
            // OELibGen might add explicit hydrogens even when not in input or trnasformation
            // if(makeHExplicit)
            if( ! keepH )
            {  oechem.OESuppressHydrogens(tmol);
               // workaround for oe bug when creating new atoms with stereo
               // reported 3/2017 should be fixed in next version
               tmol.ClearCoords();
            }

            String smi = oechem.OEMolToSmiles(tmol);
            if( ! prodSet.contains(smi) )
            {  oechem.OEWriteMolecule(ofs, tmol);
               prodSet.add(smi);
            }
            tmol.delete();
         }
         mol.Clear();
      }
      ifs.close();
      ofs.close();
      for(MyTransform rea : reacts)
         rea.close();
   }


   private static List<OEMolBase> transform(MyTransform trans, List<OEMolBase> reagents)
   {  ArrayList<OEMolBase> prods = new ArrayList<OEMolBase>();
      for(OEMolBase mol : reagents)
      {  trans.setReagent(mol);
         while( trans.hasNext() )
         {  OEMolBase tmol = trans.next();
            String name = trans.getName();
            String reaStr = oechem.OEGetSDData(tmol, "transformedBy");
            if( reaStr.length() > 0 && name.length() > 0 ) reaStr += ',';
            if( name.length() > 0 )
               oechem.OESetSDData(tmol, "transformedBy", reaStr + name);
            prods.add(new OEGraphMol(tmol));
            tmol.delete();
         }
      }
      return prods;
   }


   private static MyTransform[] getTransformations(String smirksOrFNames) throws IOException
   {  if( smirksOrFNames == null )
         return new MyTransform[0];

      if( "neutralize".equals(smirksOrFNames))
      {  smirksOrFNames = neutralTrans;
      }
      List<MyTransform> reaList = new ArrayList<MyTransform>();

      String[] trans = smirksOrFNames.split("\\s");
      for(String t : trans)
      {  if( t.length() > 0)
         addTransform(t, reaList);
      }

      return reaList.toArray(new MyTransform[reaList.size()]);
   }


   private static void addTransform(String smirksOrFName, List<MyTransform> reaList ) throws IOException
   {  if( new File(smirksOrFName).canRead() )
      {  readTransform(smirksOrFName, reaList);
         return;
      }

      reaList.add(transFormFactory.create(smirksOrFName, ""));
   }

   private static void readTransform(String fName, List<MyTransform> reaList) throws IOException
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
         val[0] = val[0].trim();
         if( val[0].length() == 0 ) continue;
         String smirks = val[0];
         String name = "";
         if( val.length == 2 ) name = val[1];

         reaList.add(transFormFactory.create(smirks, name));
      }
      in.close();
   }


   private static void readRXN(String fName, List<MyTransform> reaList)
   {  oemolistream rFile = new oemolistream(fName);
      OEQMol reaction = new OEQMol();

      // reading reaction
      int opt = OEMDLQueryOpts.ReactionQuery;// | OEMDLQueryOpts.SuppressExplicitH;
      oechem.OEReadMDLReactionQueryFile(rFile, reaction, opt);
      rFile.close();
      rFile.delete();

      try
      {  reaList.add(transFormFactory.create(reaction, fName));
      }finally
      {  reaction.delete();
      }
   }

   /**
    * return array contaiing {@link MyUniMolecularTransform} from reacts plus those
    * found in fName
    * @param smartsOrFNames either a space separated list of smarts or filenames containing
    *              newline separated smiles. The smiles must contain [U+n] to mark the
    *              rGRoups.
    */
   private static MyTransform[] getScaffolds(MyTransform[] reacts, String smartsOrFNames)
         throws IOException
   {  if( smartsOrFNames == null )
         return reacts;

      List<MyTransform> reaList = new ArrayList<MyTransform>();
      reaList.addAll(Arrays.asList(reacts));

      String[] scaffold = smartsOrFNames.split("\\s");
      for(String s : scaffold)
      {  if(s.length() > 0)
            addScaffold(s, reaList);
      }
      return reaList.toArray(new MyUniMolecularTransform[reaList.size()]);
   }

   private static void addScaffold(String smartsOrFName, List<MyTransform> reaList ) throws IOException
   {  String fName = smartsOrFName.toLowerCase();
      if( fName.endsWith(".mol") || fName.endsWith(".txt"))
      {  readScaffold(smartsOrFName, reaList);
         return;
      }

      parseScaffold( smartsOrFName, reaList );
   }

   private static void parseScaffold(String smarts, List<MyTransform> reaList )
   {
      if( ! smarts.contains("[U") )
         throw new Error(String.format("Scaffold Smarts (%s) must contain at least one [U+]", smarts));


      MyTransform tr = transFormFactory.create(scaffoldToSmirks(smarts), "");
      reaList.add(tr);
   }


   private static void readScaffold(String fName, List<MyTransform> reaList ) throws IOException
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
         reaList.add(transFormFactory.create(smirks, name));
      }
      in.close();
   }

   /**
    * Read MDL query mol file and generate transformation.
    * Recognize Rn groups and supports query features.
    */
   private static void readMDLScaffold(String fName,
         List<MyTransform> reaList)
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

      /*
      OEGraphMol dummyMol = new OEGraphMol(reaction);
      boolean clearcoords = true;
      oedepict.OEPrepareDepiction(dummyMol, clearcoords);
      OE2DMolDisplay disp = new OE2DMolDisplay(dummyMol, opts);

      oedepict.OERenderMolecule("reaction.svg", disp);
      /**/


      OEQMol qMol = new OEQMol();
      oechem.OEBuildMDLQueryExpressions(qMol,reaction);

      try
      {  reaList.add(transFormFactory.create(qMol, fName));
      }finally
      {  qMol.delete();
         reaction.delete();
      }
   }


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


   private static OEMolBase correctValence(OEMolBase mol)
   {
      OEAtomBaseIter ai = mol.GetAtoms();

      while (ai.hasNext()) {
         OEAtomBase at = ai.next();
         if (!(at.IsNitrogen() || at.IsOxygen() || at.IsSulfur() || at.IsCarbon()))
            continue;

         if (at.IsNitrogen()) {
            correctValence(at, 3);
         }
         if (at.IsOxygen()) {
            correctValence(at, 2);
         }
         if (at.IsCarbon()) {
            correctValence(at, 4);
         }
         if (at.IsSulfur()) {
            int d = at.GetDegree();
            int v = at.GetValence();

            switch (d - v) {
            case 0: correctValence(at, 2); break;
            case 1: correctValence(at, 4); break;
            case 2: correctValence(at, 6); break;
            default: break;
            }

         }
      }

      ai.delete();

      return mol;
   }

   public static void correctValence(OEAtomBase atom, int targetValence)
   {
      int formalCharge = atom.GetFormalCharge();
      int nvalence = atom.GetValence();
      int nexplicitH = atom.GetExplicitHCount();
      int nimplicitH =atom.GetImplicitHCount();
      int valence = nvalence - formalCharge;

      if (valence == targetValence)
         return;
      if (valence < targetValence) {
         atom.SetImplicitHCount(Math.abs(nimplicitH + targetValence - valence));
      }
      else {
         int correctedImplicitHcount = Math.max(0,  targetValence - valence + nimplicitH);
         atom.SetImplicitHCount(correctedImplicitHcount);
         valence = atom.GetExplicitValence() + correctedImplicitHcount;
         if (valence > targetValence && nexplicitH >= valence - targetValence) {
            RemoveExplicitHydrogen(atom, valence - targetValence);
         }
      }
   }


   private static void RemoveExplicitHydrogen(OEAtomBase atom, int nremove)
   {
      OEMolBase mol = atom.GetParent();
      OEAtomBaseIter aiter = atom.GetAtoms();
      while (aiter.hasNext()) {
         OEAtomBase at = aiter.next();
         if (at.IsHydrogen()) {
            mol.DeleteAtom(at);
            nremove--;
            if (nremove == 0)
               break;
         }
      }
      aiter.delete();
   }

}


class MyTransFormFactory
{  private final boolean singleReactionSite;

   /**
    * @param singleReactionSite if true and the educt has multiple reative sites
    *        the transformatin will return one single site products for each
    *        matching reactive site.
    *        If false, all matchng sites are transformed exhaustivly.
    */
   public MyTransFormFactory(boolean singleReactionSite)
   {  this.singleReactionSite = singleReactionSite;
   }

   MyTransform create(String smirks, String name)
   {  if( singleReactionSite )
         return new MyOELibTransform(smirks,name);
      return new MyUniMolecularTransform(smirks, name);

   }

   MyTransform create(OEQMol qMol, String name)
   {  if( singleReactionSite )
         return new MyOELibTransform(qMol,name);
      return new MyUniMolecularTransform(qMol, name);
   }
}



interface MyTransform extends Iterator<OEMolBase>, Closeable
{  String getName();
   /**
    * @param mol inptu and output moecule for transformation
    */
   void setReagent(OEMolBase mol);
}



class MyOELibTransform implements MyTransform
{  private final String name;
   private final OELibraryGen libGen;
   private OEMolBaseIter prodIt = null;

   MyOELibTransform(String smirks, String name)
   {  this.libGen = new OELibraryGen(smirks);
      if( ! libGen.IsValid() )
         throw new IllegalArgumentException(name);
      if( libGen.NumReactants() != 1 )
         throw new IllegalArgumentException(
               String.format("Reaction (%s) has %d reagends instead of just 1",
                             name, libGen.NumReactants()));
      libGen.SetValenceCorrection(true);
      this.name = name;
   }


   MyOELibTransform(OEQMol qMol, String name)
   {  this.libGen = new OELibraryGen();
      if( ! libGen.Init(qMol) && ! libGen.IsValid() )
         throw new IllegalArgumentException(name);
      if( libGen.NumReactants() != 1 )
         throw new IllegalArgumentException(
               String.format("Reaction (%s) has %d reagends instead of just 1",
                             name, libGen.NumReactants()));
      libGen.SetValenceCorrection(true);
      this.name = name;
   }



   @Override
   public void close()
   {  if( prodIt != null )
      {  prodIt.delete();
         prodIt = null;
      }
      libGen.delete();
   }

   @Override
   public String getName() { return name; }



   /** reset transformation with new reagent **/
   @Override
   public void setReagent(OEMolBase mol)
   {  libGen.SetStartingMaterial(mol, 0);
      if( prodIt != null )
      {  prodIt.delete();
         prodIt = null;
      }
      prodIt = libGen.GetProducts();
   }

   @Override
   public boolean hasNext()
   {  return prodIt.hasNext();
   }

   @Override
   public OEMolBase next()
   {  if( ! hasNext() )
         throw new NoSuchElementException();

      return prodIt.next();
   }
}

class MyUniMolecularTransform implements MyTransform
{  private final OEUniMolecularRxn trans;
   private final String name;
   private boolean hasNext = false;
   private OEGraphMol mol = null;
   private String molSmi;

   MyUniMolecularTransform(String smirks, String name)
   {  this.trans = new OEUniMolecularRxn(smirks, true);
      if( ! trans.IsValid() )
         throw new IllegalArgumentException(name);
      this.name = name;
   }

   MyUniMolecularTransform(OEQMol qMol, String name)
   {  this.trans = new OEUniMolecularRxn();
      if( ! trans.Init(qMol) && ! trans.IsValid() )
         throw new IllegalArgumentException(name);
      this.name = name;
   }



   @Override
   public void close()
   {  if( mol != null ) mol.delete();
      mol = null;
      trans.delete();
   }

   @Override
   public String getName() { return name; }



   /** reset transformation with new reagent **/
   @Override
   public void setReagent(OEMolBase mol)
   {  if( this.mol != null ) this.mol.delete();
      this.mol = new OEGraphMol(mol);
      molSmi   = oechem.OECreateCanSmiString(mol);
      // checking for the smi change is a workaround because oechem.constCall
      // was found not to always return true even when transforming correctly
      hasNext  = trans.constCall(this.mol)
                 || ! molSmi.equals(oechem.OECreateCanSmiString(this.mol));
   }

   @Override
   public boolean hasNext()
   {  return hasNext;
   }

   @Override
   public OEMolBase next()
   {  if( ! hasNext )
         throw new NoSuchElementException();
      hasNext = false;
      return new OEGraphMol(mol);
   }
}