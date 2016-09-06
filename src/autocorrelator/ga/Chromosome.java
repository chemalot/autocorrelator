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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import autocorrelator.apps.*;
import autocorrelator.ac.*;



public class Chromosome
{  private final int cycle;
   private final double quality;
   private final int execIndex;
   private final String[] qualityStrings;
   private final JobDescriptionAndValues[] allJobs;
   private final JobExecution rootExecution;
   private String parent1 = "";
   private String parent2 = "";
   
   /**
    * A chromosome for the autocorrelator GA is a sequence of jobs usually 
    * starting with an omega job and ending with a R job correlating the results.
    * 
    * A Chromosome is not mutable, all methods in this class retunring a modified 
    * Chromosome will have created a new instance and all fields will be deep copies.
    *  
    * @param baseExecution the jobExecution which yielded the results.
    */
   public Chromosome(JobExecution baseExecution) throws IOException
   {  List<JobExecution> jobSeqList = new ArrayList<JobExecution>();
      
      JobExecution exec = baseExecution;
      while(exec != null)
      {  jobSeqList.add(exec);
         exec = exec.getParentExecution();
      }
      rootExecution = jobSeqList.get(jobSeqList.size()-1);
      
      allJobs = new JobDescriptionAndValues[jobSeqList.size()];
      // jobSeqList is in last executed order but allJobs is in first to execute order
      JobExecution[] jobSequence = new JobExecution[jobSeqList.size()];
      for(int i=0, j=jobSequence.length-1; i<jobSequence.length; i++, j--)
      {  JobExecution je = jobSeqList.get(i);
         allJobs[j] = new JobDescriptionAndValues(je);;
      }

      QualityInfo qi = readQuality(baseExecution);
      qualityStrings = qi.qualStr;
      execIndex = qi.execIndex;
      
      if(qualityStrings.length > 1)
         quality = Double.parseDouble(qualityStrings[1]);
      else
         quality = 0;
      
      cycle = allJobs[allJobs.length-1].jobExec.getCycle();
   }
   
   private Chromosome(JobDescriptionAndValues[] jobs, JobExecution rootExecution, int cycle)
   {  this.allJobs = jobs;
      this.quality = Double.NaN;
      this.qualityStrings = new String[0];
      this.execIndex = 0;
      this.rootExecution = rootExecution;
      this.cycle = cycle;
   }

   /**
    * @return array of all possible chromosomes which differ only in one 
    *    parametervalue from this. 
    */
   public Chromosome[] getAllSingleChanged(int cycle)
   {  List<Chromosome> all = new ArrayList<Chromosome>();
   
      // start with 1 we are ignoring the loadData job for now
      for(int flexJobIdx=1; flexJobIdx<allJobs.length; flexJobIdx++)
      {  for(int flexParamIdx=0; flexParamIdx<allJobs[flexJobIdx].jobDesc.getNParameter(); flexParamIdx++)
         {  Parameter flexParam = allJobs[flexJobIdx].jobDesc.getParameter()[flexParamIdx];
            
            for(String newParamVal: flexParam.getValues())
            {  JobDescriptionAndValues[] newJobs = allJobs.clone();
               
               // clone  
               // start with 1 we are ignoring the loadData job for now               
               for(int j=0; j<newJobs.length; j++)
               {  newJobs[j] = new JobDescriptionAndValues(newJobs[j]);
               }
               
               newJobs[flexJobIdx].paramValues[flexParamIdx] =
                  new ParameterValue(flexParam, newParamVal);
               all.add(new Chromosome(newJobs, rootExecution, cycle));
            }
         }
      }
      return all.toArray(new Chromosome[all.size()]);
   }

   /**
    * @param rate the probablyity of mutating a single parameterValue, note the 
    *   probablility of mutating chromosome in any location is 
    *   rate * paramaterValue(having variablility).
    * @return a newly created chromosome possibly mutated. 
    */
   public Chromosome mutate(double rate, int cycle)
   {  JobDescriptionAndValues[] newJobs = allJobs.clone();
   
      // TODO Chuck suggested gaussian or kaussian distribution for probablility
      for(int j=0; j<newJobs.length; j++)
      {  // clone  
         newJobs[j] = new JobDescriptionAndValues(newJobs[j]);
      
         ParameterValue[] pVals = newJobs[j].paramValues;
         for(int p=0; p<pVals.length; p++)
         {  if( Settings.MYRandom.nextDouble() < rate )
            {  Parameter param = pVals[p].getParameter();
               String newVal = param.getRandomValue();
               pVals[p] = new ParameterValue(param,newVal);
            }
         }
      }
      return new Chromosome(newJobs, rootExecution, cycle);
   }
 
