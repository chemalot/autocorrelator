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

import openeye.oechem.*;



/**
 * The Load class uses DataLoader.load() to load data into ORACLE tables.
 * It is used in command-line mode.
 */

public class SdfTabMerger
{  private static final String EXPLAIN=
         "sdfTabMerger -sdf fname -tab fName -out fName -mergeTag tagName -mergeCol name\n" +
         "  Will merge the data from the tab file into the sdf file based on the common\n" +
         "  value, the first line in tab file is considered a header (= new sdf tag names).\n\n" +
         "\t-sdf ........input sdf file (any OE filetype),  for sdtin use .type.\n" +
         "\t-tab ........input tab file for sdtin use '-'.\n" +
         "\t-out ........output file (any OE filetype), for stdout use .type.\n" +
         "\t-outAll .....output sdf records even sdf record does not have value for mergeTag.\n" +
         "\t-quiet ......Do not print warnings.\n" +
         "\t-addEmptyValues add tags with empty values if not in the tab file\n" +
         "\t-mergeTag ...name of tag in sdf file to identify records to merge.\n" +
         "\t             'title' may be used to use the sdf-title for merging.\n" +
         "\t-mergeCol ...column header name in tab file to identify rows to merge.\n" +
         "\t-mergeMode ..If the tab-delimited file contains multiple rows with\n" +
         "\t             same value in *mergeCol*, specify to output only the first,\n" +
         "\t             last, or multiRecord or multiRecordKeepTemplate rows.\n" +
         "\t             multiRecordKeepTemplate will keep the unchanged template\n" +
         "\t             record and add a duplicate for all tab records.\n" +
         "\t             If not specified, default is last.\n" +
         "\t-templateRecord tag=value a record having value in tag is a template\n" +
         "\t             if multiRecordKeepTemplate and not given all records are templates.\n\n";

   private static final Set<String> mergeModeFlags = new HashSet<String>();
   static
   {  mergeModeFlags.add("first");
      mergeModeFlags.add("last");
      mergeModeFlags.add("multiRecord");
      mergeModeFlags.add("multiRecordKeepTemplate");
   }

   private final boolean quiet;
   private final String mergeCol;
   private String[] colHeaders;
   private Map<String,List<String[]>> tabEntries;

