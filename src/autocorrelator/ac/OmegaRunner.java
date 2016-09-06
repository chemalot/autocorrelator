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


public class OmegaRunner extends OERunner
{  OmegaRunner(String jobType, JobExecution parentExec, String execName)
   {  super(jobType, parentExec, execName);
   }
   
   @Override
   String getExecutionCommand(ParameterValue[] paramValues)
   {  StringBuilder sb = new StringBuilder();
      boolean hasTransform = false;
      
      sb.append(execName);
      
      for( ParameterValue pv: paramValues)
      {  String type = pv.getParameter().getType();
         String name = pv.getParameter().getName();
         String value = pv.getValue();
         
         if (name.equals("transform"))
         {  if("none".equalsIgnoreCase(value)) continue;
      
            hasTransform = true;
            //insert sdfTransform ahead of omega
            sb.insert(0, 
               String.format("sdfTransformer.csh -makeHExplicit -trans %s -in %s -out .ism|",
                        value, parentExec.getOutputFileName()));
            sb.append(" -in .ism");
            
         }else if(type.equals("toggle") )
         {  if ( value.equals("true") )
               sb.append(" -").append(name);
         
         }else if (type.contains("hidden"))
         {   sb.append(" -").append(value);
         }else
         {  sb.append(" -").append(name)
               .append(" ").append(value);
         }
      }
      
      if(!hasTransform)
         sb.append(" -in ").append(parentExec.getOutputFileName());
      
      sb.append(" -out ").append(getOutputFileName());
      
      return sb.toString();
   }

}