   public Chromosome crossOver(Chromosome partner, int cycle)
   {  // 1. count paramater with variable values (those with more than 1 posibility)
      // 2. select crossover point (cp) at random
      // 3. create new chromosome from values of this with n < cp 
      //    and values from partner with n > cp

      int nVariableParam = 0;
      for(int j=0; j<allJobs.length; j++)
      {  ParameterValue[] pVals = allJobs[j].paramValues;
         for(int p=0; p<pVals.length; p++)
         {  if(pVals[p].getParameter().getNValues() > 1)
               nVariableParam++;
         }
      }
         
      if(nVariableParam < 2)
         throw new Error("Needed >=2 variable parameters, found only:" + nVariableParam);
      
      int crossOverPoint = Settings.MYRandom.nextInt(nVariableParam-1);
      
      JobDescriptionAndValues[] newJobs = allJobs.clone();
      for(int j=0; j<newJobs.length; j++)
      {  // clone  
         newJobs[j] = new JobDescriptionAndValues(newJobs[j]);
         JobDescriptionAndValues partnerJob = partner.allJobs[j];
         
         ParameterValue[] pVals = newJobs[j].paramValues;
         for(int p=0; p<pVals.length; p++)
         {  if(pVals[p].getParameter().getNValues() <= 1) continue;
            
            if(crossOverPoint-- < 0 )
            {  pVals[p] = partnerJob.paramValues[p];
            }
         }
      }

      Chromosome newChr = new Chromosome(newJobs, rootExecution, cycle);
      if(this   .qualityStrings.length > 0) newChr.parent1 = this.qualityStrings[0];
      if(partner.qualityStrings.length > 0) newChr.parent2 = partner.qualityStrings[0];
      
      return newChr;
   }
   
   
   /**
    * @return array of strings first is output name of correlation callcualtion
    *         followed by an abritrary number of quality parameter; the GA only 
    *         usess the first quality parameter for ranking.
    */
   private QualityInfo readQuality(JobExecution rExecution) throws IOException
   {  BufferedReader in = new BufferedReader(
                           new FileReader(Settings.getCorrelationFilename()));
      try
      {  String line;
         String outFilename = rExecution.getOutputFileName();
         int pos = -1;
         while((line=in.readLine()) != null)
         {  pos++;
            if(! line.startsWith(outFilename)) continue;
            //format should be "filename R2"
            String vals[] = line.split("\\s+");
            if(vals==null || vals.length<2) continue;
            QualityInfo qi = new QualityInfo(pos, vals);
            return qi;
         }
         System.err.printf("Line with quality not found for %s in %s.\n", 
                  outFilename, Settings.getCorrelationFilename());
         QualityInfo qi = new QualityInfo(-1, 
                                 new String[] { rExecution.getOutputFileName() });
         return qi;
      }finally
      {  in.close();
      }
      
   }

   /**
    * Execute sequence off jobs as described by this chromosome.
    * @throws IOException 
    */
   public void execute(int nChildren) throws IOException
   {  
      String basePrefix = Settings.getNamePrefix();   // remove cycle number
      basePrefix = basePrefix.substring(0,basePrefix.lastIndexOf("_"));
      String postRJobCommands = String.format(
            "gaReleaseNext.pl -nTotal %d -percentLeft 20 -prefix %s -cycle %d",
                  nChildren, basePrefix, cycle);
      execute(postRJobCommands);
   }

   /**
    * Execute sequence off jobs as described by this chromosome.
    * @throws IOException 
    */
   public void execute() throws IOException
   {  execute("");
   }

