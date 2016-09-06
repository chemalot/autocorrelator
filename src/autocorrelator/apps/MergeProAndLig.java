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



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


//create a new molecule
public class MergeProAndLig {
   private static final String EXPLAIN=
      "mergeProAndLig.csh\n" +
      "\t-protein      input  protein (GROMACS Format)\n" +
      "\t-ligand       input  ligand  (GROMACS Format)\n" +
      "\t-out          output complex (GROMACS Format)\n" +
      "\t-waters       input  integer (Number of waters)\n" +
      "\t-protop       input  protein (GROMACS TOP Format)\n" +
      "\t-ligtop       input  ligand  (GROMACS TOP Format)\n" + 
      "\t-outtop       output ligand  (GROMACS TOP Format)\n" +
      "\t-cleanligtop  output ligand  (GROMACS TOP Format)\n" + 
      "\t-ligitp       input  ligand  (GROMACS ITP Format)\n";      

	public static void main(String argv[]) throws IOException 
   {  String[] modes    = {};
      String[] parms    = {"-protein", "-ligand", "-out","-protop",
                           "-ligtop","-outtop","-cleanligtop","-ligitp",
                           "-outLigitp", "-ff", "-fep"};
      String[] reqParms = {"-protein", "-ligand", "-out","-protop",
                           "-ligtop","-outtop","-cleanligtop","-ligitp",
                           "-outLigitp", "-ff"};
      String[] lineTokens, residueInfo = null;
      String proS, ligS;
      boolean ligInclude = false;
      String currentDir = System.getenv("PWD");
      
      CommandLineParser cParser = 
           new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);      

      int protInfo[] = mergeGroFiles(cParser);
      mergeAndCleanTopFiles(ligInclude, cParser, protInfo);
      createLigItp(cParser);
       
