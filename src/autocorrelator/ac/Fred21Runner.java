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


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Fred21Runner extends OERunner
{
   Fred21Runner(String jobType, JobExecution parentExec, String execName)
   {  super(jobType, parentExec, execName);
   }
   
   @Override
   String getExecutionCommand(ParameterValue[] paramValues)
   {  StringBuilder sb = new StringBuilder();
      
      sb.append(execName);
      
      for( ParameterValue pv: paramValues)
      {  String type = pv.getParameter().getType();
         String name = pv.getParameter().getName();
         String value = pv.getValue();
         
         if( type.equals("toggle") )
         {  if ( value.equals("true") )
               sb.append(" -").append(name);

         // scoring only needs the scrong function attibute
         }else if (name.equals("scoring"))
         {  sb.append(" -").append(value);
            if("zapbind".equals(value))
               sb.append(" -assign_ligand_charges -assign_protein_charges ");
            
         }else
         {  sb.append(" -").append(name).append(" ").append(value);
         }
      }

      sb.append(" -dbase " ).append(parentExec.getOutputFileName());
      sb.append(" -prefix ").append(getJobName());
      sb.append(" -oformat oeb.gz");

      return sb.toString();
   }

   /**
    * Write the csh fiel to execute this job (possibly in a queing system and
    * save teh cshName {@link #getCshName()}.
    * 
    * @param prefix prefix for this autocorrelator run.
    * @param paramValues parameters for this job.
    */
   @Override
   String writeCsh(ParameterValue[] paramValues, String postJobCommands) throws IOException
   {  String currentDir = System.getenv("PWD");
      String jobName = getJobName();
      
      String cshName = Settings.getNamePrefix() + "_" + getExecutableName() 
                     + "_" + getJobId() + ".csh";    
      PrintStream out = new PrintStream(new FileOutputStream(cshName));
      
      out.println("#!/bin/csh -f\n");
      out.println("cd $TMPDIR\n");
      out.printf("cp %s/*.pdb .\n", currentDir);
      out.printf("cp %s/*.mol .\n", currentDir);
      out.printf("cp %s/%s .\n", currentDir,parentExec.getOutputFileName());
      out.println(getExecutionCommand(paramValues));
      
      // @todo we need to normalize tag names or somehow make the names avaialble 
      // for R
      // fred output file name depends on prefix and scoring function
      String fredOutputFile = getScoringFunction(paramValues);
      fredOutputFile = jobName + '_' + fredOutputFile + "_docked.oeb.gz"; 
      out.printf("cp %s %s/%s\n", fredOutputFile, currentDir, getOutputFileName());
      
      out.println(postJobCommands);
      out.println("time");
      
      if(Settings.isDebugMode())
         printDirCopyStatments(out, currentDir, cshName);
      out.close();
      
      return cshName;
   }

   private String getScoringFunction(ParameterValue[] paramValues)
   {  for( ParameterValue pv: paramValues)
      {  String name = pv.getParameter().getName();
         
         if (name.equals("scoring"))
            return pv.getValue();
      }
      throw new Error("No scoring function defined for fred");
   }
   
   @Override
   public String getOutputFileName()
   {  return getJobName() + ".oeb.gz";
   }
}
