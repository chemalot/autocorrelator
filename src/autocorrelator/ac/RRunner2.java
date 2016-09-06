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
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class RRunner2 extends JobRunner
{  private final String responseFile;
   private final String correlationFileName;
   
   RRunner2(String jobType, JobExecution parentExec)
   {  super(jobType, parentExec);
      this.responseFile = Settings.getConfigFile().getMolSourceDesc()
                     .getMoleculeSource().getTabDataFileName();
      this.correlationFileName = Settings.getCorrelationFilename();
   }

   @Override
   String getCleanupCommand(ParameterValue[] paramValues)
   {
      // TODO Auto-generated method stub
      
      return null;
   }
   
   @Override
   String writeCsh(ParameterValue[] paramValues, String postJobCommands) throws IOException
   {  String currentDir = System.getenv("PWD");
      String outFileBase = Settings.getNamePrefix() + "_R_"+ getJobId();    
      String cshName     = outFileBase + ".csh";
      
      PrintStream out = new PrintStream(new FileOutputStream(cshName));

      Map<String,String> pvMap = new HashMap<String,String>(paramValues.length); 
      for(ParameterValue pv : paramValues)
         pvMap.put(pv.getParameter().getName(), pv.getValue());
      
      out.println("#!/bin/csh -f");
      out.println("echo $HOSTNAME");
      out.println("cd $TMPDIR");
      out.printf("cp %s/%s .\n", currentDir, parentExec.getOutputFileName());
      out.printf("cp %s/%s ./inhibs.tab\n", currentDir, responseFile);

      out.printf("acAnalyse.pl -mol %s -tab inhibs.tab -nvar %s -responseTag IC50 "
               + "-method %s -outR2File r2file.txt -log %s\n",
               parentExec.getOutputFileName(), 
               pvMap.get("numberOfVariables"),
               pvMap.get("method"),
               getOutputFileName());
      
      out.printf("echo %s `cat r2file.txt` >> %s/%s\n",
               getOutputFileName(),
               currentDir, correlationFileName);
   
      out.printf("cp %s %s\n", getOutputFileName(), currentDir);
   
      out.println(postJobCommands);
      out.println("time");
      
      if(Settings.isDebugMode())
         printDirCopyStatments(out, currentDir, cshName);
      
      out.close();
      
      return cshName;
   }

   @Override
   public JobExecution executeJob(JobDescription jd, ParameterValue[] paramVals, 
            JobExecution parent, int cycle, String postJobCommands) throws IOException
   {  JobExecution jobExec = super.executeJob(jd, paramVals, parent, cycle, postJobCommands);
   
      String outFileBase = Settings.getNamePrefix() + "_R_" + getJobId();    
      String jobExecFile = outFileBase + ".jobExec";
      ObjectOutputStream oStrm = new ObjectOutputStream(new FileOutputStream(jobExecFile, true));
      oStrm.writeObject(jobExec);
      oStrm.close();
      
      return jobExec;
   }
   
   @Override
   void writeCleanUpCsh(String commandLine, int position) throws IOException
   {  // TODO Auto-generated method stub
   }

   @Override
   public String getOutputFileName()
   {  return getJobName() + ".log";
   }
}
