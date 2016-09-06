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
import autocorrelator.apps.*;
import autocorrelator.ac.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;




public class PrintChromosomes
{  private static final String EXPLAIN=
      "printChromosomes [-waitForKey] -prefix prefix xmlConfig\n"
     +"\n";

   public PrintChromosomes(Element rootElement)
   {  Settings.readConfigFile(rootElement);
   }
   

   public static void main(String[] args)
   throws IOException, Exception
   {  CommandLineParser cParser;
      String[] modes    = {"-waitForKey" };
      String[] parms    = { "-prefix" };
      String[] reqParms = { "-prefix" };
      
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
      
      if( cParser.wasGiven("-waitForKey") )
      {  System.err.println("You may now start the debug then press return:");
         System.in.read();
      }
      
      SAXBuilder builder = new SAXBuilder(true);
      builder.setValidation(false);
      Document xmlDoc = builder.build(new File(xmlFile));
      
      PrintChromosomes pc = new PrintChromosomes(xmlDoc.getRootElement());
      pc.run();
   }
   
   
   private void run() throws DAException, SQLException, IOException
   {  
//      try
//      {  Chromosome c1 = readeChromosome("ns_2_R_14.jobExec");
//         Chromosome c2 = readeChromosome("ns_5_R_17.jobExec");
//         HashSet<Chromosome> allChromosomes = 
//            new HashSet<Chromosome>(20);
//         allChromosomes.add(c1);
//         System.err.println(allChromosomes.contains(c2));
//         allChromosomes.add(c2);
//         
//         Chromosome c3 = c1.crossOver(c2, 1);
//         System.err.println(allChromosomes.contains(c3));
//         allChromosomes.add(c3);
//            
//         System.err.printf("%d %d %b %b %d %b %b\n", c1.hashCode(), c2.hashCode(), 
//                  c1.equals(c2), c2.equals(c1), allChromosomes.size(),
//                  allChromosomes.contains(c1), allChromosomes.contains(c2));
//
//         System.err.printf("%d %d %b %b %d %b %b\n", c1.hashCode(), c3.hashCode(), 
//                  c1.equals(c3), c3.equals(c1), allChromosomes.size(),
//                  allChromosomes.contains(c1), allChromosomes.contains(c3));
//      } catch (ClassNotFoundException e)
//      {  e.printStackTrace();
//      }
      
      List<Chromosome> chromosomes = Chromosome.readChromosomes();
      HashSet<Chromosome> allChromosomes = 
            new HashSet<Chromosome>(chromosomes.size());
      allChromosomes.addAll(chromosomes);
      
      // sort by quality
      Collections.sort(chromosomes, new Comparator<Chromosome>()
               {  public int compare(Chromosome o1, Chromosome o2)
                  {  return -Double.compare(o1.getQuality(), o2.getQuality());
                  }
               });
      if(chromosomes.size() == 0)
         throw new Error("No chromosomes found!");

      System.out.println(chromosomes.get(0).getTabHeader());
      for(Chromosome c : chromosomes)
      {  System.out.println(c.toTabString());
      }
   }
}
