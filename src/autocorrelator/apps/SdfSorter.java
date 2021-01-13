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
import java.util.*;

import openeye.oechem.*;



/**
 * The Load class uses DataLoader.load() to load data into ORACLE tables.
 * It is used in command-line mode.
 */

public class SdfSorter
{  private static final String EXPLAIN=
         "sdfSorter -numeric -sortTag tagName -in fn -out fn\n\n" +
         "\t-in ........input file (any OE filetype),  for sdtin use .type.\n" +
         "\t-out .......output file (any OE filetype), for stdout use .type.\n" +
         "\t-maxOut ....only output n records\n"+
         "\t-limitRepeats tagName=count will limit repeats of recors with the same\n" +
         "\t\tvalue for the given tag to count number of repeats\n"+
         "\t-asc .......sort in ascending order (default).\n" +
         "\t-desc ......sort in descending order.\n" +
         "\t-string ....sort in alphabetic order (default). null => ''\n" +
         "\t-numeric ...sort in numerical numeric order. null sort last\n" +
         "\t-sortTag ...name of tag on which to sort.\n" +
         "The last tree tags can be repeated, sorting will be prioratized left to right.\n" +
         "Any -numeric, -asc or -desc flag will influence subsequent sortTags.\n\n";

   @SuppressWarnings("unchecked")
   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = { "-asc", "-desc", "-string", "-numeric" };
      String[] parms    = { "-sortTag", "-in", "-out", "-limitRepeats",
                            "-maxOut" };
      String[] reqParms = {"-in", "-out", "-sortTag"};

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);

      List<Comparator<OEGraphMol>> comparators = new ArrayList<Comparator<OEGraphMol>>();
      boolean isNumeric = false;
      boolean isReverse = false;

      for(int i=0; i < args.length; i++)
      {  if("-sortTag".equals(args[i]))
         {  Comparator<OEGraphMol> comparator;
            if(i+1 == args.length)
            {  System.err.printf("Missing tag name\n%s", EXPLAIN);
               System.exit(1);
            }
            String sortTag = args[++i];
            if(isNumeric)
               comparator = new NumericComparator(sortTag, isReverse);
            else
               comparator = new StringComparator(sortTag, isReverse);
            comparators.add(comparator);
         }
         else if("-numeric".equals(args[i]))
            isNumeric = true;
         else if("-string".equals(args[i]))
            isNumeric = false;
         else if("-asc".equals(args[i]))
            isReverse = false;
         else if("-desc".equals(args[i]))
            isReverse = true;
      }
      Comparator<OEGraphMol> comparator = new MultiComparator<OEGraphMol>(
               comparators.toArray(new Comparator[comparators.size()]));

      int maxRepeat = 0;
      String repeatLimitTag  = cParser.getValue("-limitRepeats");
      if(repeatLimitTag != null)
      {  String[] dummy = repeatLimitTag .split("=");
         if(dummy.length != 2 || ! dummy[1].matches("^\\d+$"))
         {  System.err.println("Bad Limit Expression: "+ repeatLimitTag);
            System.exit(1);
         }
         repeatLimitTag = dummy[0];
         maxRepeat = Integer.parseInt(dummy[1]);
      }

      String in = cParser.getValue("-in");
      String out = cParser.getValue("-out");
      int maxOut = Integer.MAX_VALUE;
      String d = cParser.getValue("-maxOut");
      if( d != null ) maxOut = Integer.parseInt(d);

      oemolistream ifs = new oemolistream(in);

      List<OEGraphMol> molList = new ArrayList<OEGraphMol>(300);
      OEGraphMol mol = new OEGraphMol();
      while (oechem.OEReadMolecule(ifs, mol))
      {  molList.add(mol);
         mol = new OEGraphMol();
      }
      ifs.close();

      Collections.sort(molList,comparator);

      oemolostream ofs = new oemolostream(out);
      int repeatCount = 0;
      String repeatLimitVal = null;
      for(OEGraphMol mol2 : molList)
      {
         // check for repeats in the value of the repeatLimitTag
         if(repeatLimitTag != null)
         {  String tabVal = oechem.OEGetSDData(mol2, repeatLimitTag);
            if(tabVal.equals(repeatLimitVal))
            {  repeatCount++;
            } else
            {  repeatCount = 1;
               repeatLimitVal = tabVal;
            }
            if(repeatCount>maxRepeat) continue; // do not print out until change
         }
         oechem.OEWriteMolecule(ofs, mol2);

         if( --maxOut <= 0 ) break;
      }
      ofs.close();
   }
}

class MultiComparator<T> implements Comparator<T>
{  private final Comparator<T>[] comparators;

   MultiComparator(Comparator<T>[] comparators)
   {  assert comparators.length > 0;
      this.comparators = comparators;
   }

   public int compare(T o1, T o2)
   {  for(int i=0; i<comparators.length; i++)
      {  int cmp = comparators[i].compare(o1, o2);
         if(cmp != 0) return cmp;
      }
      return 0;
   }
}

class StringComparator implements Comparator<OEGraphMol>
{  private final String sortTag;
   private final boolean isReverse;

   StringComparator(String sortTag, boolean isReverse)
   {  this.isReverse = isReverse;
      this.sortTag = sortTag;
   }

   public int compare(OEGraphMol o1, OEGraphMol o2)
   {  int ret = oechem.OEGetSDData(o1, sortTag).compareTo(oechem.OEGetSDData(o2, sortTag));
      if(isReverse) return -ret;

      return ret;
   }
}

class NumericComparator implements Comparator<OEGraphMol>
{  private final String sortTag;
   private final boolean isReverse;

   NumericComparator(String sortTag, boolean isReverse)
   {  this.isReverse = isReverse;
      this.sortTag = sortTag;
   }

   public int compare(OEGraphMol o1, OEGraphMol o2)
   {  String s1 = oechem.OEGetSDData(o1, sortTag);
      String s2 = oechem.OEGetSDData(o2, sortTag);

      if(s1.length() == 0)
      {  if( s2.length() == 0)
            return 0;
         else
            return 1;
      }

      if( s2.length() == 0)
         return -1;

      int ret = Double.compare(Double.parseDouble(s1), Double.parseDouble(s2));

      if(isReverse) return -ret;

      return ret;
   }
}
