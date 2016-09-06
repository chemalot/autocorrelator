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

public class ParameterValue implements Serializable
{  private static final long serialVersionUID = 1L;

   private final String value;
   private final Parameter parameter;

   public ParameterValue(Parameter param, String value)
   {  this.parameter = param;
      this.value = value;
   }

   public Parameter getParameter()
   {  return parameter;
   }

   public String getValue()
   {  return value;
   }

   @Override
   public int hashCode()
   {  return parameter.hashCode()*31 + value.hashCode();
   }
}
