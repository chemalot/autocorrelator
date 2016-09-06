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
//create a new molecule
public class Tab2Sdf {

   private static final String EXPLAIN=
         "tab2sdf [-in nFile] [-sepRe regExp] -out outFile\n";

   public static void main(String argv[]) 
   {
      CommandLineParser cParser;
      String[] modes    = { };
      String[] parms    = {"-in", "-out", "-sepRe" };
      String[] reqParms = {"-out"};

      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);

      String in   = cParser.getValue("-in");
      String out  = cParser.getValue("-out");
      String sepRe= cParser.getValue("-sepRe");
      if( sepRe == null ) sepRe = "\t";

      try 
      {
         oemolostream ofs = new oemolostream(out);

         InputStreamReader isr;
         if( in == null )
           isr = new InputStreamReader(System.in);
         else
           isr = new FileReader(in);
         BufferedReader br = new BufferedReader(isr);

         OEGraphMol mol;
         String str;
         int i = 0;
         String[] headerS = null;
         boolean firstLine = false;
         
         mol = new OEGraphMol();
         while ( (str = br.readLine()) != null )
         {
            if ( firstLine == false )
            {  headerS = str.split(sepRe); 
               firstLine = true;
               System.err.println("Number of columns " + headerS.length);
               continue; 
            }
            String[] tokensS = str.split(sepRe);   
            // System.err.println("Number of values " + tokensS.length); 
                 // convert the string into a molecule
            tokensS[0].trim();
            oechem.OEParseSmiles(mol, tokensS[0]);
            // Store the data associated with the molecule
            int nFields = Math.min(headerS.length, tokensS.length);
            for ( int j = 0; j < nFields; j++ )
               oechem.OESetSDData(mol, headerS[j], tokensS[j]);
            oechem.OEWriteMolecule(ofs, mol);
            i++;
            mol.Clear();
         }
      }
      catch (Exception e) 
      {
        System.err.println(e);
      }
   }
}
