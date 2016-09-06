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
public class RCorrelator {

   private static final String EXPLAIN=
         "RCorrelator.csh\n" +
         "\t -mode      (sdf, tab)\n";
   
	public static void main(String argv[]) throws IOException 
   {  // Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
      String[] modes    = {};
      String[] parms    = {"-tabFile", "-RFile"};
      String[] reqParms = {"-tabFile", "-RFile"};
      
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      
      BufferedReader tabFile = 
         new BufferedReader(new FileReader(cParser.getValue("-tabFile")));
      String line = tabFile.readLine();
      String tokens[] = line.split("\t");
      System.out.println(tokens.length);
      // print R file
      printExecutionCommand(tokens.length-3, tokens);
      // run R file
      //Runtime.getRuntime().exec("R --no-save --vanilla < autoCorrelate.R" /*+ cParser.getValue("-RFile")*/);
      // evaluate R file
      //evalRfile(tokens.length-3, tokens);
       
      
	}
   
   private static void evalRfile(int count, String[] tokens) throws IOException
   {
      // TODO Auto-generated method stub
      BufferedReader outFile = 
         new BufferedReader(new FileReader("correlate.out"));
      BufferedWriter out = new BufferedWriter(new FileWriter("eval.txt"));
      for (int i = 0; i < count; i++)
      {  String line = outFile.readLine();
         String[] tokens2 = line.split(" +");
         out.write(tokens[i+3] + "\t" + tokens2[1] + "\n");         
      }
      String line = outFile.readLine();
      out.close();
   }

   private static void printExecutionCommand(int count, String[] tokens) throws IOException
   {
      // TODO Auto-generated method stub
      String Rfile = "autoCorrelate.R";
      String currentDir = System.getenv("PWD");
      String modelLine = null, modelType = "none";
      BufferedWriter out = new BufferedWriter(new FileWriter(Rfile));
      
      // R Eval Code (Mostly finished)
      out.write("data<- read.table(\"autoCorrelate.tab\", header=TRUE, sep = '\t')\n");
      out.write("inhib <- data[,2]\n");
      out.write("ic50 <- inhib\n");
      for (int j = 0; j < count; j++)
      {  int i = j + 3;
      out.write("a" + i + " <- data[," + i + "]\n");
      out.write("value" + i + " <- data[," + i + "]\n");
      out.write("v" + i + " <- (var(ic50,value" + i 
      + ",na.rm=TRUE)/sqrt(var(ic50,na.rm=TRUE)*var(value" 
      + i +",na.rm=TRUE)))^2\n");
      }
      //  Should we itorate over this, again and again (Multiple models?)
      // Opening model arguments
      System.out.println("XXX");
      modelType = "linear";
      int k = 0;
      while( k == 0)
      {  
      if (modelType != null)
      {  if (modelType.equals("linear"))
      {  modelLine = "b <- lm(inhib~";
      }
      else if (modelType.equals("pls"))
      {  modelLine = "b.pls <- mvr(";
      }
      else 
      {  System.err.println("No model evaluation selected/supported!!!"); }
      // Close opening switch
      boolean first = true;
      for (int i = 0; i < count; i++)
      {  int j = i + 3;
      if (first)
      {  first = false;
      modelLine = modelLine + "a" + j;
      }
      else
      modelLine = modelLine + "+a" + j;
      }
      
      // Closing model arguments
      if (modelType.equals("linear"))
      {modelLine = modelLine + ")";}
      else if (modelType.equals("pls"))
      {modelLine = modelLine + ",data=b,method=\"simpls\"";}
      // Close closing switch
      if (!modelType.equals("none"))
         out.write(modelLine + "\n");
      } else {
      System.err.println("No correlation model selected!!");
      }
      // add model line
      out.write("");
      out.write("zz <- file(\"" + "correlate.out\", open=\"wt\")\n");      
      out.write("sink(zz)\n");      
      for (int i = 0; i < count; i++)
      {  int j = i + 3;
      out.write("print(v" + j + ")\n");
      }
      if (!modelType.equals("none"))
         out.write("print(summary(b))\n");
      out.close();
      
      // Make sure to return the "Evaluation of the model, etc."
      
      return;  
   }
   }
}