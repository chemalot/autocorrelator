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

import java.io.*;

public class MergeTabs {

   private static final String EXPLAIN=
      "mergeTabs.csh\n" +
      "\t -in1       (tab file containing values + pivot)\n" +
      "\t -in2       (tab file containing values + pivot)\n" +
      "\t -pivot     (pivot)\n" +         
      "\t -out       (tab seperated output)\n";
   
	public static void main(String argv[]) throws IOException 
   {
      CommandLineParser cParser;
      String[] modes    = {};
      String[] parms    = {"-in1", "-in2", "-pivot", "-out", "-headers"};
      String[] reqParms = {"-in1", "-in2", "-pivot", "-out"};
      
      int pivot1 = -1, pivot2 = -1;
		String str, str2, headerLine ="";
		String[] headerS = null , headerS2 = null;
      boolean firstLine = false , firstLine2 = false, headers1 = false, 
              headerPrint = false, headers2 = false;
      cParser = new CommandLineParser(EXPLAIN,0,0,argv,modes,parms,reqParms);

			BufferedReader br = new BufferedReader(new FileReader(cParser.getValue("-in1")));
         BufferedWriter out = new BufferedWriter(new FileWriter(cParser.getValue("-out")));
			while ( (str = br.readLine()) != null )
			{  firstLine2 = false;
				if ( firstLine == false )
				{  headerS = str.split("\t"); 
               firstLine = true;
               for ( int j = 0; j < headerS.length; j++ )
               {  System.out.println(headerS[j]);
                  if (headerS[j].equals(cParser.getValue("-pivot")))
                     pivot1 = j;
               }
               System.err.println("Number of columns " + headerS.length);
               if (cParser.wasGiven("-headers") && headers1 == false)
               {  headers1 = true;
                  for ( int j = 0; j < headerS.length; j++ )
                  {  if ( j == pivot1 ) continue;
                     if ( j == 0  || headerLine.length() < 2)
                        headerLine = headerLine + headerS[j];
                     else 
                        headerLine = headerLine + "\t" + headerS[j];
                  }
               }     
               if (pivot1 == -1)
               {System.err.println("No pivot found (-in1)!!!\n"); System.exit(15);}
               continue; 
            }
            BufferedReader br2 = new BufferedReader(new FileReader(cParser.getValue("-in2")));
            while ( (str2 = br2.readLine()) != null )
            {
               if ( firstLine2 == false )
               {  headerS2 = str2.split("\t"); 
                  firstLine2 = true;
                  for ( int j = 0; j < headerS2.length; j++ )
                     if (headerS2[j].equals(cParser.getValue("-pivot")))
                        pivot2 = j;
                  //System.err.println("Number of columns " + headerS.length);
                  if (cParser.wasGiven("-headers") && headers2 == false)
                  {  headers2 = true;
                     for ( int j = 0; j < headerS2.length; j++ )
                     {  if ( headerLine.length() < 2 )
                           headerLine = headerLine + headerS[j];
                        else 
                           headerLine = headerLine + "\t" + headerS2[j];
                     }
                     if ( headerPrint == false )
                     {  headerPrint = true;
                        out.write(headerLine + "\n");
                     }                     
                  }
                  if (pivot2 == -1)
                  {System.err.println("No pivot found (-in2)!!!\n"); System.exit(25);}
                  continue; 
               }
               
               //mol = new OEGraphMol();
   				String[] tokensS = str.split("\t");
               String[] tokens2 = str2.split("\t");
               if ( pivot1 >= tokensS.length )
               {
                  System.err.println( "Error! (Pos1) " + pivot1 );
                  continue;
               }
               if ( pivot2 >= tokens2.length )
               {
                  System.err.println( "Error! (Pos2) " + pivot2 );
                  continue;
               }
               
               //System.out.println(headerLine);
               if (tokensS[pivot1].equals(tokens2[pivot2]))
               {  // Drop here when match found
                  String line = "";
                  for (int x = 0; x < tokensS.length; x++)
                  {  if (x == pivot1) continue;
                     if (x == 0)
                        line = line + tokensS[x];
                     else 
                        line = line + "\t" + tokensS[x];
                  }
                  for (int x = 0; x < tokens2.length; x++)
                  {  
                        line = line + "\t" + tokens2[x];
                  }
                  out.write(line + "\n");
               }
            }
            //mol = null;
			}
         out.close();
     
	}
}