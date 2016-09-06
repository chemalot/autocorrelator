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


public class DAException extends Exception
{  public DAException(String msg)
   {  super(msg);
   }

   public DAException(Throwable rootCause)
   {  super(rootCause);
   }

   public DAException(String msg,Throwable rootCause)
   {  super(msg, rootCause);
   }

   public void printRootStackTrace()
   {  if(getCause() == null)
         printStackTrace(System.err);
      else
         getCause().printStackTrace(System.err);
   }
}