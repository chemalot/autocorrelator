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


import java.io.IOException;
import java.io.PrintStream;

public abstract class JobRunner
{  private final String jobType;
   private final int jobId;

   // parentExec is mutable, it keeps track of the number of childExecutions 
   protected final JobExecution parentExec;
   
   JobRunner(String jobType, JobExecution parentExec)
   {  this.jobType = jobType;
      this.parentExec  = parentExec;
      this.jobId = Settings.getNextExecutionCount();
   }
   
   
   public static JobRunner factory(JobExecution parentExec, String jobType)
   {  if("omega2".equals(jobType))
         return new OmegaRunner(jobType, parentExec,"omega2");
//      if("omega1.8".equals(jobType))
//         return new OmegaRunner1_8(jobType, parentExec,"omega1.8");
      if("rocs".equals(jobType))
         return new RocsRunner(jobType, parentExec,"rocs");
      if("fred2.2".equals(jobType))
         return new FredRunner(jobType, parentExec,"fred2.2");
      if("fred2.1".equals(jobType))
         return new Fred21Runner(jobType, parentExec,"fred2");
      if("eon".equals(jobType))
         return new EonRunner(jobType, parentExec,"eon");
      if("szybki".equals(jobType))
         return new SzybkiRunner(jobType, parentExec,"szybki");
      if("gold_auto".equals( jobType))
         return new GoldRunner(jobType, parentExec,"gold_auto");
      if("R".equals(jobType))
         return new RRunner(jobType, parentExec);
      if("R2".equals(jobType))
         return new RRunner2(jobType, parentExec);
//      if("output".equals(jobType))
//         return ReportRunner(jobType,parentExec,"illuUpdate");
      throw new Error("Unknown JobType: " + jobType);
   }
   
   abstract String writeCsh(ParameterValue[] paramValues, String postJobCommands) throws IOException;
   abstract public String getOutputFileName();

   abstract String getCleanupCommand(ParameterValue[] paramValues);
   abstract void writeCleanUpCsh(String commandLine, int position) throws IOException;

   
   public String getTypeName()
   {  return jobType;
   }

   public int getJobId()
   {  return jobId;
   }

   public String getJobName()
   {  StringBuilder sb = new StringBuilder(40);
      sb.append(Settings.getNamePrefix());
      sb.append(getTypeName());
      sb.append(jobId);
      return sb.toString();
   }

   public JobExecution executeJob(JobDescription jd, ParameterValue[] paramVals, 
             JobExecution parent, int cycle, String postJobCommands) throws IOException
   {  String jobName        = getJobName();
      String cshName        = writeCsh(paramVals, postJobCommands);
      String outputFileName = getOutputFileName();
      String parentName     = parent.getJobName();
      String queueOptions   = Settings.getQueueOptions();
   
      Process p = Runtime.getRuntime().exec(
        String.format("qsub -N %s -hold_jid %s %s %s",jobName, parentName, 
                                                      queueOptions, cshName));
      try
      {  p.waitFor();
         p.getInputStream().close();
         p.getOutputStream().close();
         p.getErrorStream().close();
      } catch (InterruptedException e)
      {  throw new Error("Should not happen", e);
      }
   
      return new JobExecution(jobName, jobId, parent, jd, paramVals, outputFileName, cycle);
   }

   protected void printDirCopyStatments(PrintStream out, String baseDir, String cshName)
   {  out.printf("cp -r . %s/%s\n", baseDir, getJobName());
      out.printf("cp %s/%s %s/%s\n", baseDir, cshName, baseDir, getJobName());
   }
}
