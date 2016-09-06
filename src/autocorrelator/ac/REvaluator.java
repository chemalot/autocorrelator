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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import autocorrelator.apps.*;

//create a new molecule
public class REvaluator {

   private static final String EXPLAIN=
         "REvaluator.csh\n" +
         "\t -mode      (sdf, tab)\n";
   
	public static void main(String argv[]) throws IOException 
   {  // Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      String[] modes    = {};
      String[] parms    = {"-tabFile","-runId", "-currentDir"};
      String[] reqParms = {"-tabFile","-runId", "-currentDir"};
      
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      
      BufferedReader tabFile = 
         new BufferedReader(new FileReader(cParser.getValue("-tabFile")));
      String line = tabFile.readLine();
      String tokens[] = line.split("\t");
      System.out.println(tokens.length);
      // print R file
      //printExecutionCommand(tokens.length-3, tokens);
      // run R file
      //Runtime.getRuntime().exec("R --no-save --vanilla < " + cParser.getValue("-RFile"));
      // evaluate R file
      evalRfile(tokens.length, tokens);
      writeBestModel(cParser.getValue("-runId")); 
      
	}
   
   private static void writeBestModel(String runId) throws IOException
   {  BufferedReader rInput = 
         new BufferedReader(new FileReader("eval.txt"));
      double bestModel = 0.000;
      BufferedWriter out = new BufferedWriter(new FileWriter("best.txt"));
      for (String line = rInput.readLine(); line != null; line = rInput.readLine())
      {  //System.out.println(line);
         if (line.contains("AC_NUMBER"))
            continue;
         String[] tokens = line.split("\t");
         if (tokens == null)
              continue;
         if (tokens.length < 1)
            continue;
         if ( !isAlpha(tokens[1]))
            if ( Double.parseDouble(tokens[1]) > bestModel )
               bestModel = Double.parseDouble(tokens[1]);
      }
      out.write(runId + "\t" + bestModel + "\t" + bestModel + "\n");
      out.close();
      rInput.close();
      return;
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
   
   private static void evalRfile(int count, String[] tokens) throws IOException
   {
      // TODO Auto-generated method stub
      BufferedReader rInput = 
         new BufferedReader(new FileReader("correlate.out"));
      BufferedWriter out = new BufferedWriter(new FileWriter("eval.txt"));
      for (int i = 0; i+3 < count; i++)
      {  //System.out.println(count + "\t" + i );
         String line = rInput.readLine();
         String[] tokens2 = line.split(" +");
         //if (tokens2.length <= i)
         //   break;
         out.write(tokens[i+2] + "\t" + tokens2[1] + "\n");         
      }
      String line = rInput.readLine();
      out.close();
      rInput.close();
   }
}