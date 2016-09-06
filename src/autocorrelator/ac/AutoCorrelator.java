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
import java.util.ArrayList;
import java.util.List;


import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import autocorrelator.ac.JobDescription.Design;
import autocorrelator.apps.*;

public class AutoCorrelator
{  private static final String EXPLAIN=
      "autoComrelator [-debug][-waitForKey] [-queOpts 'opts'] -prefix namePrefix xmlConfig\n";
   
   @SuppressWarnings("unchecked")
   public AutoCorrelator(Element rootElement)
   {  Settings.readConfigFile(rootElement);
   }
   

   public static void main(String[] args)
   throws IOException, Exception
   {  CommandLineParser cParser;
      String[] modes    = {"-debug", "-waitForKey"};
      String[] parms    = {"-prefix", "-queOpts"};
      String[] reqParms = {"-prefix"};
      
      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      String[] restArgs = cParser.getRestArgs();
      if(restArgs.length != 1)
      {  System.err.println(EXPLAIN);
         System.exit(1);
      }
      
      Settings.setQueueOptions(cParser.getValue("-queOpts"));
      Settings.setNamePrefix(cParser.getValue("-prefix"));
      Settings.setDebugMode(cParser.wasGiven("-debug"));

      if( cParser.wasGiven("-waitForKey") )
      {  System.err.println("You may now start the debug then press return:");
         System.in.read();
      }
      
      SAXBuilder builder = new SAXBuilder(true);
      builder.setValidation(false);
      Document xmlDoc = builder.build(new File(restArgs[0]));
      
      AutoCorrelator aCorrelator = new AutoCorrelator(xmlDoc.getRootElement());
      aCorrelator.run();
   }
   
   public void run() throws DAException, SQLException, IOException
   {  
      System.out.println("Running");
      
      System.out.println("Loading");
      DbMoleculeSource moleculeSource = 
         Settings.getConfigFile().getMolSourceDesc().getMoleculeSource();

      Parameter[] params = Settings.getConfigFile().getMolSourceDesc().getParameter();
      moleculeSource.getMolecules(params);
      
      int rootId = Settings.getNextExecutionCount();
      JobExecution rootExecution = new JobExecution("root" , rootId, null, null, 
               new ParameterValue[0], moleculeSource.getMoleculeFileName(), 0);
      System.out.println("Running Jobs");
      for(TaskDescription jd : Settings.getConfigFile().getJobList())
         runJobSet((JobDescription)jd, rootExecution);
   }  

   /**
    * Recursevly run job and child jobs as described in the JobDescription.
    */
   private List<JobExecution> runJobSet(JobDescription jd, JobExecution parent) 
   throws DAException, SQLException, IOException
   {  Parameter[] params = jd.getParameter();
      int count = 0;
      if(params.length > 0)
      {  if(jd.getExperimentDesign() == Design.fullFactorial)
         {  return combinatorialRun(jd, parent, params);
         }else if(jd.getExperimentDesign() == Design.singleParameter)
         {  return singleParameterVariationRun(jd, parent, params);
         }else if(jd.getExperimentDesign() == Design.random)
         {  return randomVariationRun(jd, parent, params);
         }
      }
      count++;
      List<JobExecution> jList = new ArrayList<JobExecution>(1);
      jList.addAll(runSingleJob(jd, new ParameterValue[0], parent));
      return jList;
   }


   /**
    * Execute Job with all combinations of parametervalues.
    * @return List of childJobs executed (only the direct children).
    * @throws SQLException 
    * @throws DAException 
    * @throws IOException 
    */
   private List<JobExecution> combinatorialRun(
                        JobDescription jd, JobExecution parent, Parameter[] params) 
                        throws DAException, SQLException, IOException
   {  int[] paramLength = new int[params.length];
      for(int i=0; i< paramLength.length; i++)
         paramLength[i] = params[i].getNValues();
      int count = 0;
      
      // use MixedBasednumberGenerator to enumerate all combination of parameters;
      MixedBaseNumberConverter mbn = new MixedBaseNumberConverter(paramLength);
      List<JobExecution> jList = new ArrayList<JobExecution>((int)mbn.getMaxValue()+1);

      for(long paramEnum = 0; paramEnum <= mbn.getMaxValue(); paramEnum++)
      {  int[] paramIndexes = mbn.getMixedBasedDigits(paramEnum);
         ParameterValue[] paramVals = new ParameterValue[params.length];
         for(int pPos = 0; pPos<params.length; pPos++)
         {  paramVals[pPos] = new ParameterValue(
                     params[pPos],  params[pPos].getValue(paramIndexes[pPos]));
            
         }
         count++;
         jList.addAll(runSingleJob(jd, paramVals, parent));
      }
      
      return jList;
   }

