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

/* SdfTab.java
 * 
 * MA Lardy 4/22/2006
 * Anadys Pharma
 * 
 * This will take a SD file and produce a tab seperated file with headers
*/
import openeye.oechem.*;

import java.io.*;
import java.util.*;


//create a new molecule
public class SdfSmiMerge {

   private static final String EXPLAIN=
         "SdfEvaluator " +
         "\t -values    (file containing inhibition info)\n" +
         "\t -appOutPut (file containing output of the app [ROCS, FRED, etc.])\n" +
         "\t -out       (file containing inhibition info)\n" +         
         "Output is to stdout\n";
   
	public static void main(String argv[]) throws IOException 
   {  // Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      String[] modes    = {};
      String[] parms    = {"-values", "-appOutPut", "-out"};
      String[] reqParms = {"-values", "-appOutPut", "-out"};
      System.out.println("Help me!!!");
      ArrayList<OEGraphMol> mollist = new ArrayList<OEGraphMol>();
      ArrayList<OEGraphMol> appOutList = new ArrayList<OEGraphMol>();
      OEGraphMol mol = new OEGraphMol();
      
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      oemolistream ifs = new oemolistream(cParser.getValue("-values"));
      oemolistream appifs = new oemolistream(cParser.getValue("-appOutPut"));
      oemolostream ofs = new oemolostream(cParser.getValue("-out"));
      
      while (oechem.OEReadMolecule(ifs,mol))
      {  OEGraphMol listMol = new OEGraphMol(mol);
         System.err.println("Mol Read:" + mollist.size());
         if (listMol.IsValid())
            mollist.add(listMol);
//         listMol.Clear();
      }
      
      System.err.println(mollist.size() + " molecules for tab seperated file");

      while (oechem.OEReadMolecule(appifs,mol))
      {  OEGraphMol listMol = new OEGraphMol(mol);
         System.err.println("Mol Read:" + appOutList.size());
         if (listMol.IsValid())
            appOutList.add(listMol);
//         listMol.Clear();
      }
      System.err.println(appOutList.size() + " molecules in output");
      merge(mollist, appOutList);
      write(mollist, ofs);
	}
   
   private static void write(ArrayList<OEGraphMol> mollist,
                             oemolostream ofs)
   {
      // TODO Auto-generated method stub
      for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
     { OEGraphMol mol = new OEGraphMol(iter.next());
       oechem.OEWriteMolecule(ofs,mol);
     }
   }

   @SuppressWarnings("unchecked")
   public static void merge(ArrayList<OEGraphMol> mollist,
            ArrayList<OEGraphMol> appOutList)
   { for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
     { OEGraphMol mol = iter.next();
       String molS = oechem.OECreateCanSmiString(mol);  
       for (Iterator<OEGraphMol> appIter = appOutList.iterator(); appIter.hasNext();)
       {  OEGraphMol appMol = appIter.next();
          String compareS = oechem.OECreateCanSmiString(appMol);
          if (compareS.equals(molS))
          {
            for (OESDDataIter oiter = oechem.OEGetSDDataPairs(appMol); 
                 oiter.hasNext(); )
            {  OESDDataPair dp = oiter.next();
               oechem.OESetSDData(mol,dp);
            } 
          }

       }
     }
   }
 
}