   public SdfTabMerger(String tabFile, String mergeCol, boolean quiet)
   throws IOException
   {  this.mergeCol = mergeCol;
      this.quiet = quiet;

      parseTabFile(tabFile);
   }

   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = {"-quiet", "-addEmptyValues",  "-outAll" };
      String[] parms    = { "-sdf", "-tab", "-out", "-mergeTag", "-mergeCol",
                            "-mergeMode", "-templateRecord" };
      String[] reqParms = { "-sdf", "-tab", "-out", "-mergeTag", "-mergeCol" };

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);

      String in = cParser.getValue("-sdf");
      String out = cParser.getValue("-out");
      boolean quiet = cParser.wasGiven("-quiet");
      String tabFile = cParser.getValue("-tab");
      String mergeCol = cParser.getValue("-mergeCol");
      String mergeTag = cParser.getValue("-mergeTag");
      String mergeMode = cParser.getValue("-mergeMode");
      boolean addEmptyValues = cParser.wasGiven("-addEmptyValues");
      String templateRecordTag = cParser.getValue("-templateRecord");
      boolean outAll = cParser.wasGiven("-outAll");

      if( mergeMode == null || mergeMode.length() == 0)
         mergeMode = "last";
      else if(!mergeModeFlags.contains(mergeMode))
      {  System.err.printf("Valid -mergeMode flags are first, last, multiRecord and multiRecordKeepTemplate\n%s\n\n",
               EXPLAIN);
      System.exit(1);
      }

      String templateRecordValue = null;
      if( templateRecordTag != null && templateRecordTag.length() == 0 )
      {  templateRecordTag = null;
      }else if( templateRecordTag != null )
      {  String[] parts = templateRecordTag.split("=");
         if( parts.length != 2 || parts[0].length() == 0 || parts[1].length() == 0)
         {  System.err.println("-templateRecord invalid format");
            System.err.println(EXPLAIN);
            System.exit(1);
         }
         templateRecordTag = parts[0];
         templateRecordValue = parts[1];
      }

      if( ! "multiRecordKeepTemplate".equals(mergeMode) && templateRecordTag != null )
      {  System.err.println("-templateRecord only allowed for -mergeMode multiRecordKeepTemplate");
         System.err.println(EXPLAIN);
         System.exit(1);
      }

      SdfTabMerger merger = new SdfTabMerger(tabFile, mergeCol, quiet);
      merger.merge(mergeTag, mergeMode, addEmptyValues, outAll, templateRecordTag,
                   templateRecordValue, in, out);
   }


   private void merge(String mergeTag, String mergeMode, boolean addEmptyValues, boolean outAll,
            String templateRecordTag, String templateRecordValue, String in, String out)
   {  oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);

      if( templateRecordTag == null ) templateRecordTag = "duplicates";

      OEGraphMol mol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {
         if( mergeMode.equalsIgnoreCase("multiRecordKeepTemplate") )
         {  if( isTemplate(mol, templateRecordTag, templateRecordValue) )
            {  // output unmodified template with templateRecordTag=templateRecordValue
               OEGraphMol molCopy = new OEGraphMol(mol);
               oechem.OESetSDData(molCopy, "duplicates", "original");
               oechem.OEWriteMolecule(ofs, molCopy);
               molCopy.delete();
            }
            else   // not isTemplate: output with no changes even to "duplicates" tag
            {  oechem.OEWriteMolecule(ofs, mol);
               continue;   //not a template output without merging
            }
         }

         String mergeValue;
         if(mergeTag.equals("title"))
            mergeValue = mol.GetTitle();
         else
            mergeValue = oechem.OEGetSDData(mol,mergeTag);

         if(mergeValue.length() == 0)
         {  if(! quiet )
               System.err.printf("No value for mergeTag in sdfRecord (%s)\n", mol.GetTitle());
            if( outAll )
               oechem.OEWriteMolecule(ofs, mol);
         }else
         {  List<String[]> tabList = tabEntries.get(mergeValue);

            if(tabList == null)
            {  if(! quiet )
                  System.err.printf("No entry in tab file for: %s\n", mergeValue);

               if(mergeMode.equalsIgnoreCase("multiRecord") )
                  oechem.OESetSDData(mol, "duplicates", "first");

               // no output for multiRecordKeepTemplate if not tab matches
               if( ! mergeMode.equalsIgnoreCase("multiRecordKeepTemplate") )
                  oechem.OEWriteMolecule(ofs, mol);

            }else // tabList > 0
            {  List<String[]> outList = new ArrayList<String[]>();
               if(mergeMode.equalsIgnoreCase("first"))
                  outList.add( tabList.get(0));
               else if (mergeMode.equalsIgnoreCase("last"))
                  outList.add(tabList.get(tabList.size()-1));
               else
                  outList = tabList;

               for(int outListIdx=0; outListIdx<outList.size(); outListIdx++)
               {  String[] tabValues = outList.get(outListIdx);
                  OEGraphMol molCopy = new OEGraphMol(mol);

                  for(int i=0; i<colHeaders.length; i++)
                  {  if(colHeaders[i].equals(mergeCol)) continue;
                     if(tabValues.length <= i || tabValues[i].length() == 0)
                     {  if( addEmptyValues )
                           oechem.OESetSDData(molCopy,colHeaders[i], "" );
                        continue;
                     }
                     oechem.OESetSDData(molCopy, colHeaders[i],tabValues[i]);
                  }

                  if( mergeMode.equalsIgnoreCase("multiRecord") )
                  {  if(outListIdx == 0)
                        oechem.OESetSDData(molCopy, "duplicates", "first");
                     else
                        oechem.OESetSDData(molCopy, "duplicates", "duplicate");

                  } else if( mergeMode.equalsIgnoreCase("multiRecordKeepTemplate") )
                  {  oechem.OESetSDData(molCopy, "duplicates", "duplicate");
                  }

                  oechem.OEWriteMolecule(ofs, molCopy);
                  molCopy.delete();
               }
            }
         }
         mol.Clear();
      }
      mol.delete();
      ofs.close();
      ifs.close();
      ofs.delete();
      ifs.delete();
   }

   private static boolean isTemplate(OEGraphMol mol,
                           String templateRecordTag, String templateRecordValue)
   {  return templateRecordValue == null
          || templateRecordValue.equals(oechem.OEGetSDData(mol,templateRecordTag));
   }


   private void parseTabFile(String tabFile) throws IOException
   {  BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      if(! tabFile.equals("-"))
         in = new BufferedReader(new FileReader(tabFile));
      String line = in.readLine();
      if(line == null)
         throw new IOException("Empty file: " + tabFile);

      colHeaders = line.trim().split(" *\t *");
      tabEntries = new HashMap<String,List<String[]>>();

      int mergeColIdx = -1;
      for(int i=0; i< colHeaders.length; i++)
      {  if(colHeaders[i].equals(mergeCol))
         {  mergeColIdx = i;
            break;
         }
      }
      if(mergeColIdx == -1)
         throw new IOException(String.format("Could not find header called (%s)",mergeCol));

      while((line = in.readLine()) != null)
      {  if(line.length() == 0) continue;
         String[] values = line.split(" *\t *");
         if( values.length == 0 ) continue;

         values[0] = values[0].trim();    //remove leading " "
         List<String[]> list = tabEntries.get(values[mergeColIdx]);
         if( list == null )
            list = new ArrayList<String[]>();
         list.add(values);
         tabEntries.put(values[mergeColIdx], list);
      }

      in.close();
   }
}
