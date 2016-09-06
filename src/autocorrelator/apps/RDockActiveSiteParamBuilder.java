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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class RDockActiveSiteParamBuilder 
{
	private static final String EXPLAIN=
         "rDockActiveSiteParamBuilder.csh -method (lig|bigcirc) -mol2 (prot.mol2) -ligand (receptor)\n"
       + "\n"
       + " Will build the appropiate parameter files for rDock.\n" 
       + "\n";
	
	public static void main(String[] args) throws IOException, InterruptedException
	{
		
	    String[] modes    = { };
	    String[] parms    = { "-mol2", "-rflex", "-ligand", "-method", "-radius", "-ssphere", "-score" //"-volincr", "-gridstep", 
	    		//"-maxtrans", "-maxrot", "-maxdihedral", "-transstep", "-rotstep", "-dihstep", 
	    		};
	    String[] reqParms = { "-mol2", "-ligand", "-method", "-radius", "-ssphere", "-score" //"-volincr", "-gridstep", 
	    		//"-maxtrans", "-maxrot", "-maxdihedral", "-transstep", "-rotstep", "-dihstep", 
	    		};
	    
	    CommandLineParser cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
	    
	    String currentDir   = System.getProperty("user.dir");
	    
   
      PrintWriter prm = 
	         new PrintWriter(new BufferedWriter(new FileWriter("p.prm")));
	    

		//String ttokens[] = cParser.getValue("-mol2").split("-");
		prm.write("RBT_PARAMETER_FILE_V1.00\n");
		prm.write("TITLE B\n");
		prm.write("RECEPTOR_FILE " + cParser.getValue("-mol2") + "\n");
//		prm.write("RECEPTOR SEGMENT NAME PROT\n");
		if (cParser.wasGiven("-rflex") && !cParser.getValue("-score").contains("grid"))
			prm.write("RECEPTOR_FLEX " + roundDecimal(Double.parseDouble(cParser.getValue("-rflex"))) + "\n");
		prm.write("\nSECTION MAPPER\n");
		
		if (cParser.getValue("-method").equals("lig"))
		{
			prm.write("  SITE_MAPPER RbtLigandSiteMapper\n");
			prm.write("  REF_MOL " + cParser.getValue("-ligand") + "\n");
		} else {
			prm.write("  SITE_MAPPER RbtSphereSiteMapper\n");
//		    RWMol p = RWMol.MolFromPDBFile(cParser.getValue("-ligand"));
//			double xA = 0, yA = 0, zA = 0;
//			int numAtoms = Integer.parseInt(Long.toString(p.getNumAtoms()));
//			for (int a = 0; a < numAtoms; a++)
//			{
//				Point3D ptl1 = p.getConformer().getAtomPos((a));
//				xA = xA + ptl1.getX();
//				yA = yA + ptl1.getY();
//				zA = zA + ptl1.getZ();
//			}
//			xA = xA / (double) numAtoms;
//			yA = yA / (double) numAtoms;
//			zA = zA / (double) numAtoms;
			//prm.write("  CENTER (" + roundDecimal(xA) + ", " + roundDecimal(yA) + ", " + roundDecimal(zA) + ")\n");
		}
//		prm.write("  LARGE_SPHERE " + cParser.getValue("-lsphere") + "\n");
		prm.write("  RADIUS " + roundDecimal(Double.parseDouble(cParser.getValue("-radius"))) + "\n");
		prm.write("  SMALL_SPHERE " + roundDecimal(Double.parseDouble(cParser.getValue("-ssphere"))) + "\n");
		prm.write("  MIN_VOLUME 100\n");
		prm.write("  MAX_CAVITIES 1\n");
//		prm.write("  VOL_INCR " + roundDecimal(Double.parseDouble(cParser.getValue("-volincr"))) + "\n");
//		prm.write("  GRIDSTEP " + roundDecimal(Double.parseDouble(cParser.getValue("-gridstep"))) + "\n");
		prm.write("END_SECTION\n");
		prm.write("\n");
		prm.write("SECTION CAVITY\n");
		prm.write("  SCORING_FUNCTION RbtCavityGridSF\n");
		prm.write("  WEIGHT 1.0\n");
//		prm.write("  RMAX 0.1\n");
//		prm.write("  QUADRATIC FALSE\n");
		prm.write("END_SECTION\n");
//		prm.write("SECTION LIGAND\n");
//		prm.write("  TRANS_MODE FREE\n");
//		prm.write("  ROT_MODE FREE\n");
//		prm.write("  DIHEDRAL MODE FREE\n");
//		prm.write("  MAX_TRANS " + roundDecimal(Double.parseDouble(cParser.getValue("-maxtrans"))) + "\n");
//		prm.write("  MAX_ROT " + roundDecimal(Double.parseDouble(cParser.getValue("-maxrot"))) + "\n");
//		prm.write("  MAX_DIHEDRAL " + roundDecimal(Double.parseDouble(cParser.getValue("-maxdihedral"))) + "\n");
//		prm.write("  TRANS_STEP " + roundDecimal(Double.parseDouble(cParser.getValue("-transstep"))) + "\n");
//		prm.write("  ROT_STEP " + roundDecimal(Double.parseDouble(cParser.getValue("-rotstep"))) + "\n");
//		prm.write("  DIHEDRAL_STEP " + roundDecimal(Double.parseDouble(cParser.getValue("-dihstep"))) + "\n");
//		prm.write("END_SECTION\n");
		prm.close();
		
		
		PrintWriter dprm = 
	         new PrintWriter(new BufferedWriter(new FileWriter("dock.prm")));
		
		dprm.write("RBT_PARAMETER_FILE_V1.00\n");
		dprm.write("TITLE Free docking (indexed VDW)\n\n");

		dprm.write("SECTION SCORE\n");
		if (cParser.getValue("-score").equals("normal"))
		{
			dprm.write("	INTER	 RbtInterIdxSF.prm\n");
		} else if (cParser.getValue("-score").equals("pmf")) { 
			dprm.write("	INTER	 RbtPMFIdxSF.prm\n");
		} else if (cParser.getValue("-score").equals("solv")) {
			dprm.write("	INTER	 RbtSolvIdxSF.prm\n");
		} else if (cParser.getValue("-score").equals("gridsolv")) {
			dprm.write("	INTER	 RbtSolvGridSF.prm\n");
		} else if (cParser.getValue("-score").equals("gridnorm")) {
			dprm.write("	INTER	 RbtInterGridSF.prm\n");
		} else {
			System.err.println("No scoring function selected!  Defaulting back to normal.");
			dprm.write("	INTER	 RbtInterIdxSF.prm\n");
		}
		dprm.write("	INTRA    RbtIntraSF.prm\n");
		dprm.write("    SYSTEM   RbtTargetSF.prm\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION SETSLOPE_1\n");
		dprm.write("  TRANSFORM           		    RbtNullTransform\n");
		dprm.write("  WEIGHT@SCORE.RESTR.CAVITY	    5.0	# Dock with a high penalty for leaving the cavity\n");
		dprm.write("  WEIGHT@SCORE.INTRA.DIHEDRAL	0.1	# Gradually ramp up dihedral weight from 0.1->0.5\n");
		dprm.write("  ECUT@SCORE.INTER.VDW		    1.0	# Gradually ramp up energy cutoff for switching to quadratic\n");
		dprm.write("  USE_4_8@SCORE.INTER.VDW		TRUE	# Start docking with a 4-8 vdW potential\n");
		dprm.write("  DA1MAX@SCORE.INTER.POLAR	    180.0	# Broader angular dependence\n");
		dprm.write("  DA2MAX@SCORE.INTER.POLAR	    180.0	# Broader angular dependence\n");
		dprm.write("  DR12MAX@SCORE.INTER.POLAR	    1.5	# Broader distance range\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION RANDOM_POP\n");
		dprm.write("   TRANSFORM                    RbtRandPopTransform\n");
		dprm.write("   POP_SIZE                     50\n");
		dprm.write("   SCALE_CHROM_LENGTH		    TRUE\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION GA_SLOPE1\n");
		dprm.write("  TRANSFORM			            RbtGATransform\n");	
		dprm.write("  PCROSSOVER			        0.4	# Prob. of crossover\n");
		dprm.write("  XOVERMUT			            TRUE	# Cauchy mutation after each crossover\n");
		dprm.write("  CMUTATE				        FALSE	# True = Cauchy; False = Rectang. for regular mutations\n");
		dprm.write("  STEP_SIZE			            1.0	# Max translational mutation\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION SETSLOPE_3\n");
		dprm.write("  TRANSFORM           		    RbtNullTransform\n");
		dprm.write("  WEIGHT@SCORE.INTRA.DIHEDRAL	0.2\n");
		dprm.write("  ECUT@SCORE.INTER.VDW		    5.0\n");
		dprm.write("  DA1MAX@SCORE.INTER.POLAR	    140.0\n");
		dprm.write("  DA2MAX@SCORE.INTER.POLAR	    140.0\n");
		dprm.write("  DR12MAX@SCORE.INTER.POLAR	    1.2\n");
		dprm.write("END_SECTION\n");

		dprm.write("SECTION GA_SLOPE3\n");
		dprm.write("  TRANSFORM			            RbtGATransform\n");	
		dprm.write("  PCROSSOVER			        0.4	# Prob. of crossover\n");
		dprm.write("  XOVERMUT			            TRUE	# Cauchy mutation after each crossover\n");
		dprm.write("  CMUTATE				        FALSE	# True = Cauchy; False = Rectang. for regular mutations\n");
		dprm.write("  STEP_SIZE			            1.0	# Max torsional mutation\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION SETSLOPE_5\n");
		dprm.write("  TRANSFORM           		    RbtNullTransform\n");
		dprm.write("  WEIGHT@SCORE.INTRA.DIHEDRAL	0.3\n");
		dprm.write("  ECUT@SCORE.INTER.VDW		    25.0\n");
		dprm.write("  USE_4_8@SCORE.INTER.VDW		FALSE	# Now switch to a convential 6-12 for final GA, MC, minimisation\n");
		dprm.write("  DA1MAX@SCORE.INTER.POLAR	    120.0\n");
		dprm.write("  DA2MAX@SCORE.INTER.POLAR	    120.0\n");
		dprm.write("  DR12MAX@SCORE.INTER.POLAR	    0.9\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION GA_SLOPE5\n");
		dprm.write("  TRANSFORM			            RbtGATransform\n");	
		dprm.write("  PCROSSOVER			        0.4	# Prob. of crossover\n");
		dprm.write("  XOVERMUT			            TRUE	# Cauchy mutation after each crossover\n");
		dprm.write("  CMUTATE				        FALSE	# True = Cauchy; False = Rectang. for regular mutations\n");
		dprm.write("  STEP_SIZE			            1.0	# Max torsional mutation\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION SETSLOPE_10\n");
		dprm.write("  TRANSFORM           		    RbtNullTransform\n");
		dprm.write("  WEIGHT@SCORE.INTRA.DIHEDRAL	0.5	# Final dihedral weight matches SF file\n");
		dprm.write("  ECUT@SCORE.INTER.VDW		    120.0	# Final ECUT matches SF file\n");
		dprm.write("  DA1MAX@SCORE.INTER.POLAR	    80.0\n");
		dprm.write("  DA2MAX@SCORE.INTER.POLAR	    100.0\n");
		dprm.write("  DR12MAX@SCORE.INTER.POLAR	    0.6\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION MC_10K\n");
		dprm.write("  TRANSFORM           		    RbtSimAnnTransform\n");
		dprm.write("  START_T             	     	10.0\n");
		dprm.write("  FINAL_T             		    10.0\n");
		dprm.write("  NUM_BLOCKS          		    5\n");
		dprm.write("  STEP_SIZE          		    0.1\n");
		dprm.write("  MIN_ACC_RATE            	    0.25\n");
		dprm.write("  PARTITION_DIST          	    8.0\n");
		dprm.write("  PARTITION_FREQ          	    50\n");
		dprm.write("  HISTORY_FREQ            	    0\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION SIMPLEX\n");
		dprm.write("  TRANSFORM			            RbtSimplexTransform\n");
		dprm.write("  MAX_CALLS			            200\n");
		dprm.write("  NCYCLES				        20\n");
		dprm.write("  STOPPING_STEP_LENGTH		    10e-4\n");
		dprm.write("  PARTITION_DIST			    8.0\n");
		dprm.write("  STEP_SIZE			            1.0\n");
		dprm.write("  CONVERGENCE			        0.001\n");
		dprm.write("END_SECTION\n\n");

		dprm.write("SECTION FINAL\n");
		dprm.write("  TRANSFORM           		    RbtNullTransform\n");
		dprm.write("  WEIGHT@SCORE.RESTR.CAVITY	    1.0	# revert to standard cavity penalty\n");
		dprm.write("END_SECTION\n");
		
		dprm.close();
				
	}
	
	public static double roundDecimal(double d) {
	    DecimalFormat twoDForm = new DecimalFormat("#.#");
	    return Double.valueOf(twoDForm.format(d));
	}
	public static double floorDecimal(double d) {
	    DecimalFormat twoDForm = new DecimalFormat("#");
	    return Double.valueOf(twoDForm.format(d));
	}
}
