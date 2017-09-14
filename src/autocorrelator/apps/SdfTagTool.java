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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import openeye.oechem.*;
import openeye.oeiupac.*;

/**
 *
 */

public class SdfTagTool
{
   private static final String EXPLAIN=
         "sdfTagTool -in fn -out fn\n" +
         "  -addSmi\n"+
         "  -addAll fn\n" +
         "  -add tag=const|tag2=const2\n"+
         "  -append tag1=tag2|tag3=tag4\n"+
         "  -copy srctag=desttag|srctag2=desttag2 \n" +
         "  -format outTag=FORMATSTRING -formatNullReplacement repl\n" +
         "  -IUPAC openeye|iupac|cas|traditional|systematic \n" +
         "  -keep tag1|tag2|...\n"+
         "  -prefix tagPrefix\n"+
         "  -remove tag1|tag2|...\n"+
         "  -rename oldtag=newtag|tag2=newtag2\n"+
         "  -reorder tag1|tag2|..\n"+
         "  -numberRepeats repeatTag\n"  +
         "  -title tagName|counter\n\n"+
         "sdfTagTool can generate and processes tags and their values.\n" +
         "It accepts all openeye supported file formats.\n" +
         "Options in order of processing:\n" +
         "\t-in ........input file (any OE filetype),  for sdtin use .type\n" +
         "\t-out .......output file (any OE filetype), for stdout use .type\n" +
         "\t-numberRepeats.creat new field repeatTag_Num which counts the oocurence \n" +
         "\t               of consecutive values in repeatTag\n" +
         "\t-remove ....remove any tags with the specified names\n" +
         "\t-copy ......copy from tag to tag (TITLE can be used as src or dest)\n" +
         "\t-append ....append second tag to first tag\n" +
         "\t-rename ....rename tags specified, use TITLE to rename from and to the title.\n" +
         "\t-prefix ....Prefix all tags with the tagPrefix\n" +
         "\t-keep ......keep only tags with the specified names (applied after rename and prefix).\n" +
         "\t-transform .regular expression transform (use '\\' to quote '/') eg. TAGNAME/a/b/\n"+
         "\t-addSmi ....will add the SMI tag with the oeCanSmiles\n" +
         "\t-rmDupTag ..remove records if any previous records had the same val\n"+
         "\t-markAsRepeatTag add a Repeat Tag by prepending the word Repeat in front of argument to mark repeated records\n"+
         "\t-rmRepeatTag tag=n remove records if tag value was found more than n times previously\n"+
         "\t-counterTag use the argument as the tag name in -addCounter\n" +
         "\t-counterStart use the argument as first counter value in -addCounter\n" +
         "\t-addCounter will add the counter tag with the position number\n" +
         "\t-add .......will add a tag with a constant value (use TITLE to replace mol-title with constant)\n" +
         "\t-addAll ....will add all values and the title from the first record in the\n" +
         "\t            given file\n" +
         "\t-IUPAC .....will add a IUPAC_style tag that stores a molecule's in the style specified\n" +
         "\t-InChiKey .....will add InChi key to a tag named InChiKey\n" +
         "\t-title .....add contents of a tag or a 'counter' as the title\n"+
         "\t-format ....Will reformat one or multiple fields into a new one. eg.:\n" +
         "\t            oTag={it1}/{it2}/{it3} : create out-tag (oTag) combining values from in-tags (it?).\n"+
         "\t            For numeric fields the in-tags can contain roundign information e.g. {tag:r3}\n"+
         "\t-formatNullReplacement specify the string replacing an empty value in -format (def=_)\n"+
         "\t-reorder ...reorder tags, unspecified tags are appened at the end, in their original order.\n"+
      "   All '|' seperated options can also be entered by a newline separated file:\n"+
      "     specify filename.txt (.txt is requires) instead of a '|' separated value\n" +
         "\nExamples:\n"+
         "Remove duplicate molecules based on canism: -addSmi -rmDupTag SMI\n";

   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = { "-addSmi", "-addCounter", "-InChiKey"};
      String[] parms    = {"-remove", "-title",  "-in", "-out", "-rename", "-prefix",
                           "-add",    "-addAll", "-keep", "-copy", "-append", "-reorder",
                           "-keepNumeric", "-rmDupTag", "-rmRepeatTag", "-markAsRepeatTag", "-transform", "-IUPAC",
                           "-format", "-formatNullReplacement", "-counterTag", "-counterStart", "-numberRepeats" };
      String[] reqParms = {"-in", "-out"};
      HashSet<String> keepTags = new HashSet<String>();
      String[] removeTags = new String[0];
      Set<String> rmDuplicatSet = new HashSet<String>(2000);
      Set<String> markAsRepeatSet = new HashSet<String>(2000);

