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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import openeye.oechem.*;



/**
 * The Load class uses DataLoader.load() to load data into ORACLE tables.
 * It is used in command-line mode.
 */

public class SdfSdfMerger
{  private static final String EXPLAIN=
         "sdfSdfMerger -master fname -second fName -out fName -masterTag tagName -secondTag name\n" +
         "  Will merge the data from the second file into the first file based on the common\n" +
         "  value, the second file is read into memory.\n" +
         "  All entries in the -second file not matching tags in the master are appended to the output\n\n" +
         "\t-master......input sdf file (any OE filetype), for sdtin use .type\n" +
         "\t             no tag values of the master will be overwritten.\n" +
         "\t-second......input sdf file, for sdtin use .type\n" +
         "\t             The data is read into memory.\n" +
         "\t-out.........output file (any OE filetype), for stdout use .type.\n" +
         "\t-masterTag...name of tag in master file to identify records to join.\n" +
         "\t             'title' may be used to use the sdf-title for merging.\n" +
         "\t-secondTag...name of tag in second file to identify records to join.\n\n";

   private final String secondTag;
   private final Map<String,OEGraphMol> secondRecords = new HashMap<String, OEGraphMol>();
   private final HashSet<String> secondMatched;

   public SdfSdfMerger(String secondFile, String secondTag) throws IOException
   {  this.secondTag = secondTag;
      parseSecondFile(secondFile);
      secondMatched = new HashSet<String>(secondRecords.size());
   }

   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = { };
      String[] parms    = { "-master", "-second", "-out", "-masterTag", "-secondTag" };
      String[] reqParms = { "-master", "-second", "-out", "-masterTag", "-secondTag" };

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);

      String secondFile = cParser.getValue("-second");
      String secondTag = cParser.getValue("-secondTag");
      String masterTag = cParser.getValue("-masterTag");
      String in = cParser.getValue("-master");
      String out = cParser.getValue("-out");

      SdfSdfMerger merger = new SdfSdfMerger(secondFile, secondTag);
      merger.merge(masterTag,in, out);
   }


   private void merge(String masterTag, String in, String out)
   {  oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);

      OEGraphMol mol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {  String mergeValue;
         if(masterTag.equals("title"))
            mergeValue = mol.GetTitle();
         else
            mergeValue = oechem.OEGetSDData(mol,masterTag);

         if(mergeValue.length() == 0)
         {  System.err.printf("Warning: No value for mergeTag in sdfRecord (%s)\n", mol.GetTitle());
         }else
         {  OEGraphMol secondMol = secondRecords.get(mergeValue);
            if(secondMol == null)
            {  System.err.printf("Warning: No entry in second file for: %s\n", mergeValue);
            }else
            {  OESDDataIter sVals = oechem.OEGetSDDataPairs(secondMol);
               while( sVals.hasNext() )
               {  OESDDataPair vals = sVals.next();
                  String tag = vals.GetTag();
                  String val = vals.GetValue();

                  if(oechem.OEGetSDData(mol, tag).length() == 0)
                     oechem.OESetSDData(mol, tag, val);
               }
               sVals.delete();
               secondMatched.add(mergeValue);
            }
         }
         oechem.OEWriteMolecule(ofs, mol);
         mol.Clear();
      }
      mol.delete();

      for(Entry<String, OEGraphMol> scnds : secondRecords.entrySet())
      {  OEGraphMol scndMol = scnds.getValue();
         if( ! secondMatched.contains(scnds.getKey()) )
            oechem.OEWriteMolecule(ofs, scnds.getValue());
         scndMol.Clear();
      }

      ofs.close();
      ifs.close();
   }


   private void parseSecondFile(String secondFile) throws IOException
   {  oemolistream ifs = new oemolistream(secondFile);

      OEGraphMol mol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {  String id;
         if(secondTag.equals("title"))
            id = mol.GetTitle();
         else
            id = oechem.OEGetSDData(mol, secondTag);

         if(id == null || id.length() == 0) continue;

         if(secondRecords.containsKey(id))
            System.err.printf("Warning: id is not unique, second record overwrites first: %s\n",
                               id);
         secondRecords.put(id, mol);
         mol = new OEGraphMol();
      }
      ifs.close();

      if( secondRecords.size() == 0 )
         System.err.printf("No (zero) records with ID=%s found in %s\n\n", secondTag, secondFile);
   }
}