   /**
    * Execute sequence off jobs as described by this chromosome.
    * @throws IOException 
    */
   private void execute(String postRJobCommand) throws IOException
   {  
//      DbMoleculeSource moleculeSource = 
//         Settings.getConfigFile().getMolSourceDesc().getMoleculeSource();
//   
//      Parameter[] params = Settings.getConfigFile().getMolSourceDesc().getParameter();
//      moleculeSource.getMolecules(params);
//      
//      int rootId = Settings.getNextExecutionCount();
//      JobExecution rootExecution = new JobExecution("root" , rootId, null, null, null,
//                                       moleculeSource.getMoleculeFileName());

      JobExecution parentExec = rootExecution;
      
      // skip first job for now the first job is the datasource which is currently treated separatly
      for(int j=1; j<allJobs.length; j++)
      {  JobDescriptionAndValues jv = allJobs[j];
         JobDescription jDesc = jv.jobDesc;
         JobRunner runner = JobRunner.factory(parentExec, jDesc.getType());
         ParameterValue[] paramVals = jv.paramValues;
         
         String myPostJobCommands = "";
         if(j == allJobs.length-1) // only on R execution
         {  myPostJobCommands = postRJobCommand;
         }
         parentExec = runner.executeJob(jDesc, paramVals, parentExec, cycle, myPostJobCommands);
      }   
   }

   public double getQuality()
   {  return quality;
   }
   
   public int getCycle()
   {  return cycle;
   }
   
   /**
    * Return the index in the quality file at which this chromosome was returned.
    * This should be the order of job completion
    * @return -1 if no quality was found.
    */
   public int getExecIndex()
   {  return execIndex;
   }
   
   
   /**
    * Tab separated list of Parameter names
    */
   public String getTabHeader()
   {  StringBuilder sb = new StringBuilder(500);
      
      sb.append("cycle");

      for(JobDescriptionAndValues jv : allJobs)
      {  JobDescription jDesc = jv.jobDesc;
         
         String jName = jDesc == null ? "molsource" : jDesc.getType(); 
         sb.append('\t').append(jName);
         
         for(ParameterValue pv : jv.paramValues)
         {  sb.append('\t').append(pv.getParameter().getName());
         }
      }

      for(int i=1; i< qualityStrings.length; i++)
         sb.append('\t').append("Quality").append(i);
      sb.append('\t').append("parent1");
      sb.append('\t').append("parent2");
      
      for(JobDescriptionAndValues jv : allJobs)
      {  JobDescription jDesc = jv.jobDesc;
         String jName = jDesc == null ? "molsource" : jDesc.getType(); 
         sb.append('\t').append(jName);
      }
      return sb.toString();
   }

   /**
    * Tab separated list of parameters
    */
   public String toTabString()
   {  StringBuilder sb = new StringBuilder(500);
      
      sb.append(cycle);
      
      for(JobDescriptionAndValues jv : allJobs)
      {  JobDescription jDesc = jv.jobDesc;
         
         String jName = jDesc == null ? "molsource" : jDesc.getType(); 
         sb.append('\t').append(jName);
         for(ParameterValue pv : jv.paramValues)
         {  sb.append('\t').append(pv.getValue());
         }
      }

      for(int i=1; i< qualityStrings.length; i++)
         sb.append('\t').append(qualityStrings[i]);
      sb.append('\t').append(parent1);
      sb.append('\t').append(parent2);
      
      for(JobDescriptionAndValues jv : allJobs)
      {  JobExecution   jExec = jv.jobExec;
         if(jExec!= null)sb.append('\t').append(jExec.getOutputFileName());
      }
      return sb.toString(); // remove first tab
   }

   /**
    * Human readable representation of Chromosome.
    */
   public String toString()
   {  String preTabs = "";
      StringBuilder sb = new StringBuilder(500);
      
      for(JobDescriptionAndValues jv : allJobs)
      {  JobDescription jDesc = jv.jobDesc;
         JobExecution   jExec = jv.jobExec;
         
         String jName = jDesc == null ? "molsource" : jDesc.getType(); 
         sb.append('\n').append(preTabs).append(jName);
         if(jExec != null)
            sb.append('\t').append(jExec.getOutputFileName());
         sb.append('\n');
         preTabs += '\t';
         
         for(ParameterValue pv : jv.paramValues)
         {  sb.append(preTabs)
              .append(String.format(" %s = %s\n", 
                     pv.getParameter().getName(), pv.getValue()));
         }
      }
      return sb.toString();
   }
   