      List <String> reorderTags = new ArrayList<String>();
      List <String> iupacStyles = new ArrayList<String>();

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      String dummy      = cParser.getValue("-remove");
      if(dummy != null && dummy.length() > 0) removeTags = parseSeparatedArgument(dummy);

      dummy      = cParser.getValue("-keep");
      if(dummy != null && dummy.length() > 0)
      {  String[] dummies = parseSeparatedArgument(dummy);
         keepTags.addAll(Arrays.asList(dummies));
      }

      /* for adding iupac names*/
      dummy      = cParser.getValue("-IUPAC");
      if(dummy != null && dummy.length() > 0)
      {  String[] dummies = parseSeparatedArgument(dummy);
         iupacStyles.addAll(Arrays.asList(dummies));
      }

      /* for adding InChi key*/
      boolean addInChiKey = cParser.wasGiven("-InChiKey");
      
      /* for reordering sd tags*/
      dummy      = cParser.getValue("-reorder");
      if(dummy != null && dummy.length() > 0)
      {  String[] dummies = parseSeparatedArgument(dummy);
         reorderTags.addAll(Arrays.asList(dummies));
      }

      String title = cParser.getValue("-title");
      if(title==null) title="";
      title = title.trim();

      String numberRepeatTag = cParser.getValue("-numberRepeats");
      String lastRepeatVal = "";
      int repeatValCounter = 0;

      boolean addSmiles  = cParser.wasGiven("-addSmi");
      boolean addCounter = cParser.wasGiven("-addCounter");
      String counterTag  = cParser.getValue("-counterTag");
      if( counterTag == null ) counterTag = "counter";
      int count = 0;
      dummy = cParser.getValue("-counterStart");
      if( dummy != null )
         count = Integer.parseInt(dummy) - 1;

      dummy = cParser.getValue("-rename");
      Map<String, String> renameMap = getNameValuePairs(dummy);

      dummy = cParser.getValue("-copy");
      Map<String, String> copyMap = getNameValuePairs(dummy);

      dummy = cParser.getValue("-append");
      Map<String, String> appendMap = getNameValuePairs(dummy);

      dummy = cParser.getValue("-addAll");
      Map<String, String> constMap = getAllTags(dummy);

      dummy = cParser.getValue("-add");
      constMap.putAll(getNameValuePairs(dummy));

      String transTag = null;
      Pattern transPat = null;
      String replacement = null;
      dummy = cParser.getValue("-transform");
      if( dummy != null )
      {  String[] parts = dummy.split("(?<!\\\\)(?>\\\\\\\\)*/",-1);
         if( parts.length != 4 || parts[0].length() == 0 || parts[1].length() == 0)
         {  System.err.printf("Invalid transform: %s\n", dummy);
            System.exit(1);
         }

         transTag = unQuoteBackSlash(parts[0]);
         replacement = unQuoteBackSlash(parts[2]);
         transPat = Pattern.compile(unQuoteBackSlash(parts[1]));
      }

      Map<String,Integer> rmRepeatMap = null;
      String rmRepeatTag = cParser.getValue("-rmRepeatTag");
      int rmRepeatMax = 0;
      if( rmRepeatTag != null )
      {  if( ! rmRepeatTag.matches("^.*=\\d+$") )
         {  System.err.println("invalid rmRepeatTag: " + rmRepeatTag);
            System.exit(1);
         }
         rmRepeatMap = new HashMap<String, Integer>(2000);
         dummy = rmRepeatTag.substring(rmRepeatTag.indexOf('=')+1);
         rmRepeatTag = rmRepeatTag.substring(0,rmRepeatTag.indexOf('='));
         rmRepeatMax = Integer.parseInt(dummy);
      }

      String formatNullReplacment = cParser.getValue("-formatNullReplacement");
      if( formatNullReplacment == null ) formatNullReplacment = "_";

      String formatOutTag = null;
      Matcher formatParser = null;
      dummy = cParser.getValue("-format");
      if( dummy != null )
      {  if(! dummy.matches("[^=]+=.*\\{[^}]+\\}.*") )
         {  System.err.println("invalid format string: " + dummy);
            System.exit(1);
         }

         formatOutTag = dummy.substring(0,dummy.indexOf("="));
         formatParser = Pattern.compile("\\{(([^}]+?)(:(r)\\d+)?)\\}").matcher(dummy.substring(formatOutTag.length()+1));
         if( ! formatParser.find() )
         {  System.err.println("No input tags ({tagName}) specified in -format " + dummy);
            System.exit(1);
         }
         formatParser.reset();
      }

