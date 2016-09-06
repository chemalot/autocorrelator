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

import java.io.*;

public class MergeSDFIfRecordsExist {

   private static final String EXPLAIN=
         "MergeSDFIfRecordsExist.csh -record -SDF -output -tags\n" +
         "-record an MDL file with tags .\n" +
         "-SDF  A list of MDL files which already have the tags of interest .\n" +
         "-output A MDL file of the combined output .\n";
   
	public static void main(String argv[]) throws IOException 
   {  // Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      String[] modes    = {""};
      String[] parms    = {"-record", "-SDF", "-output", "-tags"};
      String[] reqParms = {"-record", "-output", "-tags"};
      String[] tag = null;

      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      OEGraphMol mol = new OEGraphMol();      
      OEGraphMol mol2 = new OEGraphMol();       
      oemolistream ifs = new oemolistream(cParser.getValue("-record"));      
      oechem.OEReadMolecule(ifs,mol);

      tag = cParser.getValue("-tags").split("\\|");

      for (int j = 0; j<tag.length; j++)
      {  String tagsI = tag[j];
         String val;
         val = oechem.OEGetSDData(mol, tagsI);
         if (val == null)
         {
            System.err.println("MergeSDFIfRecordsExist: Value Missing " + tagsI);
            System.exit(0);
         }
         if (val.length() == 0)
         {
            System.err.println("MergeSDFIfRecordsExist: Value Missing " + tagsI);
            System.exit(0);
         }
         if (val == "0")
         {
            System.err.println("MergeSDFIfRecordsExist: Aborted Run " + tagsI);
            System.exit(0);
         }
         System.out.print(val);
         System.out.print('\t');
      }      
      oemolostream ofs = new oemolostream(cParser.getValue("-output"));
      if ( cParser.getValue("-SDF") != null ) 
      {
         oemolistream iSDFfs = new oemolistream(cParser.getValue("-SDF"));      
         while (oechem.OEReadMolecule(iSDFfs,mol2))
         {
            oechem.OEWriteMolecule(ofs, mol2);
         }
         iSDFfs.close();
      }
      oechem.OEWriteMolecule(ofs, mol);      
      ifs.close();      
      ofs.close();
      System.exit(1);
   }
	
 
}