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


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import autocorrelator.apps.*;
import autocorrelator.ga.*;

public class LocalOptimizer
{  private static final String EXPLAIN=
      "localOptimizer[-debug][-waitForKey] [-queOpts 'opts']\n"
     +"  -prefix namePrefix xmlConfig cycle\n"
     +"\n";

   private final int cycle;
   
   public LocalOptimizer(Element rootElement, int cycle)
   {  Settings.readConfigFile(rootElement);
      this.cycle = cycle;
   }
   

   public static void main(String[] args)
   throws IOException, Exception
   {  CommandLineParser cParser;
      String[] modes    = {"-debug", "-waitForKey" };
      String[] parms    = {"-prefix", "-queOpts"  };
      String[] reqParms = {"-prefix" };
      
      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      String[] restArgs = cParser.getRestArgs();
      if(restArgs.length != 2)
      {  System.err.println(EXPLAIN);
         System.exit(1);
      }
      String xmlFile = restArgs[0];
      int cycle = Integer.parseInt(restArgs[1]);
      
      String prefix = cParser.getValue("-prefix");
      
      Settings.setNamePrefix(prefix, cycle);
      Settings.setDebugMode(cParser.wasGiven("-debug"));
      Settings.setQueueOptions(cParser.getValue("-queOpts"));

      if( cParser.wasGiven("-waitForKey") )
      {  System.err.println("You may now start the debug then press return:");
         System.in.read();
      }
      
      
      SAXBuilder builder = new SAXBuilder(true);
      builder.setValidation(false);
      Document xmlDoc = builder.build(new File(xmlFile));
      
      LocalOptimizer ga = new LocalOptimizer(xmlDoc.getRootElement(), cycle);
      ga.run();
   }
   
   
   private void run() throws DAException, SQLException, IOException
   {  List<Chromosome> chromosomes = Chromosome.readChromosomes();
      HashSet<Chromosome> allChromosomes = 
            new HashSet<Chromosome>(chromosomes.size() + 200);
      allChromosomes.addAll(chromosomes);
      
      // sort by quality
      Collections.sort(chromosomes, new Comparator<Chromosome>()
               {  public int compare(Chromosome o1, Chromosome o2)
                  {  return -Double.compare(o1.getQuality(), o2.getQuality());
                  }
               });

      Chromosome bestChromo = chromosomes.get(0);
      for(Chromosome child : bestChromo.getAllSingleChanged(cycle))
      {  if(!allChromosomes.contains(child))
         {  if(Settings.isDebugMode()) 
               System.err.println("\n\n=========== executing chromosomes");
         
            child.execute();
         }
         allChromosomes.add(child);
      }
   }  
}
