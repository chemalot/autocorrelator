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


import java.io.Serializable;

public class JobExecution implements Serializable
{  private final int cycle;
   private static final long serialVersionUID = 1L;
   private final int executionId;
   private final JobDescription jobDescription;
   private final ParameterValue[] paramVals;
   private final JobExecution parentExecution;
   private final String outputFilename;
   private final String jobName;
   
   /**
    * ExecutionID is a unique number for this execution.
    */
   public JobExecution(String jobName, int execId, JobExecution parent, JobDescription desc, 
                       ParameterValue[] vals, String outputFileName, int cycle )
   {  this.jobName = jobName;
      this.executionId = execId;
      this.jobDescription = desc;
      this.paramVals = vals;
      this.parentExecution = parent;
      this.outputFilename = outputFileName;
      this.cycle = cycle;
   }

   public String getOutputFileName()
   {  return outputFilename;
   }
   
   public JobDescription getJobDescription()
   {  return jobDescription;
   }
   public ParameterValue[] getParamVals()
   {  return paramVals;
   }
   public JobExecution getParentExecution()
   {  return parentExecution;
   }

   public int getExecutionId()
   {  return executionId;
   }
   
   public String getJobName()
   {  return jobName;
   }
   
   public int getCycle()
   {  return cycle;
   }
}
