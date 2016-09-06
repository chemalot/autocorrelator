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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class GoldRunner extends JobRunner
{	protected final String execName; 

	GoldRunner(String jobType, JobExecution parentExec, String execName)
	{	super(jobType, parentExec);
      this.execName = execName;
	}
	
   String writeGoldConfFile(ParameterValue[] paramValues) throws FileNotFoundException
   {  String confName = execName + getJobName() + ".conf";
      if( Settings.isDebugMode() )
         System.err.println(confName);
      
      //BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("goldTemplate.conf")));
      BufferedReader in = new BufferedReader(new FileReader("goldTemplate.conf"));
      PrintStream out;
      try
      {  //BufferedReader in = new BufferedReader(new FileReader("goldTemplate.conf"));
         out = new PrintStream(new FileOutputStream(confName));
         String line;
         while((line=in.readLine()) != null)
         {  String tokens[] = line.trim().split(" +");
            if(tokens.length != 0) 
            {  boolean found = false; 
               for(ParameterValue pv : paramValues)
               {  
                  if(tokens[0].equals(pv.getParameter().getName()))
                  {   out.printf("%s = %s\n", pv.getParameter().getName(), pv.getValue());
                     found = true;
                  }
               }
               if(!found)
                  out.println(line);
            }else
            {  out.println(line);
            }
         }
      } catch (IOException e)
      {  throw new Error(e);
      }
      return confName;
   }
   
   @Override
   String getCleanupCommand(ParameterValue[] paramValues)
   {  //return "Cleanup: " + getExecutionCommand(paramValues, 0, parentExec);
      return null;
   }
   

   @Override
   String writeCsh(ParameterValue[] paramValues, String postJobCommands) throws IOException
   {  String currentDir = System.getenv("PWD");
      String confFileName = writeGoldConfFile(paramValues);
      
      String cshName = Settings.getNamePrefix() + "_" + execName 
                     + "_" + getJobId() + ".csh";    
      PrintStream out = new PrintStream(new FileOutputStream(cshName));
   
      out.println("#!/bin/csh -f\n");
      out.println("cd $TMPDIR\n");
      out.println("echo $HOST\n");
      out.printf("cp %s/*.pdb .\n", currentDir);
      out.printf("cp %s/*.mol2 .\n", currentDir);
      out.printf("cp %s/*.mol .\n", currentDir);
      out.printf("cp %s/%s .\n", currentDir, parentExec.getOutputFileName());
      out.printf("babel3 -in %s/%s -out gold_in.sdf \n", 
               currentDir, parentExec.getOutputFileName());
      out.printf("cp %s/%s .\n", currentDir, confFileName);
      out.printf("%s %s\n", execName, confFileName);  // exec gold
      out.printf("sdfTagTool.csh -in gold.sdf -rename \"TITLE=AC_NUMBER\" -split \"|\" -splitTag AC_NUMBER -out %s/%s\n", currentDir, getOutputFileName());   
      out.printf("babel3 -in gold.sdf -out %s/%s\n", 
                  currentDir, getOutputFileName());

      out.println(postJobCommands);
      
      out.println("time");
      
      if(Settings.isDebugMode())
         printDirCopyStatments(out, currentDir, cshName);
      
      out.close();
      
      return cshName;
   }

   @Override
   void writeCleanUpCsh(String commandLine, int position) throws IOException
   {
      // TODO Auto-generated method stub
   }

   public String getOutputFileName()
   {  return getJobName() + ".oeb.gz";
   }
}
