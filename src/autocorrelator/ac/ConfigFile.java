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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

public class ConfigFile
{
   private MoleculeSourceDescription molSourceDesc;
   private final List<TaskDescription> jobList = new ArrayList<TaskDescription>();
   private final List<ReportDescription> finalList = new ArrayList<ReportDescription>();
   private final Map<String,JobDescription> idToJobDescMap; 
   private final Map<String,Parameter> idToJobParameterMap;
   
   public ConfigFile()
   {  idToJobDescMap = new HashMap<String, JobDescription>();
      idToJobParameterMap = new HashMap<String, Parameter>();
   }
   
   @SuppressWarnings("unchecked")
   void init(Element rootElement)
   {  assert molSourceDesc == null && jobList.size() == 0
         : "ConfigFile may only be initialized once";
   
      Element srcEl = rootElement.getChild("load");
      molSourceDesc = new MoleculeSourceDescription(srcEl);
      
      for( Element el: (List<Element>)(rootElement.getChildren("job")))
         jobList.add(new JobDescription(el, null));
   
      long posibilities = 1;
      for(TaskDescription jd : jobList)
         posibilities *= jd.getPossibilities();
      
      System.err.printf("Read config file. It has %d possible models\n", posibilities);
      
//   for( Element el: (List<Element>)(rootElement.getChildren("final")))
//      finalList.add(new ReportDescription(el, null, idToJobDescMap, idToParameterMap));
   }


   public List<ReportDescription> getFinalList()
   {  return finalList;
   }

   public List<TaskDescription> getJobList()
   {  return jobList;
   }

   public MoleculeSourceDescription getMolSourceDesc()
   {  return molSourceDesc;
   }
   
   /**
    * Keep track of JobDescriptions to keep JobDescriptions unique in deserialization.
    */
   public void registerJobSDescription(JobDescription jd)
   {  if(idToJobDescMap.containsKey(jd.getId()))
         throw new Error(String.format("Two jobs have the same id=%s",jd.getId()));
   
      idToJobDescMap.put(jd.getId(), jd);
   }
   
   /**
    * Keep track of JobParameters to keep parameter unique in deserialization.
    */
   public void registerParameter(Parameter param)
   {  if(idToJobParameterMap.containsKey(param.getId()))
         throw new Error(String.format("Job has same parameter twice (%s)",param.getId()));
   
      idToJobParameterMap.put(param.getId(), param);
   }

   /**
    * Keep track of JobParameters to keep parameter unique in deserialization.
    */
   public void registerParameter(Parameter[] params)
   {  for(Parameter param : params)
         registerParameter(param);
   }
   
   public Parameter getJobParameter(String pId)
   {  return idToJobParameterMap.get(pId);
   }
   
   public TaskDescription getJobDescription(String id)
   {  return idToJobDescMap.get(id);
   }
}
