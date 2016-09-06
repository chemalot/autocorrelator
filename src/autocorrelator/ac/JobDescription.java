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


import java.util.List;

import org.jdom.Element;

public class JobDescription extends TaskDescription
{  private static final long serialVersionUID = 1L;

   transient private final JobDescription[] childJobs;
   transient private final JobDescription parent;
   transient private final Design experimentDesign;
   transient private final long numberOfExecutions; 
   
   public enum Design
   {  fullFactorial,
      random,
      singleParameter;
   }
   
   @SuppressWarnings("unchecked")
   public JobDescription(Element xmlJob, JobDescription parent)
   {  super(xmlJob);
      Settings.getConfigFile().registerParameter(getParameter());
      Settings.getConfigFile().registerJobSDescription(this);
      
      this.parent = parent;

      List<Element> childEl = xmlJob.getChildren("job");
      childJobs = new JobDescription[childEl.size()];
      for(int i=0; i<childEl.size(); i++)
      {  Element el=childEl.get(i);
         // recursivly build sub-juobs
         childJobs[i] = new JobDescription(el, this);
      }
      
      String dummy  = xmlJob.getAttributeValue("experimentalDesign");
      if(null == dummy|| dummy.length() == 0)
         experimentDesign = Design.fullFactorial;
      else
         experimentDesign = Design.valueOf(dummy);
      
      dummy = xmlJob.getAttributeValue("numberOfExecutions");
      if(null == dummy || dummy.length() == 0)
         numberOfExecutions = 0;
      else
         numberOfExecutions = Integer.parseInt(dummy);
   }
   
   
   JobDescription[] getChildJobs()
   {  return childJobs;
   }

   JobDescription getParent()
   {  return parent;
   }

   JobRunner createJobRunner(JobExecution parentExec)
   {  return JobRunner.factory(parentExec, getType());
   }

   public Design getExperimentDesign()
   {  return experimentDesign;
   }

    /**
    * Only to be used with the random experimental design
    */
   public long getNumberOfExecutions()
   {  return numberOfExecutions;
   }

   
   protected Object readResolve()
   {  JobDescription jd = (JobDescription)
                           Settings.getConfigFile().getJobDescription(getId());
      if(jd == null) 
         throw new Error(String.format(
           "Error desirializing Jobdescription %s: id does not exist in configfile!",
                  getId()));
      
      return jd;
   }
   
   public long getPossibilities()
   {  long posibilities = super.getPossibilities();   //count parameter combinations
      for(JobDescription jd : getChildJobs())
         posibilities  *= jd.getPossibilities();

      return posibilities;
   }


   public int getNParameter()
   {  return getParameter().length;
   }
}
