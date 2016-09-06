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
import java.util.List;

import org.jdom.Element;

public class TaskDescription implements Serializable
{  private static final long serialVersionUID = 1L;

   private final String id;
   private final Parameter[] parmeters;
   private final String jobType;
   
   @SuppressWarnings("unchecked")
   public TaskDescription(Element xmlJob)
   {  this.id = xmlJob.getAttributeValue("id");
      if(id == null || id.length() == 0)
         throw new Error("ID missing for:" + xmlJob.toString());
      
      this.parmeters = parseParameter(xmlJob.getChild("parameters"));
      
      // if type contains hidden, variables do not use the parameter name on command line
      jobType = xmlJob.getAttributeValue("type");
   }
   
   
   @SuppressWarnings("unchecked")
   private Parameter[] parseParameter(Element params)
   {  if(params == null) return new Parameter[0];
   
      List<Element> paramEle = params.getChildren("parameter");
      Parameter[] parameter = new Parameter[paramEle.size()];
      for(int i=0; i<parameter.length; i++)
         parameter[i] = new Parameter(paramEle.get(i), id);
      
      return parameter;
   }
   
   
   public Parameter[] getParameter()
   {  return parmeters;
   }

   public String getType()
   {  return jobType;
   }
   
   /**
    * unique id for this task in xmlFile.
    */
   public String getId()
   {  return id;
   }

   /**
    * 
    * @return the number of possible parameter and subTask combinations.
    */
   public long getPossibilities()
   {  long posibilities = 1;
      for(Parameter p : getParameter())
         posibilities  *= p.getNValues();

      return posibilities;
   }
}
