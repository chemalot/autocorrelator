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
package autocorrelator.ac;

import openeye.oechem.*;

import java.io.*;
import java.util.*;
import autocorrelator.apps.*;

//create a new molecule
public class CreateUniqueACID {

   private static final String EXPLAIN=
         "createUniqueACID.csh\n" +
         "\t -mode      (sdf, tab)\n";
   
	public static void main(String argv[]) throws IOException 
   {  // Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      HashMap<String, Integer> map = new HashMap<String, Integer>();
      HashMap<String, Integer> badMap = new HashMap<String, Integer>();
      int id = 0;
      String[] modes    = {};
      String[] parms    = {"-mode"};
      String[] reqParms = {"-mode"};

      OEGraphMol mol = new OEGraphMol();
      
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      String mode = cParser.getValue("-mode");
      if (mode.equals("SDF")|mode.equals("sdf")|mode.equals("sDF")|
          mode.equals("sdF")|mode.equals("SdF")|mode.equals("SDf"))
      {
      oemolistream ifs = new oemolistream(cParser.getValue("-in"));
      oemolostream ofs = new oemolostream(cParser.getValue("-out"));
      
      while (oechem.OEReadMolecule(ifs,mol))
      {  String s = String.format("%08d", id);
         oechem.OESetSDData(mol, "AC#", s);
         mol.SetTitle(s);
         id++;
         oechem.OEWriteMolecule(ofs, mol);
      }
      
      }
      
      
      
      String s = String.format("%08d", id);

       
      
	}

}