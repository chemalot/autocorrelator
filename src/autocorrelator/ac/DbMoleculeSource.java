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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import openeye.oechem.OEGraphMol;
import openeye.oechem.oechem;
import openeye.oechem.oemolistream;
import openeye.oechem.oemolostream;





public class DbMoleculeSource
{  protected final String execName;
//   protected final String mode;
//   protected final String fileName;

   public DbMoleculeSource(String execName)//, String Mode, String Filename)
   {  this.execName = execName;
//      this.mode = Mode;
//      this.fileName = Filename; 
   }

   public String getMolecules(Parameter[] params) throws IOException
   {  String tabDataFileName = getTabDataFileName();
      String mode = null, fileName = null;
      boolean tautomerize = false;
      String tag = null;
      OEGraphMol mol = new OEGraphMol();

      for ( int i = 0; i < params.length; i++)
      {  if (params[i].getName().equals("push") || 
             params[i].getName().equals("pull")  )
         {  mode = params[i].getName();
            //fileName = params[i].getValue(i);
         }
         if (params[i].getName().equals("Comparitor"))
         {  tag = params[i].getValue(i);
         }
      }

      // @TODO what is this, doecument and implement

      if(mode.equals("push"))
      {  
      }
       else {
         System.err.println(mode + " No molecules!!!  Aborting!!!");
         System.exit(1);
      } 

      

      // copy file to file containing only smiles and ID

      BufferedReader inhibs = 
         new BufferedReader(new FileReader(tabDataFileName));
      PrintWriter out = null;

      try
      {  out = new PrintWriter(new BufferedWriter(new FileWriter("temp.out")));
         // out = new PrintWriter(new BufferedWriter(new FileWriter(getMoleculeFileName())));

         String molRec;
         while( ( molRec = inhibs.readLine() ) != null)
         {  String tokens[] = molRec.split("\t");
            out.print(tokens[0] + "\t" + tokens[1] + "\n");
         }
         out.close();
      } catch (IOException e)
      {  throw new Error(e);
      }
      return "inhibitors.tab";
   }

   public String getMoleculeFileName()
   {  return "inhibs.ism";
   }

   

   public String getTabDataFileName()
   {  return "inhibitors.tab";
   }
}

