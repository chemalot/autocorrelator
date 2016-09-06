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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import openeye.oechem.OEGraphMol;
import openeye.oechem.OESDDataIter;
import openeye.oechem.OESDDataPair;
import openeye.oechem.oechem;
import openeye.oechem.oemolistream;

//create a new molecule
public class CorrelateIC50withScore {
   
   private static final String EXPLAIN=
         "CorrelateFiles " +
         "-ic50 is the label of the IC50 values in the SDF file\n" +
         "-scores sdf file with scores from application\n" +         
         "Output is a list of R^2 values\n";
   
	public static void main(String argv[]) throws IOException,
       InterruptedException 
   {
		// Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      int ic50Position = -1;
      String[] modes    = {};
      String[] parms    = {"-ic50", "-scores"};
      String[] reqParms = {"-ic50", "-scores"};
      String list = "";    
      String currentDir = System.getenv("PWD");
      System.out.println(currentDir);
      
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      oemolistream ifs = new oemolistream(cParser.getValue("-scores"));
      OEGraphMol mol = new OEGraphMol();
      oechem.OEReadMolecule(ifs,mol);
      OESDDataIter pairsIt = oechem.OEGetSDDataPairs(mol);
      int i = 0;
      while(pairsIt.hasNext())
      {  OESDDataPair pair = pairsIt.next();
         i++;
         String tag = pair.GetTag();
         System.out.println(tag);
         if (cParser.getValue("-ic50").equals(pair.GetTag()))
            ic50Position = i;               
         if (isNumeric(pair.GetValue()))
            if(!isAlpha(pair.GetValue()))
               list = list + " " + i;
      }
      String[] valueList = list.split(" ");
      printRfile(ic50Position, valueList, cParser.getValue("-scores"));
      

   }
   
   private static void printRfile(int ic50Position, String[] valueList, String nameS) 
   throws IOException, InterruptedException
   {
      // TODO Auto-generated method stub     
      String Rfile = "autoCorrelate.R";
      String currentDir = System.getenv("PWD");      
      BufferedWriter out = new BufferedWriter(new FileWriter(Rfile)); 
      
      out.write("data<- read.table(\"" + "autoCorrelate.tab\", header=TRUE, sep = '\t')\n");
      out.write("inhib <- data[," + ic50Position + "]\n");
      out.write("ic50 <- log(1/inhib)\n");
      
      for ( int i = 0 ; i < valueList.length; i++ )
      {  if (isNumeric(valueList[i]))
         {      
         int j = Integer.parseInt(valueList[i]);
         out.write("value" + j + " <- data[," + j + "]\n");
         out.write("v" + j + " <- (var(ic50,value" + j 
                  + ",na.rm=TRUE)/sqrt(var(ic50,na.rm=TRUE)*var(value" 
                  + j +",na.rm=TRUE)))^2\n");
         }
      }
      out.write("zz <- file(\"" + "correlate.out\", open=\"wt\")\n");      
      out.write("sink(zz)\n");      
      for ( int i = 0 ; i < valueList.length; i++ )
      {  if (isNumeric(valueList[i]))
         {  int j = Integer.parseInt(valueList[i]);
            out.write("a"+ j + " <- c(names(data)[" + j + "], \" \", v" + j + ")\n");
            out.write("b"+ j + " <- paste(a" + j + ", collapse = \" \")\n");
            //out.write("print(names(data)["+j + "])\n");
            out.write("print(b"+j + ")\n");
         }
      }
      out.close();
   
      return;
   }
   private static boolean isNumeric(String str)
   {
      boolean blnNumeric = false;
      
      char chr[] = null;
      if(str != null)
         chr = str.toCharArray();
 
      for(int i=0; i<chr.length; i++)
      {
         if(chr[i] >= '0' && chr[i] <= '9')
         {
            blnNumeric = true;
            break;
         }
      }
      System.out.println(str + " Numeric? " + blnNumeric);
      return (blnNumeric);
   }
   
   private static boolean isAlpha(String str)
   {
      boolean blnAlpha = false;
   
      char chr[] = null;
      if(str != null)
         chr = str.toCharArray();
      
      for(int i=0; i<chr.length; i++)
      {
         if((chr[i] >= 'A' && chr[i] <= 'Z') || (chr[i] >= 'a' && chr[i] <= 'z'))
         {
            blnAlpha = true;
            break;
         }
      }
      System.out.println(str + " Alpha? " + blnAlpha);      
      return (blnAlpha);
   }
}
 