      return;
   }

   private static void createLigItp(CommandLineParser cParser) 
   throws IOException
   {  String ligS = null, lineTokens[] =null;
      // TODO Auto-generated method stub
       BufferedReader ligtop = 
         new BufferedReader(new FileReader(cParser.getValue("-ligtop")));  
      PrintWriter alteredTop = 
         new PrintWriter(new BufferedWriter(new FileWriter(cParser.getValue("-outLigitp"))));       
      while( ( ligS = ligtop.readLine() ) != null)      
      {
         if ( ligS.contains("[ atoms ]"))
         {  alteredTop.println(ligS);
            ligS = ligtop.readLine(); // Patch 
            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();            
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               System.out.println(ligS);
               lineTokens = ligS.split(" +");
               for (int i = 0; i < lineTokens.length; i++)
                  System.out.println(i + " "+ lineTokens[i]);
//               System.out.println(protInfo[0] + " " + protInfo[1]);
               int atomNum = Integer.parseInt(lineTokens[1].trim());//+protInfo[1];
               int resNum  =  Integer.parseInt(lineTokens[3].trim());//+protInfo[0];
               System.out.println(atomNum);
               System.out.println(resNum);
               if ( lineTokens[4].length() > 1 )
               {  String s = null;
                  if (cParser.wasGiven("-fep"))
                  {  s = String.format("%6d%11s%7d%7s%7s%7d%11.5f%11.5f",
                           atomNum,
                           lineTokens[2].trim(),
                           resNum,
                           lineTokens[4].trim(),lineTokens[5].trim(),
                           atomNum,
                           Float.parseFloat(lineTokens[7].trim()),
                           Float.parseFloat(lineTokens[8].trim()));
                  s = s + "    du_" + lineTokens[2].trim() +  "    0.000    " +  Float.parseFloat(lineTokens[8].trim()) + "\n";
                  } else {
                     s = String.format("%6d%11s%7d%7s%7s%7d%11.5f%11.5f\n",
                        atomNum,
                        lineTokens[2].trim(),
                        resNum,
                        lineTokens[4].trim(),lineTokens[5].trim(),
                        atomNum,
                        Float.parseFloat(lineTokens[7].trim()),
                        Float.parseFloat(lineTokens[8].trim()));
                  }
                  alteredTop.print(s);                              
               } else {
                  String s = null;
                  if (cParser.wasGiven("-fep"))
                  {  s = String.format("%6d%11s%7d%7s%7s%7d%11.5f%11.5f",
                           atomNum,
                           lineTokens[2].trim(),
                           resNum,
                           lineTokens[4].trim(),lineTokens[5].trim(),
                           atomNum,
                           Float.parseFloat(lineTokens[7].trim()),
                           Float.parseFloat(lineTokens[8].trim()));
                     s = s + "    du_" + lineTokens[2].trim() +  "    0.000    " +  Float.parseFloat(lineTokens[8].trim()) + "\n";
                  } else {
                     s = String.format("%6d%11s%7d%7s%7s%7d%11.5f%11.5f\n",
                        atomNum,
                        lineTokens[2].trim(),
                        resNum,
                        lineTokens[5].trim(),lineTokens[6].trim(),
                        atomNum,
                        Float.parseFloat(lineTokens[8].trim()),
                        Float.parseFloat(lineTokens[9].trim()));
                  }
                  alteredTop.print(s);                              
               }
            }
            alteredTop.println("");                        
            continue;
         }
         if ( ligS.contains("[ bonds ]"))
         {  alteredTop.println(ligS);
            ligS = ligtop.readLine(); // Patch
            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();      
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               lineTokens = ligS.split(" +");
               int atomNum1 = Integer.parseInt(lineTokens[1].trim());//+protInfo[1];
               int atomNum2 = Integer.parseInt(lineTokens[2].trim());//+protInfo[1];
               if (atomNum1 == atomNum2) continue;
               String s = String.format("%5d%6d%6s%12.5f%12.0f.\n",
                        atomNum1,
                        atomNum2,
                        lineTokens[3].trim(),
                        Float.parseFloat(lineTokens[4].trim()),
                        Float.parseFloat(lineTokens[5].trim()));
               alteredTop.print(s);               
            }
            alteredTop.println("");                        
            continue;
         }         
         if ( ligS.contains("[ pairs ]"))
         {  alteredTop.println(ligS);
            ligS = ligtop.readLine(); // Patch
            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();      
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               lineTokens = ligS.split(" +");
               int atomNum1 = Integer.parseInt(lineTokens[1].trim());//+protInfo[1];
               int atomNum2 = Integer.parseInt(lineTokens[2].trim());//+protInfo[1];
               if (atomNum1 == atomNum2) continue;                          
               String s = String.format("%8d%8d  1\n",
                        Integer.parseInt(lineTokens[1].trim()),//+protInfo[1],
                        Integer.parseInt(lineTokens[2].trim()));//+protInfo[1]);
               alteredTop.print(s);               
            }
            alteredTop.println("");                        
            continue;
         }         
         if ( ligS.contains("[ angles ]"))
         {  alteredTop.println(ligS);
            ligS = ligtop.readLine(); // Patch
            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();      
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               lineTokens = ligS.split(" +");
               int atomNum1 = Integer.parseInt(lineTokens[1].trim());//+protInfo[1];
               int atomNum2 = Integer.parseInt(lineTokens[2].trim());//+protInfo[1];
               int atomNum3 = Integer.parseInt(lineTokens[3].trim());//+protInfo[1];               
               if (atomNum1 == atomNum2 || atomNum1 == atomNum3 || atomNum2 == atomNum3 ) continue;                                         
               String s = String.format("%5d%6d%6d%6s%12.3f%12.4f\n",
                        Integer.parseInt(lineTokens[1].trim())/*+protInfo[1]*/,
                        Integer.parseInt(lineTokens[2].trim())/*+protInfo[1]*/,
                        Integer.parseInt(lineTokens[3].trim())/*+protInfo[1]*/,
                        lineTokens[4].trim(),
                        Float.parseFloat(lineTokens[5].trim()),
                        Float.parseFloat(lineTokens[6].trim()));
               alteredTop.print(s);               
            }
            alteredTop.println("");            
            continue;
         }         
         if ( ligS.contains("[ dihedrals ]"))
         {  alteredTop.println(ligS);
            System.out.println(ligS);
            ligS = ligtop.readLine(); // Patch
            System.out.println(ligS);
            alteredTop.println(ligS);
            System.out.println(ligS);
            ligS = ligtop.readLine();      
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
                lineTokens = ligS.split(" +");
                int atomNum1 = Integer.parseInt(lineTokens[1].trim());//+protInfo[1];
                int atomNum2 = Integer.parseInt(lineTokens[2].trim());//+protInfo[1];
                int atomNum3 = Integer.parseInt(lineTokens[3].trim());//+protInfo[1];               
                int atomNum4 = Integer.parseInt(lineTokens[4].trim());//+protInfo[1];                                    
                if (atomNum1 == atomNum2 || atomNum1 == atomNum3 
                       || atomNum2 == atomNum3 || atomNum4 == atomNum1 ||
                       atomNum4 == atomNum2 || atomNum4 == atomNum3) continue;                       
                String s = String.format("%5d%6d%6d%6d%6s%12.3f%12.3f%10d  ;\n",
                           Integer.parseInt(lineTokens[1].trim()),//+protInfo[1],
                           Integer.parseInt(lineTokens[2].trim()),//+protInfo[1],
                           Integer.parseInt(lineTokens[3].trim()),//+protInfo[1],
                           Integer.parseInt(lineTokens[4].trim()),//+protInfo[1],
                           "1",
                           Float.parseFloat(lineTokens[6].trim()),
                           Float.parseFloat(lineTokens[7].trim()),
                           Integer.parseInt(lineTokens[8].trim()));
                alteredTop.print(s);               
                }
            alteredTop.println("");
            continue;
         }
         if ( ligS.contains("[ system ]") || ligS.contains("[ molecules ]") )
         {
            ligS = ligtop.readLine(); // Patch
            ligS = ligtop.readLine(); // Patch
            continue;
         }
      alteredTop.println(ligS);
   }

         alteredTop.close();
      }      


   private static void mergeAndCleanTopFiles(boolean ligInclude, 
            CommandLineParser cParser, int protInfo[]) 
      throws FileNotFoundException, IOException
   {
      String[] lineTokens;
      boolean ligandDihedrals = false;
      String proS = null;
      String ligS = null;

      boolean moleculesReached = false;
      BufferedReader protop = 
         new BufferedReader(new FileReader(cParser.getValue("-protop")));
      BufferedReader ligtop = 
         new BufferedReader(new FileReader(cParser.getValue("-ligtop")));  
      PrintWriter alteredTop = 
         new PrintWriter(new BufferedWriter(new FileWriter(cParser.getValue("-outtop"))));      
      PrintWriter cleanLigTop = 
         new PrintWriter(new BufferedWriter(new FileWriter(cParser.getValue("-cleanligtop"))));      

      while( ( proS = protop.readLine() ) != null)
      {
         if ( proS.length() == 0 && ligInclude == false )
         {
            alteredTop.print("; Include ligand parameters\n");
            alteredTop.print("#include \"" + cParser.getValue("-ligitp") +"\"\n");
            alteredTop.print("; Include ligand topology\n");
            alteredTop.print("#include \"" + cParser.getValue("-outLigitp") +"\"\n");
            
            ligInclude = true;
            continue;
         }
         if ( proS.contains("tip3p") )
         {  if (cParser.getValue("-ff").contains("amber"))
            {
              String line = "#include \"ffamber_tip3p.itp\"\n";
              alteredTop.print(line);
              continue;
            } else {
              String line = "#include \"spc.itp\"\n";
              alteredTop.print(line);
              continue; 
            }
         }
         if ( proS.contains("[ molecules ]") )
         {  alteredTop.println(proS);
            moleculesReached = true;
            continue;
         }
         if ( proS.contains("Protein_C"))
         {  alteredTop.println(proS);
            String line = "molecule       1\n";
            System.out.println(line);
            alteredTop.print(line);
            moleculesReached = false;
            continue;
         }
         if ( proS.contains("SOL"))
         {  alteredTop.println(proS);
            String line = "molecule       1\n";
            System.out.println(line);
            alteredTop.print(line);
            moleculesReached = false;
            continue;
         }
         // Merge Protein and Ligand info
         if ( proS.contains("[ atoms ]"))
         {  alteredTop.println(proS);
            proS = protop.readLine(); // Patch
            // Print out Protein Info
            for (; proS.length() > 0 ; proS = protop.readLine())
            {
               alteredTop.println(proS);
            }
 /*           if (Integer.parseInt(cParser.getValue("-waters")) > 0)
            {
               while ( ( watS = watertop.readLine() ) != null )
                  if (watS.contains("[ atoms ]"))
                     break;
               for (; watS.length() > 0 ; watS = watertop.readLine())
               {
                  System.out.println(watS);
                  alteredTop.println(watS);
               }

            }*/
            // Fast Forward to the Ligand Info
            while ( ( ligS = ligtop.readLine() ) != null )
               if (ligS.contains("[ atoms ]"))
                  break;
            // Print out Ligand Info            
            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();
            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();            
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               System.out.println(ligS);
               lineTokens = ligS.split(" +");
               for (int i = 0; i < lineTokens.length; i++)
                  System.out.println(i + " "+ lineTokens[i]);
               System.out.println(protInfo[0] + " " + protInfo[1]);
               int atomNum = Integer.parseInt(lineTokens[1].trim())+protInfo[1];
               int resNum  =  Integer.parseInt(lineTokens[3].trim())+protInfo[0];
               System.out.println(atomNum);
               System.out.println(resNum);                 
               if ( lineTokens[4].length() > 1 )
               {                
                  String s = String.format("%6d%11s%7d%7s%7s%7d%11.5f%11.5f\n",
                        atomNum,
                        lineTokens[2].trim(),
                        resNum,
                        lineTokens[4].trim(),lineTokens[5].trim(),
                        atomNum,
                        Float.parseFloat(lineTokens[7].trim()),
                        Float.parseFloat(lineTokens[8].trim()));
                  alteredTop.print(s);                              
               } else {
                  String s = String.format("%6d%11s%7d%7s%7s%7d%11.5f%11.5f\n",
                        atomNum,
                        lineTokens[2].trim(),
                        resNum,
                        lineTokens[5].trim(),lineTokens[6].trim(),
                        atomNum,
                        Float.parseFloat(lineTokens[8].trim()),
                        Float.parseFloat(lineTokens[9].trim()));                  
                  alteredTop.print(s);                              
               }
            }
            alteredTop.println("");                        
            continue;
         }
         if ( proS.contains("[ bonds ]"))
         {  //.println("");
         
            // Print out Protein Info
            for (; proS.length() > 0 ; proS = protop.readLine())
            {
               alteredTop.println(proS);
            }
            // Fast Forward to the Ligand Info
            while ( ( ligS = ligtop.readLine() ) != null )
               if (ligS.contains("[ bonds ]"))
                  break;
            // Print out Ligand Info            
//            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();
//            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();            
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               lineTokens = ligS.split(" +");
               if (lineTokens.length < 2) continue;
               int atomNum1 = Integer.parseInt(lineTokens[1].trim())+protInfo[1];
               int atomNum2 = Integer.parseInt(lineTokens[2].trim())+protInfo[1];
               if (atomNum1 == atomNum2) continue;
               String s = String.format("%5d%6d%6s%12.5f%12.0f.\n",
                        atomNum1,
                        atomNum2,
                        lineTokens[3].trim(),
                        Float.parseFloat(lineTokens[4].trim()),
                        Float.parseFloat(lineTokens[5].trim()));
               alteredTop.print(s);               
            }
            alteredTop.println("");                        
            continue;
         }         
         if ( proS.contains("[ pairs ]"))
         {  //alteredTop.println(proS);
         
            // Print out Protein Info
            for (; proS.length() > 0 ; proS = protop.readLine())
            {
               alteredTop.println(proS);
            }
            // Fast Forward to the Ligand Info
            while ( ( ligS = ligtop.readLine() ) != null )
               if (ligS.contains("[ pairs ]"))
                  break;
            // Print out Ligand Info            
//            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();
//            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();  
           
            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               lineTokens = ligS.split(" +");
               int atomNum1 = Integer.parseInt(lineTokens[1].trim())+protInfo[1];
               int atomNum2 = Integer.parseInt(lineTokens[2].trim())+protInfo[1];
               if (atomNum1 == atomNum2) continue;                          
               String s = String.format("%8d%8d  1\n",
                        Integer.parseInt(lineTokens[1].trim())+protInfo[1],
                        Integer.parseInt(lineTokens[2].trim())+protInfo[1]);
               alteredTop.print(s);               
            }
            alteredTop.println("");                        
            continue;
         }         
         if ( proS.contains("[ angles ]"))
         {  //.println(proS);
         
            // Print out Protein Info
            for (; proS.length() > 0 ; proS = protop.readLine())
            {
               alteredTop.println(proS);
            }
            // Fast Forward to the Ligand Info
            while ( ( ligS = ligtop.readLine() ) != null )
               if (ligS.contains("[ angles ]"))
                  break;
            // Print out Ligand Info            
//            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();
//            alteredTop.println(ligS);                          
            ligS = ligtop.readLine();            

            for (; ligS.length() > 0 ; ligS = ligtop.readLine())
            {
               lineTokens = ligS.split(" +");
               int atomNum1 = Integer.parseInt(lineTokens[1].trim())+protInfo[1];
               int atomNum2 = Integer.parseInt(lineTokens[2].trim())+protInfo[1];
               int atomNum3 = Integer.parseInt(lineTokens[3].trim())+protInfo[1];               
               if (atomNum1 == atomNum2 || atomNum1 == atomNum3 || atomNum2 == atomNum3 ) continue;                                         
               String s = String.format("%5d%6d%6d%6s%12.3f%12.4f\n",
                        Integer.parseInt(lineTokens[1].trim())+protInfo[1],
                        Integer.parseInt(lineTokens[2].trim())+protInfo[1],
                        Integer.parseInt(lineTokens[3].trim())+protInfo[1],
                        lineTokens[4].trim(),
                        Float.parseFloat(lineTokens[5].trim()),
                        Float.parseFloat(lineTokens[6].trim()));
               alteredTop.print(s);               
            }
            alteredTop.println("");            
            continue;
         }         
         if ( proS.contains("[ dihedrals ]"))
         {  if ( ligandDihedrals == false )
            {  ligandDihedrals = true;
               // Fast Forward to the Ligand Info
               while ( ( ligS = ligtop.readLine() ) != null )
                  if (ligS.contains("[ dihedrals ]"))
                     break;
               // Print out Ligand Info            
               alteredTop.println(ligS);                          
               ligS = ligtop.readLine();
               alteredTop.println(ligS);                          
               ligS = ligtop.readLine();            
   
                  for (; ligS.length() > 0 ; ligS = ligtop.readLine())
                  {
                     lineTokens = ligS.split(" +");
                     int atomNum1 = Integer.parseInt(lineTokens[1].trim())+protInfo[1];
                     int atomNum2 = Integer.parseInt(lineTokens[2].trim())+protInfo[1];
                     int atomNum3 = Integer.parseInt(lineTokens[3].trim())+protInfo[1];               
                     int atomNum4 = Integer.parseInt(lineTokens[4].trim())+protInfo[1];                                    
                     if (atomNum1 == atomNum2 || atomNum1 == atomNum3 
                           || atomNum2 == atomNum3 || atomNum4 == atomNum1 ||
                           atomNum4 == atomNum2 || atomNum4 == atomNum3) continue;                       
                     String s = String.format("%5d%6d%6d%6d%6s%12.3f%12.3f%10d  ;\n",
                              Integer.parseInt(lineTokens[1].trim())+protInfo[1],
                              Integer.parseInt(lineTokens[2].trim())+protInfo[1],
                              Integer.parseInt(lineTokens[3].trim())+protInfo[1],
                              Integer.parseInt(lineTokens[4].trim())+protInfo[1],
                              "1",
                              Float.parseFloat(lineTokens[6].trim()),
                              Float.parseFloat(lineTokens[7].trim()),
                              Integer.parseInt(lineTokens[8].trim()));
                     alteredTop.print(s);               
                  }
                  alteredTop.println("");
            }
         
            // Print out Protein Info
            for (; proS.length() > 0 ; proS = protop.readLine())
            {
               alteredTop.println(proS);
            }
            alteredTop.println("");            
            continue;
         }
         alteredTop.println(proS);
      }
      alteredTop.close();
   }

   private static int[] mergeGroFiles(CommandLineParser cParser) 
      throws FileNotFoundException, IOException
   {
      String[] lineTokens;
      String[] residueInfo = null;
      String proS =null;
      String ligS;
      int protInfo[] = {0,0};
      int totalLigAtoms, count = 0, lineCount = 0;
      int totalAtoms, totalProtAtoms = 0;
      BufferedReader protein = 
         new BufferedReader(new FileReader(cParser.getValue("-protein")));
      BufferedReader ligand = 
         new BufferedReader(new FileReader(cParser.getValue("-ligand")));  
      PrintWriter mergedFile = 
         new PrintWriter(new BufferedWriter(new FileWriter(cParser.getValue("-out"))));      
      ligS = ligand.readLine();
      ligS = ligand.readLine(); // Get Num of Ligand atoms
      mergedFile.println(proS);
      proS = protein.readLine();
      lineCount++;
      while( ( proS = protein.readLine() ) != null)
      {  lineCount++;
         if ( lineCount == 2 ) 
         {  ligS = ligS.trim();
            proS = proS.trim();
            System.out.println(proS);
            System.out.println(ligS);            
            totalProtAtoms = Integer.parseInt(proS);   
            totalLigAtoms  = Integer.parseInt(ligS);
            totalAtoms = totalProtAtoms + totalLigAtoms;
            String atomLine = String.format("%5d\n", totalAtoms);
            mergedFile.print(atomLine);
            continue;
         } 
         String[] tokenize = proS.split(" +");
         System.out.println(tokenize.length);
         if ( tokenize.length != 4 )
         {  count++;
//            System.out.println(residueInfo[0]);
//            System.out.println(residueInfo[1]);
//            System.out.println(residueInfo[2]);
//            System.out.println(residueInfo[3]);
            lineTokens = proS.split(" +");
            lineTokens[1] = lineTokens[1].trim();
            System.out.println(lineTokens[1]);
            residueInfo = lineTokens[1].split("[A-Z]");
            System.out.println(residueInfo[0]);
         }
         
         if ( tokenize.length == 4 )
         {  count--;
            while( ( ligS = ligand.readLine() ) != null)
            {  count++;
               lineTokens = ligS.split(" +");
               int resNum = Integer.parseInt(residueInfo[0]) + 1;
               protInfo[0] = resNum - 1;
               lineTokens[1] = Integer.toString(resNum);
               lineTokens[1] = lineTokens[1].concat("LIG");
               String ligLine = String.format("%8s%7s%5d%8.3f%8.3f%8.3f\n",
                        lineTokens[1],lineTokens[2].trim(), count+1,
                        Float.parseFloat(lineTokens[4].trim()),Float.parseFloat(lineTokens[5].trim()),
                        Float.parseFloat(lineTokens[6].trim()));
               System.out.print(ligLine);
               mergedFile.print(ligLine);
            }
         }
         mergedFile.println(proS);    
      }
      mergedFile.close();
      protInfo[1] = totalProtAtoms;
      return protInfo;
   }
}