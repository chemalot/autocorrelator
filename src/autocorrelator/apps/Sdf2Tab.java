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
import java.util.regex.Pattern;

import openeye.oechem.*;

public class Sdf2Tab {

   private static final String EXPLAIN=
         "Sdf2Tab -in [-tags tag1|tag2|...] [-tagRE re] [-suppressHead]\n" +
         "-parseAll ....Parse through the complete input to determine tags.\n" +
         "              If not specified only the first record is used.\n" +
         "-tags.........specifies the tags to be used on output.\n" +
         "              if not specified the tags from the first record are used.\n" +
         "              use SMILES to output the isomeric smiles\n" +
         "              use TITLE to output the MOLFILE title\n" +
         "              If this is a readable fielname a newline separated list is read.\n" +
         "-tagRE........specifies the tags in the output by Regular expression. eg. '.*SMI'\n" +
         "              Note that only tags present in the first record are considered.\n" +
         "              If both -tags and -tagRE are given the -tags columns come first\n" +
         "-newLineReplacement replace new liness in values with given string\n" +
         "-skip ........skip the given number of records at the beginning of the input\n" +
         "-insertSmiles.insert smiles in first column, not used with -tags\n" +
         "-insertTitle..insert mofile title, not used with -tags\n" +
         "-suppressHead.do not print headers.\n" +
         "-in...........input file (any OE filetype), for sdtin use .type.\n" +
         "Output is to stdout\n"+
         "Tags must be in same order\n";

   private static final Pattern NEWLinePattern = Pattern.compile("[\r\n]+");
   private static String newLineReplacement;

   public static void main(String argv[]) throws IOException
   {  // Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      String[] modes    = {"-suppressHead", "-insertSmiles", "-insertTitle", "-parseAll" };
      String[] parms    = {"-in", "-tags", "-tagRE", "-skip", "-newLineReplacement" };
      String[] reqParms = {"-in"};
      List<String> tags = new ArrayList<String>();

      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      if( cParser.wasGiven("-tags") )
         readTags(cParser.getValue("-tags"), tags);


      newLineReplacement = cParser.getValue("-newLineReplacement");

      OEGraphMol mol = new OEGraphMol();

      oemolithread ifs = new oemolithread(cParser.getValue("-in"));
      if (cParser.wasGiven("-skip"))
      {  int skipCount = Integer.parseInt(cParser.getValue("-skip"));
         while(skipCount-- > 0)
            if( ! oechem.OEReadMolecule(ifs,mol) ) break;
      }

      boolean insertSmi = cParser.wasGiven("-insertSmiles");
      boolean insertTit = cParser.wasGiven("-insertTitle");
      if (cParser.wasGiven("-parseAll"))
         ifs = parseAll(ifs,tags,cParser.getValue("-tagRE"), insertSmi, insertTit);

      boolean headerPrinted = false;
      while (oechem.OEReadMolecule(ifs,mol))
      {  if( ! mol.IsValid() ) continue;

         if(! headerPrinted )
         {  if( cParser.wasGiven("-tagRE") && ! cParser.wasGiven("-parseAll"))
               tags = getSDTags(mol, tags, cParser.getValue("-tagRE"), insertSmi, insertTit);
            if( tags.size() == 0 ) // migth be null if not on command line and first record
               tags = getSDTags(mol, insertSmi, insertTit);
            if( ! cParser.wasGiven("-suppressHead") ) printHeaders(tags);
            headerPrinted = true;
         }

         if(tags.size()>0)
         {  for(int i=0; i<tags.size()-1; i++)
            {  String tag = tags.get(i);
               printField(mol, tag);
               System.out.print('\t');
            }

            // now print last tag without tab
            String tag = tags.get(tags.size()-1);
            printField(mol, tag);
            System.out.print("\n");
         }

      }
      ifs.close();
	}

   private static void readTags(String tagStr, List<String> tags) throws IOException
   {  if( new File(tagStr).canRead() )
         tags.addAll(Arrays.asList(readSeparatedFile(tagStr)));
      else
         tags.addAll(Arrays.asList(tagStr.split("\\|")));
   }


   private static String[] readSeparatedFile(String fName) throws IOException
   {  BufferedReader in = new BufferedReader(new FileReader(fName));
      List<String> tags = new ArrayList<String>();

      String line;
      while( (line = in.readLine()) != null )
      {  if( line.trim().length() == 0 ) continue;

         tags.add(line.trim());
      }
      in.close();

      return tags.toArray(new String[tags.size()]);
   }



