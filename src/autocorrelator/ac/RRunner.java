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

public class RRunner extends JobRunner
{  
   private final String responseFile;
   private final String correlationFileName;
   
   RRunner(String jobType, JobExecution parentExec)
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

      String rCommandFile = getJobName() + ".conf";
        
      
      out.println("#!/bin/csh -f");
      out.println("cd $TMPDIR");
      //out.printf("cp %s/%s .\n", currentDir, rCommandFile);
      out.printf("cp %s/%s .\n", currentDir, parentExec.getOutputFileName());
      out.printf("cp %s/%s ./inhibs.tab\n", currentDir, "inhibitors.tab");
      

      // Clean and seperate out the "legal" variables
      // SdfTagTool Handling
      
      String sdfFileName = outFileBase + ".sdf";
      String tagToolCmd = String.format("babel3 -in %s -out a.sdf ",
                           parentExec.getOutputFileName());
      out.println(tagToolCmd);
      
//      String keepVars = "AC_NUMBER";
      
      // split here is only necessary for rocs and eon but should not harm others
      // @TODO instead of this we shoulc have an AC_NUMBER tag fromt he very 
      // begiinning which then is carried thoough the end and we can rely on
      tagToolCmd = String.format("sdfTagTool.csh -in a.sdf -out %s " 
             + "-split '_' -splitTag AC_NUMBER -keepNumeric Y -rename TITLE=AC_NUMBER -keep 'AC_NUMBER'",
             sdfFileName);
      out.println(tagToolCmd);
      // End tagtool handling
      Map<String,String> pvMap = new HashMap<String,String>(paramValues.length); 
      for(ParameterValue pv : paramValues)
         pvMap.put(pv.getParameter().getName(), pv.getValue());
      // Begin sdf2tab conversion handling
      String sdf2TabCmd = String.format("sdfTagTool.csh -in %s -out .sdf -rename TITLE=AC_NUMBER | sdfTagTool.csh -in .sdf -out "
    		  					      + ".sdf -split '|' -splitTag AC_NUMBER -keepNumeric Y  -keep AC_NUMBER " 
    		  					      + "| sdfTagTool.csh -split '_' -splitTag AC_NUMBER -keepNumeric Y -keep AC_NUMBER -in .sdf -out .sdf |sdf2Tab.csh -in .sdf > auto1.tab",
                                        sdfFileName);
      
      out.println(sdf2TabCmd);
      
      out.println("mergeTabs.csh -pivot AC_NUMBER -in1 inhibs.tab -in2 auto1.tab" 
                + " -out autoCorrelate.tab -headers Y");
      out.println("cat autoCorrelate.tab");
      out.println("echo " + pvMap.get("numberOfVariables"));
      // queue script which first executes R then calls java program which calls
      // modelEvaluation method which saves the model object into a file
      // named using the jobId as part of the name 
      out.printf("rCorrelator.csh -tabFile autoCorrelate.tab -RFile %s\n", rCommandFile);
      out.printf("R --no-save --vanilla < autoCorrelate.R\n");
      out.printf("rEvaluator.csh -tabFile autoCorrelate.tab -runId %s -currentDir %s\n"
               , getOutputFileName(), currentDir);
      out.printf("cp eval.txt %s/%s\n", currentDir, getOutputFileName());
      out.printf("cat best.txt >> %s/%s\n", currentDir, Settings.getCorrelationFilename());
      
      out.println(postJobCommands);
      out.println("time");
      
      if(Settings.isDebugMode())
         printDirCopyStatments(out, currentDir, cshName);
      
      out.close();
      
      return cshName;
   }

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
//      String Rfile = commandLine + "_" + "autoCorrelate.R";
   }

   @Override
   public String getOutputFileName()
   {  return getJobName() + ".log";
   }
}
