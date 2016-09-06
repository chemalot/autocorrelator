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

import openeye.oechem.*;



/**
 * The Load class uses DataLoader.load() to load data into ORACLE tables.
 * It is used in command-line mode.
 */

public class SdfSplicer
{  private static final String EXPLAIN=
         "sdfSplicer -start n [-end|count] n -in fn -out fn [-skipped fn]\n\n" +
         "   This will cut out a region from an SDF file\n" +
         "\t-in.........input file (any OE filetype),  for sdtin use .type.\n" +
         "\t-out........output file (any OE filetype), for stdout use .type.\n" +
         "\t-skipped ...output file for records not in -out\n" +
         "\t-start n ...First molecule to include in output (default=1)\n" +
         "\t-end n......First molecule to be excluded from output (default all)\n"+
         "\t-count n....How many molecules to output (default none)\n"+
         "\t-readAll....continue reading input even if no more output is produced\n"+
         "\t            use this in pipes to enable preceeding commands to see all input\n"+
         "\t\t          start must be < then end.\n\n";

  public static void main(String[] args)
   {  CommandLineParser cParser;
      String[] modes    = { "-readAll" };
      String[] parms    = { "-start", "-end", "-in", "-out", "-skipped", "-count" };
      String[] reqParms = {"-in", "-out" };
      
      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      
      int start=1;
      int end  = Integer.MAX_VALUE;

      if(cParser.wasGiven("-start"))
         start = Integer.parseInt(cParser.getValue("-start"));
      
      if(cParser.wasGiven("-end"))
         end = Integer.parseInt(cParser.getValue("-end"));
      
      if(cParser.wasGiven("-count"))
         end = start + Integer.parseInt(cParser.getValue("-count"));
      
      if(cParser.wasGiven("-end") && cParser.wasGiven("-count"))
      {  System.err.println("only one of -end and -count is allowed!\n");
         System.err.println(EXPLAIN);
         System.exit(1);
      }
      
      if(start >= end)
      {  System.err.println(EXPLAIN);
         System.exit(1);
      }
      
      
      String in = cParser.getValue("-in");
      String out = cParser.getValue("-out");
      String skipped = cParser.getValue("-skipped");
      boolean readAll = cParser.wasGiven("-readAll") || skipped != null;
      
      oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);
      oemolostream ofsSkipped = null;
      if( skipped != null ) ofsSkipped = new oemolostream(skipped);
      
      OEGraphMol mol = new OEGraphMol();
      int count = 0;
      while (oechem.OEReadMolecule(ifs, mol)) 
      {  count++;
         if( count >= end) break;
         
         if( count < start) 
         {  if( ofsSkipped != null ) 
               oechem.OEWriteMolecule(ofsSkipped, mol);
            continue;
         }
         
         oechem.OEWriteMolecule(ofs, mol);
         mol.Clear();
      }
      
      if( readAll )
      {  while (oechem.OEReadMolecule(ifs, mol))
         {  if( ofsSkipped != null ) 
               oechem.OEWriteMolecule(ofsSkipped, mol);
         }
      }
            
      ifs.close();
      ofs.close();
      if( ofsSkipped != null ) 
      {  ofsSkipped.close();
         ofsSkipped.delete();
      }
      ifs.delete();
      ofs.delete();
      mol.delete();
   }
}