   /**
    * Executes a random subset of all the possible combinations of ParameterValues.
    * @throws SQLException 
    * @throws DAException 
    * @throws IOException 
    */
   private List<JobExecution> randomVariationRun(JobDescription jd, 
                                       JobExecution parent, Parameter[] params) 
   throws DAException, SQLException, IOException
   {  int[] paramLength = new int[params.length];
      for(int i=0; i< paramLength.length; i++)
         paramLength[i] = params[i].getNValues();
      int count = 0;
      // use MixedBasednumberGenerator to enumerate all combination of parameters;
      MixedBaseNumberConverter mbn = new MixedBaseNumberConverter(paramLength);
      System.out.println(mbn.getMaxValue()+1 + " " + params.length);
      long numExecutions = jd.getNumberOfExecutions();
      CircularRandomNumberGenerator rand = 
                        new CircularRandomNumberGenerator((long)mbn.getMaxValue()+1);
      List<JobExecution> jList = new ArrayList<JobExecution>((int)numExecutions);

      for(long paramEnum = 0; paramEnum < numExecutions; paramEnum++)
      {  int[] paramIndexes = mbn.getMixedBasedDigits(rand.nextValue());
         ParameterValue[] paramVals = new ParameterValue[params.length];
         for(int pPos = 0; pPos<params.length; pPos++)
         {  paramVals[pPos] = new ParameterValue(
                     params[pPos],  params[pPos].getValue(paramIndexes[pPos]));
            
         }
         count++;
         jList.addAll(runSingleJob(jd, paramVals, parent));
      }
      
      return jList;
   }


   /**
    * Vary all variable in turn keeping all the others at their default values.
    * @throws SQLException 
    * @throws DAException 
    * @throws IOException 
    */
   private List<JobExecution> singleParameterVariationRun(JobDescription jd, 
                                       JobExecution parent, Parameter[] params) 
                                       throws DAException, SQLException, IOException
   {  List<JobExecution> jList = new ArrayList<JobExecution>();
      int count = 0;
      for(int varParamIdx=0; varParamIdx< params.length; varParamIdx++)
      {  Parameter varParam = params[varParamIdx];
         for(int variant=0; variant<varParam.getNValues(); variant++)
         {  ParameterValue[] paramVals = new ParameterValue[params.length];
            paramVals[varParamIdx] = 
                  new ParameterValue(varParam, varParam.getValue(variant));
            
            for(int statParam = 0; statParam<params.length; statParam++)
            {  if(statParam == varParamIdx) continue;
               paramVals[statParam] = params[statParam].getDefaultValue();
            }
            count++;            
            jList.addAll(runSingleJob(jd, paramVals, parent));
         }
      }
      return jList;
   }

   /**
    * Execute a single job and all its children.
    * @throws SQLException 
    * @throws DAException 
    * @throws IOException 
    */
   private List<JobExecution> runSingleJob(JobDescription jd, ParameterValue[] paramVals,
            JobExecution parent) throws DAException, SQLException, IOException
   {  JobExecution computeJob = executeJob(jd, paramVals, parent);
      ArrayList<JobExecution> dependend = new ArrayList<JobExecution>();
      
      JobDescription[] childJobs = jd.getChildJobs();
      for(int i=0; i<childJobs.length; i++)
      {  JobDescription child = childJobs[i];
         dependend.addAll(runJobSet(child, computeJob));
      }
      
      dependend.add(computeJob);
      
      //JobExecution jobExec = null;//executeCleanUpJob(jd, paramVals, dependend.toArray(new JobExecution[dependend.size()]));
      
      
      return dependend;
   }

   
   private JobExecution executeJob(JobDescription jd, ParameterValue[] paramVals, 
                                   JobExecution parent) throws IOException
   {  JobRunner runner      = jd.createJobRunner(parent);
      return runner.executeJob(jd, paramVals, parent, 0, "");
   }
}
