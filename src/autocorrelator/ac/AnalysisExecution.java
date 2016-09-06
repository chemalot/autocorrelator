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




public class AnalysisExecution extends JobExecution
{  private static final long serialVersionUID = 1L;
   final double quality;
   final ParameterValue[] outputVals;
   
   public AnalysisExecution(String jobName, int execId, JobExecution parent, 
            JobDescription desc, ParameterValue[] vals, String outputFileName, 
            ParameterValue[] outputVals, double quality, int cycle)
   {  super(jobName, execId, parent, desc, vals, outputFileName, cycle );
      this.outputVals = outputVals;
      this.quality = quality;
   }
   
   public double getQuality()
   {  return quality;
   }
   
   public ParameterValue[] getOutputParamVals()
   {  return outputVals;
   }
}
