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

import openeye.oechem.OEGraphMol;
import openeye.oechem.OESubSearch;
import openeye.oechem.oechem;
import openeye.oechem.oemolistream;
import openeye.oechem.oemolostream;



/**
 *
 */

public class SdfSmartsGrep
{
   private static final String EXPLAIN=
         "sdfSmartsGrep -in fn -out fn [-v] smarts\n" +
         "  -v..........output if no match\n" +
         "\t-in.........input file (any OE filetype),  for sdtin use .type.\n" +
         "\t-out........output file (any OE filetype), for stdout use .type.\n" +
         "\n";

   public static void main(String[] args)
   {  CommandLineParser cParser;
      String[] modes    = { "-v", "-makeHExplicit", "-makeHImplicit"};
      String[] parms    = { "-in", "-out" };
      String[] reqParms = {"-in", "-out" };

      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);

      String in = cParser.getValue("-in");
      String out = cParser.getValue("-out");
      boolean nonMatch = cParser.wasGiven("-v");

      boolean makeHExplicit = cParser.wasGiven("-makeHExplicit");
      boolean makeHImplicit = cParser.wasGiven("-makeHImplicit");
      if( makeHExplicit && makeHImplicit )
      {  System.err.println("makeHImplicit may not be used with makeHExplicit");
         System.exit(1);
      }


      args = cParser.getRestArgs();
      if( args.length == 0 && (makeHExplicit || makeHImplicit) )
      {  System.err.println("No Smarts given only applying hydrogen transformation!\n");
         
      } else if( args.length != 1 )
      {  System.err.println("Exactly one smarts must be given!\n"
                           +EXPLAIN );
         System.exit(1);
      }

      oemolistream ifs = new oemolistream(in);
      oemolostream ofs = new oemolostream(out);

      int count = 0;
      OEGraphMol mol = new OEGraphMol();
      OESubSearch ss = null;
      if( args.length == 1)
      {  ss = new OESubSearch(args[0]);
         if(! ss.IsValid())
            throw new Error("Invalid Smarts " + args[0]);

         if( nonMatch )
         {  while (oechem.OEReadMolecule(ifs, mol))
            {  count++;
               if(makeHExplicit) oechem.OEAddExplicitHydrogens(mol);
               if(makeHImplicit) oechem.OESuppressHydrogens(mol, false, false, false);

               oechem.OEPrepareSearch(mol, ss);
               if( ! ss.SingleMatch(mol) )
               {  oechem.OEWriteMolecule(ofs, mol);
               }
   
               mol.Clear();
            }
         }else
         {  while (oechem.OEReadMolecule(ifs, mol))
            {  count++;
               if(makeHExplicit) oechem.OEAddExplicitHydrogens(mol);
               if(makeHImplicit) oechem.OESuppressHydrogens(mol, false, false, false);
               
               oechem.OEPrepareSearch(mol, ss);
               if( ss.SingleMatch(mol) )
               {  oechem.OEWriteMolecule(ofs, mol);
               }
   
               mol.Clear();
            }
         }
      } else  // no smarts specified just hydrogen change
      {  while (oechem.OEReadMolecule(ifs, mol))
         {  count++;
            if(makeHExplicit) oechem.OEAddExplicitHydrogens(mol);
            if(makeHImplicit) oechem.OESuppressHydrogens(mol, false, false, false);

            oechem.OEWriteMolecule(ofs, mol);
            mol.Clear();
         }
      }
      ifs.close();
      ofs.close();

      mol.delete();
      if( ss != null) ss.delete();
      System.err.printf("SdfSmartsGrep: completed reading %d compounds\n", count);
   }
}