   private static void printField(OEGraphMol mol, String tag)
   {  String val;
      if("SMILES".equals(tag))
      {  oechem.OE3DToInternalStereo(mol);
         oechem.OEAssignAromaticFlags(mol);
         val = oechem.OECreateIsoSmiString(mol);
      }
      else if( "TITLE".equals(tag))
         val = mol.GetTitle();
      else
         val = oechem.OEGetSDData(mol, tag);

      if( newLineReplacement != null )
         val = NEWLinePattern.matcher(val).replaceAll(newLineReplacement);

      System.out.print(val);
   }

   private static void printHeaders(List<String> tags)
   {  for(int i=0; i<tags.size()-1; i++)
         System.out.print(tags.get(i) + '\t');
      if(tags.size()>0) System.out.println(tags.get(tags.size()-1));
   }


   private static List<String> getSDTags(OEGraphMol mol,
                                     boolean insSmi, boolean insTit)
   {  ArrayList<String> tags = new ArrayList<String>();

      if( insSmi ) tags.add("SMILES");
      if( insTit ) tags.add("TITLE");

      for (OESDDataIter oiter = oechem.OEGetSDDataPairs(mol); oiter.hasNext(); )
      {  OESDDataPair dp = oiter.next();
         tags.add(dp.GetTag());
      }

      return tags;
   }

   /**
    * add tags from mol that match tagRE to the elemts in tags and returnt he new list.
    * @return
    */
   private static List<String> getSDTags(OEGraphMol mol, List<String> tags, String tagRE,
         boolean insSmi, boolean insTit)
   {  Set<String> tagSet = new LinkedHashSet<String>();
      if( tags != null ) tagSet.addAll(tags);
      if( insSmi ) tagSet.add("SMILES");
      if( insTit ) tagSet.add("TITLE");

      Pattern pat = Pattern.compile(tagRE);
      for (OESDDataIter oiter = oechem.OEGetSDDataPairs(mol); oiter.hasNext(); )
      {  OESDDataPair dp = oiter.next();
         String tag = dp.GetTag();
         if( pat.matcher(tag).matches() )
            tagSet.add(tag);
      }

      return new ArrayList<String>(tagSet);
   }



   private static oemolithread parseAll(oemolithread ifs, List<String> tags,
         String tagREStr, boolean insertSmi, boolean insertTit) throws IOException
   {  Set<String> tagSet = new LinkedHashSet<String>();
      if( tags != null ) tagSet.addAll(tags);
      if( insertSmi ) tagSet.add("SMILES");
      if( insertTit ) tagSet.add("TITLE");

      Pattern pat = null;
      if( tagREStr != null ) pat=Pattern.compile(tagREStr);

      File tmpFileName = File.createTempFile("s2tab", ".oeb");
      tmpFileName.deleteOnExit();

      OEGraphMol mol = new OEGraphMol();
      oemolothread ofs = new oemolothread(tmpFileName.getAbsolutePath());
      while(oechem.OEReadMolecule(ifs, mol))
      {  oechem.OEWriteMolecule(ofs, mol);

         OESDDataIter oiter = oechem.OEGetSDDataPairs(mol);
         while (oiter.hasNext() )
         {  OESDDataPair dp = oiter.next();
            String tag = dp.GetTag();
            if( pat == null || pat.matcher(tag).matches() )
               tagSet.add(tag);
         }
      }

      tags.addAll(tagSet);
      ifs.close(); ifs.delete();
      ofs.close(); ofs.delete();

      ifs = new oemolithread(tmpFileName.getAbsolutePath());
      return ifs;
   }

   public static void toTab(ArrayList<OEGraphMol> mollist)
   {
     boolean readHeader = false;
     for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
     {
       if ( readHeader == false )
       {  for (OESDDataIter oiter = oechem.OEGetSDDataPairs(iter.next());
               oiter.hasNext(); )
          {  OESDDataPair dp = oiter.next();
             System.out.print(dp.GetTag() + "\t");
             if (dp.GetTag().length() > 0) readHeader = true;
          }
          System.out.print("\n");
          break;
       }
     }
     for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
     {
       for (OESDDataIter oiter = oechem.OEGetSDDataPairs(iter.next());
               oiter.hasNext(); )
       {  OESDDataPair dp = oiter.next();
          String fld = dp.GetValue();
          if( newLineReplacement != null )
             fld = NEWLinePattern.matcher(fld).replaceAll(newLineReplacement);
          System.out.print(fld + "\t");
       }
       System.out.print("\n");
     }
   }
}
