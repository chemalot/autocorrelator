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
package autocorrelator.ga;


import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import autocorrelator.apps.*;
import autocorrelator.ac.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;




public class ChromoAnalysis
{  private static final String EXPLAIN=
      "chromoAnalysis [-waitForKey] -breedingPool n -prefix prefix xmlConfig\n"
     +"\n";
   private final int breedPoolSize;

   public ChromoAnalysis(Element rootElement, int breedingPool)
   {  Settings.readConfigFile(rootElement);
      breedPoolSize = breedingPool;
   }
   

   public static void main(String[] args)
   throws IOException, Exception
   {  CommandLineParser cParser;
      String[] modes    = {"-waitForKey" };
      String[] parms    = { "-prefix", "-breedingPool" };
      String[] reqParms = { "-prefix", "-breedingPool" };
      
      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      String[] restArgs = cParser.getRestArgs();
      if(restArgs.length != 1)
      {  System.err.println(EXPLAIN);
         System.exit(1);
      }
      String xmlFile = restArgs[0];
      
      String prefix = cParser.getValue("-prefix");
      
      Settings.setNamePrefix(prefix);
      Settings.setDebugMode(false);
      
      int breedingPool = Integer.parseInt(cParser.getValue("-breedingPool"));
      
      if( cParser.wasGiven("-waitForKey") )
      {  System.err.println("You may now start the debug then press return:");
         System.in.read();
      }
      
      SAXBuilder builder = new SAXBuilder(true);
      builder.setValidation(false);
      Document xmlDoc = builder.build(new File(xmlFile));
      
      ChromoAnalysis ca = new ChromoAnalysis(xmlDoc.getRootElement(), breedingPool);
      ca.run();
   }
   
   
   private void run() throws DAException, SQLException, IOException
   {  List<Chromosome> chromosomes = Chromosome.readChromosomes();
   
      if(chromosomes.size() == 0)
      {  System.err.println("No chromosomes found\n");
         System.exit(1);
      }
      
      // remove chromosome whitout quality (getExecIndex = -1)
      int pos = 0;
      for(int i=0; i< chromosomes.size();i++)
      {  Chromosome c = chromosomes.get(i);
         if(c.getExecIndex() == -1) continue;
         
         chromosomes.set(pos++, c);
      }
      chromosomes = chromosomes.subList(0, pos);
      
      //sort by complition order
      Collections.sort(chromosomes, new Comparator<Chromosome>()
               {  public int compare(Chromosome o1, Chromosome o2)
                  {  int i1 = o1.getExecIndex();
                     int i2 = o2.getExecIndex();
                     if(i1==i2) return 0;
                     if(i1 < i2) return -1;
                     return 1;
                  }
               });
      System.out.printf("cycle\tmax\tavg\tmedian\n");
      int cycle=chromosomes.get(0).getCycle();
      for(int i=0; i< chromosomes.size();i++)
      {  Chromosome c = chromosomes.get(i);
         if(c.getCycle() <= cycle) continue;
         
         List<Chromosome> currentList = chromosomes.subList(0, i);
         Collections.sort(currentList, new Comparator<Chromosome>()
                  {  public int compare(Chromosome o1, Chromosome o2)
                     {  return -Double.compare(o1.getQuality(), o2.getQuality());
                     }
                  });
         
         
         int size = breedPoolSize;
         if(breedPoolSize > i) size = i;
         
         currentList = currentList.subList(0, size);
         int middle = size/2;  // subscript of middle element
         double median;
         if (size%2 == 1)
            median = currentList.get(middle).getQuality();
         else
            median = (  currentList.get(middle).getQuality() 
                      + currentList.get(middle-1).getQuality()) / 2.0;
         
         double avg = 0;
         double max = 0;
         for(int j=0; j<size; j++)
         {  double q = currentList.get(j).getQuality();
            avg += q; 
            if(q > max) max = q;
         }
         avg = avg / size;
         
         System.out.printf("%d\t%g\t%g\t%g\n", cycle, max, avg, median);
         
         cycle = c.getCycle();
      }
   }
}
