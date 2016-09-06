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


import java.util.Random;

import org.jdom.Element;


public class Settings
{  private static String CORRELATIONFileName;

   private static String namePrefix = null;
   private static Boolean debug = null;
   private static ConfigFile configFile = null;
   private static String QUEUEOptions = "";
   private static int executionCounter = 0;
   
   public static void setNamePrefix(String prefix)
   {  assert namePrefix == null : "namePrefix should be set only once";
      namePrefix = prefix;
      CORRELATIONFileName = prefix + "_ModelCorrelation.txt";
   }

   /** for use by GA */
   public static void setNamePrefix(String prefix, int cycle)
   {  assert namePrefix == null : "namePrefix should be set only once";
      namePrefix = prefix + '_' + cycle;
      CORRELATIONFileName = prefix + "_ModelCorrelation.txt";
   }

   public static void setDebugMode(boolean isDebug)
   {  assert debug == null : "DebugMode should be set only once";
      debug = isDebug;
   }
   
   public static void readConfigFile(Element rootElement)
   {  assert configFile == null : "ConfigFile should be read only once!";
      configFile = new ConfigFile();
      configFile.init(rootElement);
   }

   /**
    * Prefix to make filenames and queue naes unique for this autocorrelator run.
    */
   public static String getNamePrefix()
   {  return namePrefix;
   }
   
   public static int getNextExecutionCount()
   {  return executionCounter++;
   }
   
   public static boolean isDebugMode()
   {  return debug;
   }

   public static ConfigFile getConfigFile()
   {  return configFile;
   }

   /**
    * return filename for file storing all correlation results.
    * 
    * This file should contain one line per output file from any runs with the 
    * following format:<br/>
    * outputFileName correlationCoeficient<br/> 
    */
   public static String getCorrelationFilename()
   {  return CORRELATIONFileName;
   }

   public static final Random MYRandom = new Random();

   public static void setQueueOptions(String queOpts)
   {  if(queOpts == null) return;
      QUEUEOptions = queOpts;
   }

   public static String getQueueOptions()
   {  return QUEUEOptions;
   }
}