      String prefix = cParser.getValue("-prefix");
      String rmDupTag = cParser.getValue("-rmDupTag");
      String markAsRepeatTag = cParser.getValue("-markAsRepeatTag");
      String repeatTag = "Repeat " + markAsRepeatTag;
      String in = cParser.getValue("-in");
      String out = cParser.getValue("-out");

      oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);

      OEGraphMol mol = new OEGraphMol();
      OEGraphMol tmpMol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {  count++;

         // numberRepeatTag
         if( numberRepeatTag != null && numberRepeatTag.length() > 0 )
         {  String newVal = oechem.OEGetSDData(mol, numberRepeatTag);
            if( lastRepeatVal.equals(newVal) )
               repeatValCounter++;
            else
               repeatValCounter = 1;

            oechem.OESetSDData(mol, numberRepeatTag + "_Num", Integer.toString(repeatValCounter) );
            lastRepeatVal = newVal;
         }


         // remove
         for(String tag : removeTags)
         {  if( "TITLE".equals(tag))
               mol.SetTitle("");
            else
            {  while(oechem.OEDeleteSDData(mol, tag))
               {  // just delete
               }
            }
         }

         // copy
         for(Entry<String,String> entry : copyMap.entrySet())
         {  String fromTag = entry.getKey();
            String toTag   = entry.getValue();

            String val = oechem.OEGetSDData(mol, fromTag);
            if(val.length() == 0 && "TITLE".equals(fromTag))
               val = mol.GetTitle();

            if(val.length() > 0)
            {  if("TITLE".equals(toTag))
                  mol.SetTitle(val);
               else
                  oechem.OESetSDData(mol,toTag, val);
            }
         }

         // append
         for(Entry<String,String> entry : appendMap.entrySet())
         {  String toTag = entry.getKey();
            String fromTag   = entry.getValue();

            String valtail = oechem.OEGetSDData(mol, fromTag);
            if(valtail.length() == 0 && "TITLE".equals(fromTag))
               valtail = mol.GetTitle();

            if(valtail.length() > 0)
            {  if("TITLE".equals(toTag))
               {  String val = mol.GetTitle();
                  val += " " + valtail;
                  mol.SetTitle(val);
               } else
               {  String val = oechem.OEGetSDData(mol,fromTag) + " " + valtail;
                  oechem.OESetSDData(mol,fromTag, val);
               }
            }
         }

         // rename
         for(Entry<String,String> entry : renameMap.entrySet())
         {  String fromTag = entry.getKey();
            String toTag   = entry.getValue();
            if( fromTag.equals(toTag) ) continue;

            String val;
            if("TITLE".equals(fromTag))
               val = mol.GetTitle();
            else
            {  val = oechem.OEGetSDData(mol, fromTag);
               if( val.length() == 0 && !oechem.OEHasSDData(mol, fromTag))
                  continue; // do not overwrite if source tag does not exist
            }

            if("TITLE".equals(toTag))
               mol.SetTitle(val);
            else
               oechem.OESetSDData(mol,toTag, val);

            if( "TITLE".equals(fromTag) )
               mol.SetTitle("");
            else
               oechem.OEDeleteSDData(mol, fromTag );
         }

         //prefix
         if(prefix != null && prefix.length()>0)
         {  OESDDataIter pairsIt = oechem.OEGetSDDataPairs(mol);
            while(pairsIt.hasNext())
            {  OESDDataPair pair = pairsIt.next();
               oechem.OESetSDData(mol,prefix+pair.GetTag(),pair.GetValue());
               oechem.OEDeleteSDData(mol, pair.GetTag());
            }
         }

         // keep
         if(keepTags.size() > 0)
         {  OESDDataIter pairsIt = oechem.OEGetSDDataPairs(mol);
            while(pairsIt.hasNext())
            {  OESDDataPair pair = pairsIt.next();
               if (!keepTags.contains(pair.GetTag()))
               {
                  if (!cParser.wasGiven("-keepNumeric"))
                     oechem.OEDeleteSDData(mol, pair.GetTag());
                  else if (!isNumeric(pair.GetValue()))
                     oechem.OEDeleteSDData(mol, pair.GetTag());
               }
            }
         }


         //transform
         if( transTag != null )
         {  if( "TITLE".equals(transTag) )
              dummy = mol.GetTitle();
            else
              dummy = oechem.OEGetSDData(mol, transTag);

            dummy = transPat.matcher(dummy).replaceAll(replacement);

            if( "TITLE".equals(transTag) )
               mol.SetTitle(dummy);
            else
               oechem.OESetSDData(mol, transTag, dummy);
         }

         ///////////////////////////////////////////////////////////////////////
         // adding data with user defined tag after rename delete and keep
         // gives best possible control
         if(addSmiles)
         {  if( mol.GetDimension() == 3 )
            {  mol.SetPerceived(OEPerceived.BondStereo, false);
               mol.SetPerceived(OEPerceived.AtomStereo, false);
               oechem.OEPerceiveChiral(mol);
               oechem.OE3DToInternalStereo(mol);
            }
            String smi = oechem.OECreateIsoSmiString(mol);
            oechem.OESetSDData(mol, "SMI", smi);
         }


         // remove Duplicate records
         if( rmDupTag != null )
         {  if( "TITLE".equals(rmDupTag) )
               dummy = mol.GetTitle();
            else
               dummy = oechem.OEGetSDData(mol, rmDupTag);

            if( rmDuplicatSet.contains(dummy) )
               continue;
            else
               rmDuplicatSet.add(dummy);
         }
         
         // mark records as duplicated by checking for duplicated tag values
         if (markAsRepeatTag != null)
         {  
            if( "TITLE".equals(markAsRepeatTag) )
               dummy = mol.GetTitle();
            else
               dummy = oechem.OEGetSDData(mol, markAsRepeatTag);
            
            if (markAsRepeatSet.contains(dummy))
            {
               // mark as duplicate
               oechem.OESetSDData(mol, repeatTag, "Yes");
            }else
            {
               markAsRepeatSet.add(dummy);
            }
         }

         if( rmRepeatTag != null )
         {  if( "TITLE".equals(rmRepeatTag) )
               dummy = mol.GetTitle();
            else
               dummy = oechem.OEGetSDData(mol, rmRepeatTag);

            if( dummy != null && dummy.length() > 0 )
            {  Integer rCountI = rmRepeatMap.get(dummy);
               int rCount = rCountI == null ? 0: rCountI;
               if( rCount++ >= rmRepeatMax )
                  continue;   // skip this record

               rmRepeatMap.put(dummy, rCount);
            }
         }

         if(addCounter)
            oechem.OESetSDData(mol, counterTag, Integer.toString(count));


         // new constant values
         for(Entry<String,String> entry : constMap.entrySet())
         {  String tag = entry.getKey();
            String val = entry.getValue();

            if("TITLE".equals(tag))
               mol.SetTitle(val);
            else
               oechem.OESetSDData(mol,tag, val);
         }

         //iupac names
         for (String style : iupacStyles)
         {
            //add a tag for each style
            short [] iupacStyle = oeiupac.OEGetIUPACNamStyle(style); // if style is unknown, it defaults to openeye
            String iupacName = oeiupac.OECreateIUPACName(mol, iupacStyle);
            iupacName = oeiupac.OEToASCII(iupacName);
            if( iupacName.contains("BLAH") ) iupacName = "Unknown Fragment";
            String tag = "IUPAC_" + style;
            oechem.OESetSDData(mol,tag, iupacName);
         }
         
         // add InChi key
         if (addInChiKey)
         {
        	 OEInChIOptions inchi_opt = new OEInChIOptions();
        	 inchi_opt.SetStereo(true); // use stereo information from input structure
        	 String inchiKey = oechem.OECreateInChIKey(mol, inchi_opt);
        	 oechem.OESetSDData(mol,  "InChiKey", inchiKey);
         }

         // title
         if(title.equals("counter"))
            mol.SetTitle(Integer.toString(count));
         else if(title.length() > 0)
            mol.SetTitle(oechem.OEGetSDData(mol, title));


         // -format
         if( formatParser != null )
         {  StringBuffer sb = new StringBuffer();

            while( formatParser.find() )
            {  String inTag = formatParser.group(2);
               String inVal;
               if( "TITLE".equals(inTag) )
                  inVal = mol.GetTitle();
               else
                  inVal = oechem.OEGetSDData(mol, inTag);

               if( inVal == null || inVal.length() == 0 ) 
               {  inVal = formatNullReplacment;
               }else
               {  String numFormat = formatParser.group(3);
                  if( numFormat != null )
                  {  numFormat = numFormat.substring(2); // skip over :r 
                     inVal = String.format("%g."+numFormat, Double.parseDouble(inVal));
                  }
               }

               formatParser.appendReplacement(sb, inVal);
            }
            formatParser.appendTail(sb);
            formatParser.reset();

            if( "TITLE".equals(formatOutTag) )
               mol.SetTitle(sb.toString());
            else
               oechem.OESetSDData(mol, formatOutTag, sb.toString());
         }

         // reorder
         oechem.OEClearSDData(tmpMol);
         oechem.OECopySDData(tmpMol, mol); // saved SD data in a tmpMol
         oechem.OEClearSDData(mol); //get rid of old SD data
         //add SD data back in order specified

         for (String tag : reorderTags)
         {  if (oechem.OEHasSDData(tmpMol,tag))
            {  oechem.OESetSDData(mol, tag, oechem.OEGetSDData(tmpMol, tag));
               oechem.OEDeleteSDData(tmpMol, tag);
            }else
            {  oechem.OESetSDData(mol, tag, "");
            }
         }
         // append rest of tags that were not explicitly reordered
         oechem.OECopySDData(mol,tmpMol);
         tmpMol.Clear();


         oechem.OEWriteMolecule(ofs, mol);
         mol.Clear();
      }
      ifs.close();
      ofs.close();
   }

   private static Map<String,String> getAllTags(String fileName)
   {  Map<String,String> tags = new HashMap<String, String>();

      if(fileName == null || fileName.length() == 0)
         return tags;

      oemolistream ifs = new oemolistream(fileName);
      OEGraphMol mol = new OEGraphMol();
      if(oechem.OEReadMolecule(ifs, mol))
      {  OESDDataIter pairsIt = oechem.OEGetSDDataPairs(mol);
         while(pairsIt.hasNext())
         {  OESDDataPair pair = pairsIt.next();
            tags.put(pair.GetTag(), pair.GetValue());
         }

         String title = mol.GetTitle();
         if(title != null && title.length() > 0)
            tags.put("TITLE", title);
      }
      mol.delete();
      ifs.close();
      return tags;
   }

   private static Map<String, String> getNameValuePairs(String dummy) throws IOException
   {  Map<String,String> varMap = new HashMap<String,String>();

      if(dummy == null || dummy.length() == 0)
         return varMap;

      String [] vars = parseSeparatedArgument(dummy);
      for(String var : vars)
      {  String[] nameVal = var.split("=", 2);
         if(nameVal.length == 1)
            nameVal = new String[] { nameVal[0].trim(), "" };
         else if(nameVal.length != 2)
            throw new Error("Bad rename: "+ var);
         varMap.put(nameVal[0].trim(), nameVal[1].trim());
      }

      return varMap;
   }

   private static boolean isNumeric(String str)
   {  System.err.println("TagTool: isNumeric!!!");
      boolean blnAlpha = false;

      char chr[] = null;
      if(str != null)
      chr = str.toCharArray();

      for(int i=0; i<chr.length; i++)
      {
        if(chr[i] >= '0' && chr[i] <= '9')
        {   break;
        }
      }

     for(int i=0; i<chr.length; i++)
     {
      if((chr[i] >= 'A' && chr[i] <= 'Z') || (chr[i] >= 'a' && chr[i] <= 'z'))
      {
        blnAlpha = true;
        break;
      }
     }
     return (!blnAlpha);

   }

   private static String[] parseSeparatedArgument(String arg) throws IOException
   {  arg = arg.trim();
      if( !arg.contains("|") && arg.endsWith(".txt") && new File(arg).exists() )
         return readSeparatedFile(arg);

      String[] dummies = arg.split("\\s*\\|\\s*");
      Set<String> tagSet = new LinkedHashSet<String>(dummies.length);
      tagSet.addAll(Arrays.asList(dummies));

      return tagSet.toArray(new String[tagSet.size()]);
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


   // look for last \ of uneven sequence of \ followed by (char)
   private static final Pattern quotingBackS = Pattern.compile("(?<!\\\\)(\\\\\\\\)*\\\\([^\\\\])");
   private static final Pattern doubleBackS  = Pattern.compile("\\\\\\\\");

   static String unQuoteBackSlash(String str)
   {  str = quotingBackS.matcher(str).replaceAll("$1$2");
      str = doubleBackS.matcher(str).replaceAll("\\\\");
      return str;
   }
}
