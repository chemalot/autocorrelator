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
import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;


public class Parameter implements Serializable
{  private static final long serialVersionUID = 1L;

   /** the id is a unique String made of jobId.paramaterName, userd for deserializing */
   private final String id;
   transient private final String name;
   transient private final String type;
   transient private final String[] values;
   transient private final ParameterValue defaultValue;
   
   @SuppressWarnings("unchecked")
   public Parameter(Element xml, String parentId)
   {  this.name = xml.getAttributeValue("name");
      this.type = xml.getAttributeValue("type");
      this.id = parentId + '.' + name;
      
      if( Settings.isDebugMode() )
         System.err.println(this.name + "\t" + this.type);  
      if("list".equals(type) || "value".equals(type))
         values = parseListPatameter(xml.getChildren("value"));
      else if("range".equals(type))
      {  if("int".equals(xml.getAttributeValue("numType")))
            values = parseIntRangeParameter(xml);
         else
            values = parseRangeParameter(xml);
      }else if("toggle".equals(type))
      {  values = parseToggleParameter(xml);
      } else
         throw new Error("unknown parameter type " + type);

      defaultValue = parseDefaultValue(xml.getAttributeValue("default"));
   }

   /**
    * Unique id for this paramater consisting of parentId . name.
    */
   public String getId()
   {  return id;
   }

   public String getName()
   {  return name;
   }

   public String getType()
   {  return type;
   }
   
   public String[] getValues()
   {  return values;
   }

   private ParameterValue parseDefaultValue(String defVal)
   {  if(defVal == null) 
         return null;
      
      if(type.equals("toggle") 
               && !defVal.equals("true") && !defVal.equals("false") )
      {  throw new Error("toggle parameter must hafe true or false default value");
      }
   
      if(type.equals("range")) 
      {  @SuppressWarnings("unused")  // just for format checking
         double val = Double.parseDouble(defVal);
      }
   
      return new ParameterValue(this, defVal);
   }

   private String[] parseListPatameter(List<Element> children)
   {  ArrayList<String> vals = new ArrayList<String>();
      
      for(Element child: children)
         vals.add(child.getTextTrim());
      
      return vals.toArray(new String[vals.size()]);
   }

   private String[] parseValueParameter(Element xml)
   {  ArrayList<String> vals = new ArrayList<String>();
      vals.add(xml.getAttributeValue("value"));
      
      return vals.toArray(new String[vals.size()]);
   }
   
   private String[] parseIntRangeParameter(Element xml)
   {  ArrayList<String> vals = new ArrayList<String>();
      int min  = Integer.parseInt(xml.getAttributeValue("from"));
      int max  = Integer.parseInt(xml.getAttributeValue("to"));
      int step = Integer.parseInt(xml.getAttributeValue("step"));
      while(min<=max)
      {  vals.add(Integer.toString(min));
         min += step;
      }
      return vals.toArray(new String[vals.size()]);
   }
   
   private String[] parseRangeParameter(Element xml)
   {  ArrayList<String> vals = new ArrayList<String>();
      double min  = Double.parseDouble(xml.getAttributeValue("from"));
      double max  = Double.parseDouble(xml.getAttributeValue("to"));
      double step = Double.parseDouble(xml.getAttributeValue("step"));
      while(min<=max)
      {  vals.add(Double.toString(min));
         min += step;
      }
      return vals.toArray(new String[vals.size()]);
   }
   
   private String[] parseToggleParameter(@SuppressWarnings("unused")Element xml)
   {  ArrayList<String> vals = new ArrayList<String>();
      vals.add("true");
      vals.add("false");
      return vals.toArray(new String[vals.size()]);
   }   

   public int getNValues()
   {  return values.length;
   }

   public String getValue(int i)
   {  return values[i];
   }

   public ParameterValue getDefaultValue()
   {  if(defaultValue == null) 
         throw new Error(getName() + " does not have defaultValue!");
   
      return defaultValue;
   }
   
   // This method is called immediately after an object of this class is deserialized.
   // This method returns the singleton instance.
   protected Object readResolve()
   {  Parameter param = Settings.getConfigFile().getJobParameter(this.id);
      if(param == null) 
         throw new Error(String.format(
           "Error desirializing parameter %s: parameter does not exist in configfile!",
                  this.id));
      
      return param;
   }

   public String getRandomValue()
   {  return values[Settings.MYRandom.nextInt(values.length)];
   }
}
