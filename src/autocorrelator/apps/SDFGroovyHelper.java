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
package autocorrelator.apps;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import openeye.oechem.OEMolBase;
import openeye.oechem.oechem;


/**
 * Implement static conversion functions to allow for shorter groovy scripts
 */
public final class SDFGroovyHelper
{  private SDFGroovyHelper()
   {/*static*/}
   private static final Pattern OPERATORCleaner = Pattern.compile("^[^\\d.-]+");

   public static final Integer i(String s)
   {  if( s == null ) return null;
      s = s.trim();
      if( s.length() == 0 ) return null;
      try
      {  return new Integer(s);
      } catch(NumberFormatException e)
      {  System.err.println("sdfGroovy: NumberFormatException " + s);
         return null;
      }
   }

   public static final BigDecimal f(String s)
   {  if( s == null ) return null;
      s = s.trim();
      if( s.length() == 0 ) return null;

      try
      {  return new BigDecimal(s);
      } catch(NumberFormatException e)
      {  System.err.println("sdfGroovy: NumberFormatException " + s);
         return null;
      }
   }

   public static final BigDecimal f(String s, final boolean removeOperator)
   {  if( s == null ) return null;

      if( removeOperator)
         s = OPERATORCleaner.matcher(s).replaceAll("");
      return f(s);
   }

   public static final String tVal( OEMolBase mol, String tagName)
   {  if( tagName == null ) return null;
      return oechem.OEGetSDData(mol, tagName);
   }

   public static final void setVal( OEMolBase mol, String tagName, Object val)
   {  oechem.OESetSDData(mol, tagName, val.toString());
   }

   public static final String avg(String ... args )
   {  int nVals = 0;
      double sum = 0D;

      for(String v: args)
      {  if( v != null && v.length() > 0 )
         {  v = OPERATORCleaner.matcher(v).replaceAll("");
            sum += Double.parseDouble(v);
            nVals++;
         }
      }
      if( nVals == 0 ) return null;

      return new BigDecimal(sum/nVals).toString();
   }
}

