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
import java.util.HashSet;
import java.util.List;
import autocorrelator.ac.*;
import autocorrelator.apps.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


public class GA
{  private static final String EXPLAIN=
      "gaStep [-debug][-waitForKey] [-queOpts 'opts'] -mutationProb 0.1\n"
     +"   -breedingPool 20 -childPool 15 -prefix namePrefix xmlConfig cycle\n"
     +"  childPool: number of children to create.\n"
     +"  breedingPool: number of parents from which to create children.\n"
     +"\n";

   private static final int MAX_TRIES = 500;
   private final int cycle;
   private final double mutationProbability;
   private final int breedingPoolSize;
   private final int childPoolSize;
   
   public GA(Element rootElement, int cycle, double mutationProb, 
             int breedingPoolSize, int childPoolSize)
   {  Settings.readConfigFile(rootElement);
      this.cycle = cycle;
      this.mutationProbability = mutationProb;
      this.breedingPoolSize = breedingPoolSize;
      this.childPoolSize = childPoolSize;
   }
   

   public static void main(String[] args)
   throws IOException, Exception
   {  CommandLineParser cParser;
      String[] modes    = {"-debug", "-waitForKey" };
      String[] parms    = {"-prefix", "-mutationProb", "-breedingPool", "-childPool", "-queOpts", "-xmlFile", "-cycle"  };
      String[] reqParms = {"-prefix", "-mutationProb", "-breedingPool", "-childPool", "-xmlFile", "-cycle" };
      
      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      String[] restArgs = cParser.getRestArgs();
      /*if(restArgs.length != 2)
      {  System.err.println(EXPLAIN);
         System.exit(1);
      }*/
      String xmlFile = cParser.getValue("-xmlFile");
      int cycle = Integer.parseInt(cParser.getValue("-cycle"));
      
      double mutationProb = Double.parseDouble(cParser.getValue("-mutationProb"));
      int breedingPool = Integer.parseInt(cParser.getValue("-breedingPool"));
      int childPool = Integer.parseInt(cParser.getValue("-childPool"));
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
      
      GA ga = new GA(xmlDoc.getRootElement(), cycle, mutationProb, breedingPool, childPool);
      ga.run();
   }
   
   
   private void run() throws DAException, SQLException, IOException
   {  List<Chromosome> chromosomes = Chromosome.readChromosomes();
      HashSet<Chromosome> allChromosomes = 
            new HashSet<Chromosome>(chromosomes.size() + childPoolSize);
      allChromosomes.addAll(chromosomes);
      
      // sort by quality
      Collections.sort(chromosomes, new Comparator<Chromosome>()
               {  public int compare(Chromosome o1, Chromosome o2)
                  {  return -Double.compare(o1.getQuality(), o2.getQuality());
                  }
               });
      if(chromosomes.size() > breedingPoolSize) 
         chromosomes = chromosomes.subList(0, breedingPoolSize);
      
      if(chromosomes.size() == 0)
         throw new Error("No chromosomes found!");

      if(chromosomes.size() == 1)
         throw new Error("There is only a single chromosome!");

      // mutate and crossover
      Chromosome[] children = new Chromosome[childPoolSize];
      int nChild = 0;
      int nTries = 0;
      while(nChild < childPoolSize)
      {  int p1 = Settings.MYRandom.nextInt(chromosomes.size());
      
         int p2;
         while((p2 = Settings.MYRandom.nextInt(chromosomes.size())) == p1)
         {  // look for next random number until p1 and p2 differ
         }
         
         Chromosome c = chromosomes.get(p1).crossOver(chromosomes.get(p2), cycle);
         c = c.mutate(mutationProbability, cycle);
         
         if(allChromosomes.contains(c))
         {  if(nTries++ > MAX_TRIES)
               throw new Error(String.format(
                   "Tried %d times to create new unique chromosome but failed, "
                  +"maybe this run is converged.", nTries));
            continue;
         }
         nTries = 0;
         allChromosomes.add(c);
         children[nChild++] = c;
      }
      
      if(Settings.isDebugMode()) 
         System.err.println("\n\n=========== executing chromosomes");
      
      for(Chromosome c : children)
      {  c.execute(children.length);
         if(Settings.isDebugMode())  System.err.println(c);
      }
   }  
}
