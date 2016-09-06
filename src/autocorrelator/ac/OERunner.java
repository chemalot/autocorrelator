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


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class OERunner extends JobRunner
{	protected final String execName;
   
	OERunner(String jobType, JobExecution parentExec, String execName)
	{	super(jobType, parentExec);
      this.execName = execName;
	}
	
   String getExecutionCommand(ParameterValue[] paramValues)
   {  StringBuilder sb = new StringBuilder();
      
      sb.append(execName);
      
      for( ParameterValue pv: paramValues)
      {  String type = pv.getParameter().getType();
         String name = pv.getParameter().getName();
         String value = pv.getValue();
         
         if ( type.equals("toggle") )
         {  if ( value.equals("true") )
               sb.append(" -").append(name);
         // hidden variables do not use the parameter name on command line
         }else if (type.contains("hidden"))
         {  sb.append(" -").append(value);
         }else
         {  sb.append(" -").append(name)
               .append(" ").append(value);
         }
      }
      return sb.toString();
   }

   /**
    * Write the csh fiel to execute this job (possibly in a queing system and
    * save teh cshName {@link #getCshName()}.
    * 
    * @param prefix prefix for this autocorrelator run.
    * @param paramValues parameters for this job.
    */
   @Override
   String writeCsh(ParameterValue[] paramValues, String postJobCommands) throws IOException
   {  String currentDir = System.getenv("PWD");
      
      String cshName = Settings.getNamePrefix() + "_" + getExecutableName() 
                     + "_" + getJobId() + ".csh";    
      PrintStream out = new PrintStream(new FileOutputStream(cshName));

      out.println("#!/bin/csh -f\n");
      out.println("cd $TMPDIR\n");
      out.println("echo $HOST\n");
      out.printf("cp %s/*.pdb .\n", currentDir);
      out.printf("cp %s/*.mol .\n", currentDir);
      out.printf("cp %s/*.mol2 .\n", currentDir);
      out.printf("cp %s/%s .\n", currentDir,parentExec.getOutputFileName());
      out.println(getExecutionCommand(paramValues));
      out.printf("cp %s %s\n", getOutputFileName(), currentDir);
      out.println(postJobCommands);
      out.println("time");
      
      if(Settings.isDebugMode())
         printDirCopyStatments(out, currentDir, cshName);
      out.close();
      
      return cshName;
   }


   void writeCleanUpCsh(String commandLine) throws IOException
   {
//      String currentDir = System.getenv("PWD");
//      String cshFile = this.execName + "_cleanUp.csh";    
//      BufferedWriter out = new BufferedWriter(new FileWriter(cshFile));
//
//      out.write("#!/bin/csh -f\n");
//      out.write("cd " + currentDir + "\n");      
//      out.write("rm -f rocs_*.csh\n");
//      out.write("\n");
//      out.close();
//     // Runtime.getRuntime().exec("chmod 777 " + cshFile);
   }
   
   @Override
   String getCleanupCommand(ParameterValue[] paramValues)
   {  return null;
//      return "Cleanup: " + getExecutionCommand(paramValues, 0, parentExec);
   }
   
   String getExecutableName()
   {  return execName;
   }

   @Override
   void writeCleanUpCsh(String commandLine, int position) throws IOException
   {  // TODO Auto-generated method stub
      
   }

   @Override
   public String getOutputFileName()
   {  return getJobName() + ".oeb.gz";
   }
 }