   @Override
   public int hashCode()
   {  int hashCode = 1;
      for(JobDescriptionAndValues jv : allJobs)
      {  if(jv.jobDesc != null) hashCode = 31*hashCode + jv.jobDesc.hashCode();
         for(ParameterValue pv : jv.paramValues)
         {  hashCode = 31*hashCode + pv.getValue().hashCode();
            hashCode = 31*hashCode + pv.getParameter().hashCode();
         }
      }
      
      return hashCode;
   }
   
   @Override
   public boolean equals(Object o)
   {  if( o == this) return true;
      if(! (o instanceof Chromosome)) return false;
      Chromosome other = (Chromosome)o;
      
      for(int j=0; j<allJobs.length; j++)
      {  JobDescriptionAndValues otherJob = other.allJobs[j];
         if(!otherJob.equals(allJobs[j])) return false;
      }
      
      return true;
   }

   public static List<Chromosome> readChromosomes() 
         throws IOException, FileNotFoundException, Error
   {  List<Chromosome> chromosomes = new ArrayList<Chromosome>();
   
      try
      {  FilenameFilter fFilter = new FilenameFilter()
         {  public boolean accept(File dir, String name) 
            {  return name.endsWith(".jobExec") 
                   && name.startsWith(basePrefix);
            }
         
            final String basePrefix = Settings.getNamePrefix().replaceAll("_\\d+$", "");
         };
         String[] fNames = new File(".").list(fFilter);
         
         if(Settings.isDebugMode()) System.err.println("=========== reading chromosomes");
         
         for(String fName : fNames)
         {  Chromosome c = readeChromosome(fName);
            
            if(c != null)
            {  chromosomes.add(c);
               if(Settings.isDebugMode())  System.err.println(c);
            }
         }
      } catch (ClassNotFoundException e)
      {  throw new Error(e);
      }
   
      return chromosomes;
   }
   
   static Chromosome readeChromosome(String fName) throws IOException, ClassNotFoundException
   {  ObjectInputStream in = new ObjectInputStream(new FileInputStream(fName));
      Object o=in.readObject();
      JobExecution jExec = (JobExecution)o;
      
      Chromosome c = null;
      try
      {  c = new Chromosome(jExec);
      }catch(IOException e)
      {  System.err.printf("Problem reading %s: %s\n", fName, e.getMessage());
      }finally
      {  in.close();
      }
      return c;
   }
}


class JobDescriptionAndValues implements Cloneable
{  public final ParameterValue[] paramValues;
   public final JobDescription jobDesc;
   
   /** if job execution is available */
   public final JobExecution jobExec;

   public JobDescriptionAndValues(JobExecution jobExec)
   {  this.paramValues = jobExec.getParamVals();
      this.jobDesc = jobExec.getJobDescription();
      this.jobExec = jobExec;
   }

   public JobDescriptionAndValues(JobDescriptionAndValues val)
   {  if( val.paramValues != null)
         this.paramValues = (ParameterValue[]) val.paramValues.clone();
      else
         this.paramValues = null;
   
      this.jobDesc = val.jobDesc;
      this.jobExec = null;
   }
   
   public boolean equals(Object other)
   {  if(other == this) return true;
      if(! (other instanceof JobDescriptionAndValues)) return false;
      
      JobDescriptionAndValues otherJDV = (JobDescriptionAndValues) other;
      if(this.jobDesc != otherJDV.jobDesc) return false;
      
      ParameterValue[] pVals = this.paramValues;
      if(pVals.length != otherJDV.paramValues.length) return false;
      for(int p=0; p<pVals.length; p++)
      {  if( ! pVals[p].getValue().equals(otherJDV.paramValues[p].getValue()))
            return false;
      }
      
      return true;
   }
}


class QualityInfo
{  public QualityInfo(int pos, String[] vals)
   {  this.qualStr = vals;
      this.execIndex = pos;
   }
   final String[] qualStr;
   final int execIndex;
}