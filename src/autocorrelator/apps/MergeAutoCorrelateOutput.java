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
package autocorrelator.apps;

import openeye.oechem.*;
 
import java.io.*; 
import java.util.*;

//create a new molecule
public class MergeAutoCorrelateOutput {
   
   private static final String EXPLAIN=
         "MergeAutoCorrelateOutput.csh " +
         "-mols molecules to analyize,  for sdtin use .type.\n" +
         "Output is to stdout\n";
   
	public static void main(String argv[]) throws IOException, InterruptedException {
		// Generate Canonical SMILES of inhibitors
      CommandLineParser cParser;
//      boolean ActivesRead = false;
      String[] modes    = {};
      String[] parms    = {"-list"};
      String[] reqParms = {"-list"};
      String str;
      LinkedList goodList = new LinkedList(); 
      LinkedList vari = new LinkedList();
      ArrayList<OEGraphMol> mollist = new ArrayList<OEGraphMol>();
      OEGraphMol mol = new OEGraphMol();
      
      String currentDir = System.getenv("PWD");
      System.out.println(currentDir);
      
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);
      InputStreamReader isr = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isr);
      oemolistream ifs = new oemolistream(cParser.getValue("-list")); 
      
      while (oechem.OEReadMolecule(ifs,mol))
      {  OEGraphMol listMol = new OEGraphMol(mol);
         oechem.OESetSDData(listMol, "Canonical_SMILES", oechem.OECreateCanSmiString(mol));         
         mollist.add(listMol);
      }      
      
      while ( (str = br.readLine()) != null )
      {  oemolistream current = new oemolistream(str.trim()); 
         String tok = str.substring(0, str.length()-4); 
         System.out.println("New Open " + str);
         vari = mergeData(current, mollist, tok, vari);
         System.err.println(mollist.size() + " FILENAME: " + tok);
         toTab(mollist, tok, goodList, vari);    
         vari.clear();
      }      
      report(goodList);
      // Reporting       
	}

   @SuppressWarnings("unchecked")
   public static LinkedList mergeData(oemolistream ifs, ArrayList<OEGraphMol> mollist,
                                String nameS, LinkedList variables)
   {  OEGraphMol mol  = new OEGraphMol();
      int count = 0;

      while (oechem.OEReadMolecule(ifs,mol))
      {  oechem.OESetSDData(mol, 
                  "Canonical_SMILES", oechem.OECreateCanSmiString(mol));         
         String canonicalSmilesS = oechem.OEGetSDData(mol, "Canonical_SMILES");
         for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
         {  OEGraphMol molObj = iter.next();
            if (canonicalSmilesS.equals(oechem.OEGetSDData
                     (molObj, "Canonical_SMILES")))
            {  
   // Push all of the data
               for (OESDDataIter oiter = oechem.OEGetSDDataPairs(mol); 
                  oiter.hasNext(); )
               {  OESDDataPair dp = oiter.next();
                  oechem.OESetSDData(molObj,dp.GetTag(),dp.GetValue());
                  if (!variables.contains(dp.GetTag()))
                     variables.add(dp.GetTag());
               } 
                 
            }          
         }
         mol.Clear();
         count++;
      }
      return variables;
   }

   @SuppressWarnings("unchecked")
   public static void report(LinkedList goodList) 
      throws IOException, InterruptedException
   {
      BufferedWriter out = new BufferedWriter(new FileWriter("goodModels.txt"));
      for (Iterator<String> iter = goodList.iterator(); iter.hasNext();)
      {
         String tempValue = iter.next();
         out.write(tempValue + "\n");
      }
      out.close();
   }
   
   @SuppressWarnings("unchecked")
   public static void toTab(ArrayList<OEGraphMol> mollist, String name,
            LinkedList goodList, LinkedList list) 
      throws IOException, InterruptedException
   {
     String nameS = name + "_autoCorrelate.tab";
     BufferedWriter out = new BufferedWriter(new FileWriter(nameS));
     String headers[] = null; 
     int i = -1, count =0;
     boolean readHeader = false, potency = false;

      for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
      {  OEGraphMol molObj = iter.next(); count = 0;
         for (OESDDataIter oiter = oechem.OEGetSDDataPairs(molObj); 
              oiter.hasNext();)
         {  OESDDataPair dp = oiter.next();
            if ( dp.GetTag().contains("IC50"))
            {  if (!list.contains(dp.GetTag()))
                  list.addFirst(dp.GetTag());
            }
         }
      }  

     for (Iterator<String> iter = list.iterator(); iter.hasNext();)
      {
         String tempValue = iter.next();
 /*                  if(isAlpha(tempValue)) 
             { System.out.println(tempValue + "Is a letter!");   
               continue; }
             
             if (isNumeric(tempValue)) 
             { System.out.println(tempValue + "Is a number!");
               if(isAlpha(tempValue)) 
                  {  System.out.println(tempValue + "Also a letter!");
                     continue; }
             }*/             
        //     System.out.println("Storing " + tempValue + " as a legal header!");           
         System.out.print(tempValue+ "\t");               
             out.write(tempValue + "\t");
     }
     out.write("\n");
     System.out.print("\n");
/* DMZ */     
     for (Iterator<OEGraphMol> iter = mollist.iterator(); iter.hasNext();)
     { OEGraphMol mol = iter.next();
       for (Iterator<String> liter = list.iterator(); liter.hasNext();)
       {     String tempValue = liter.next();
             if(isAlpha(oechem.OEGetSDData(mol, tempValue))) 
             {// System.out.println(tempValue + "Is a letter!");   
               out.write(" " + "\t");  continue; }
 //       System.out.println("HERE ");                 
             if (isNumeric(oechem.OEGetSDData(mol, tempValue))) 
             { // System.out.println(tempValue + "Is a number!");
               if(isAlpha(oechem.OEGetSDData(mol, tempValue))) 
                  { // System.out.println(tempValue + "Also a letter!");
               out.write(" " + "\t");  continue; }
             }
//         System.out.println("HERE 2");                
           //  System.out.println("Storing Key: " + tempValue + " and Value: " 
           //                         + oechem.OEGetSDData(mol, tempValue) 
           //                      + " as a legal value!");
             out.write(oechem.OEGetSDData(mol, tempValue) + "\t");
//                     System.out.println("HERE 3");    
       }
       out.write("\n");
     } 
     out.close();        
     printRfile(list.size(), name);
     String currentDir = System.getenv("PWD");     
     writeCsh(name, "/home/ccdev/bin/R","--no-save --vanilla < " + currentDir +"/" + name +
               "_autoCorrelate.R");
     Runtime rt = Runtime.getRuntime();
     Process pt = rt.exec("./" + name + ".csh");     
     pt.waitFor();
     FileReader isr = new FileReader(name+"_correlate.out");
     BufferedReader br = new BufferedReader(isr);
     String str=null;   
     String headerS[] = null;
     Iterator<String> iter = list.iterator();
     
//     String tempValue = iter.next();
 /*                  if(isAlpha(tempValue)) 
             { System.out.println(tempValue + "Is a letter!");   
               continue; }
             
             if (isNumeric(tempValue)) 
             { System.out.println(tempValue + "Is a number!");
               if(isAlpha(tempValue)) 
                  {  System.out.println(tempValue + "Also a letter!");
                     continue; }
             }*/             
        //     System.out.println("Storing " + tempValue + " as a legal header!");           
        
     
        while ( (str = br.readLine()) != null )
        {  if ( str.length() < 4 ) continue;
           headerS = str.split(" ");
//           tempValue = iter.next();           
           if (headerS[0] != null)
              if (!headerS[1].equals("NA"))
              {
                 System.out.println(headerS[1]);
           if ( 0.3 < Double.valueOf(headerS[1]))
           {  System.out.println("Good");
              goodList.add(name + "\t" + headerS[1]);
           } else {System.out.println("Bad");}
              }
        }
     isr.close();
//     runAndWait("r.csh");     
     
   } 
   
   private static void writeCsh(String nameS, String app, String variables) 
      throws IOException, InterruptedException
   {
      String currentDir = System.getenv("PWD");
      String cshFile = nameS + ".csh";    
      BufferedWriter out = new BufferedWriter(new FileWriter(cshFile));

      out.write("#!/bin/csh -f\n");
      out.write("#$ -cwd\n");
      out.write("source /home/ccdev/.login\n");
      out.write("source /home/ccdev/.cshrc\n");
      out.write("cd " + currentDir + "\n");      
      out.write("exec " + app + " " + variables + "\n");
      out.close();
      Process pt = Runtime.getRuntime().exec("chmod 777 " + cshFile);
      pt.waitFor();
//      sleep();
   }

   private static void printRfile(int count, String nameS) 
      throws IOException, InterruptedException
   {
      // TODO Auto-generated method stub
      Process pid = null;
      Runtime rt = Runtime.getRuntime();        
      String Rfile = nameS + "_autoCorrelate.R";
      String currentDir = System.getenv("PWD");      
      BufferedWriter out = new BufferedWriter(new FileWriter(Rfile)); 
      
      out.write("data<- read.table(\"" + currentDir + "/" + nameS +
             "_autoCorrelate.tab\", header=TRUE, sep = '\t')\n");
      out.write("inhib <- data[,1]\n");
      out.write("ic50 <- log10(1/inhib)\n");
      for (int i = 1; i < count; i++)
      {  int j = i + 1;
         out.write("value" + j + " <- data[," + j + "]\n");
         out.write("v" + j + " <- (cor(ic50,value" + j 
                  + ", use = \"complete.obs\"))^2\n");
      }
      out.write("zz <- file(\"" + nameS + "_correlate.out\", open=\"wt\")\n");      
      out.write("sink(zz)\n");      
      for (int i = 1; i < count; i++)
      {  int j = i + 1;
         out.write("print(v"+j + ")\n");
      }
      out.close();

      return;
   }

   
   private static boolean isNumeric(String str)
   {
      boolean blnNumeric = false;
      boolean blnAlpha = false;
      
      char chr[] = null;
      if(str != null)
         chr = str.toCharArray();
 
      for(int i=0; i<chr.length; i++)
      {
         if(chr[i] >= '0' && chr[i] <= '9')
         {
            blnNumeric = true;
            break;
         }
      }
//      System.out.println(str + " Numeric? " + blnNumeric);
      return (blnNumeric);
   }
   private static boolean isAlpha(String str)
   {
      boolean blnAlpha = false;
   
      char chr[] = null;
      if(str != null)
         chr = str.toCharArray();
      
      for(int i=0; i<chr.length; i++)
      {
         if((chr[i] >= 'A' && chr[i] <= 'Z') || (chr[i] >= 'a' && chr[i] <= 'z'))
         {
            blnAlpha = true;
            break;
         }
      }
//      System.out.println(str + " Alpha? " + blnAlpha);      
      return (blnAlpha);
   }
}   
   
 
