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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import openeye.oechem.OEAtomBase;
import openeye.oechem.OEAtomBaseIter;
import openeye.oechem.OEBondBase;
import openeye.oechem.OEBondBaseIter;
import openeye.oechem.OEGraphMol;
import openeye.oechem.OEIsCAlpha;
import openeye.oechem.OEMatchBaseIter;
import openeye.oechem.OEMatchPairBondIter;
import openeye.oechem.OEResidue;
import openeye.oechem.OESubSearch;
import openeye.oechem.OEUnaryAtomPred;
import openeye.oechem.oechem;
import openeye.oechem.oemolistream;
import openeye.oechem.oemolostream;



/**
 * 
 */

public class KernelSplit
{
   private static final String EXPLAIN=
         "sdfTagTool -protein pname [-residues resName,resName2,...]\n"
        +"  will split the prtein input file into residues for the kernel calculation.\n"
        +"  if -residues is specified only residues with the specified names will be\n"
        +"     splited. Residue naem format: Chain_ResResNum eg. A_SER33\n"
        +"\n";
   
   private static final String SPLITSmarts = "[C;$(*N)]-[C;$(*=O)]";
   private static final String SSSmarts    = "[S]-[S]";

   public static final Set<String> AMINOAcidCodes;
   
   static
   {  String[] dummy = {  "Ala", "Arg", "Asn", "Asp", "Cys", "Gln", "Glu", "Gly", 
                          "His", "Ile", "Leu", "Lys", "Met", "Phe", "Pro", "Ser",
                          "Thr", "Trp", "Tyr", "Val" };
      AMINOAcidCodes = new HashSet<String>(dummy.length);
      for( int i=0; i<dummy.length; i++)
      {  AMINOAcidCodes.add(dummy[i].toLowerCase());
      }
   }
   public static void main(String[] args)
   throws IOException
   {  CommandLineParser cParser;
      String[] modes    = {  };
      String[] parms    = {"-protein", "-residues" };
      String[] reqParms = {"-protein"};
      Set<String> residueSet = null;
      
      cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);
      String protFileName = cParser.getValue("-protein");

      String residueListStr = cParser.getValue("-residues");
      if(residueListStr != null)
      {  String[] residueList = residueListStr.trim().split("\\s*,\\s*");
         if(residueList.length > 0)
         {  residueSet = new HashSet<String>(residueList.length);
            for(String res: residueList)
               residueSet.add(res);
         }
      }

      String baseName =protFileName.replaceAll(".[^.]*$", "");
      oemolistream ifs = new oemolistream(protFileName);
      
      OEUnaryAtomPred cAlphaTest = new OEIsCAlpha();
      OESubSearch cAlphaCPAt = new OESubSearch(SPLITSmarts);
      OESubSearch ssPat      = new OESubSearch(SSSmarts);
      
      OEGraphMol mol = new OEGraphMol();
      OEGraphMol newMol = new OEGraphMol();
      int count = 0;
      while (oechem.OEReadMolecule(ifs, mol)) 
      {  count++;
         //oechem.OEPerceiveResidues(mol, OEPreserveResInfo.ResidueNumber|OEPreserveResInfo.ChainID);
         oechem.OEDetermineConnectivity(mol);
         oechem.OEPerceiveBondOrders(mol);
         
         markIntraResidueBondsDeleted(cAlphaCPAt, mol);
         markResidueBondsDeleted(ssPat, mol);
         
         int[] oldAtomToNewMap = new int[mol.GetMaxAtomIdx()+1];
         for( OEAtomBaseIter aiter = mol.GetAtoms(cAlphaTest); aiter.hasNext();)
         {  
            OEAtomBase atom = aiter.next();
            OEResidue res = oechem.OEAtomGetResidue(atom);
            String resName = res.GetName();
            char chain = res.GetChainID();
            int resNum = res.GetResidueNumber();
            String fullResName = String.format("%c_%s%d", chain, resName, resNum);
            if(residueSet != null && ! residueSet.contains(fullResName)) continue;
            
            ArrayList<OEAtomBase> resAtoms = new ArrayList<OEAtomBase>();
            ArrayList<OEBondBase> resBonds = new ArrayList<OEBondBase>();
            getFragmentAtoms(atom, resAtoms, resBonds);

            newMol.Clear();
            // create copy of atoms
            OEAtomBase[] newAtoms = new OEAtomBase[resAtoms.size()]; 
            for(int i=0; i<resAtoms.size(); i++)
            {  OEAtomBase at = resAtoms.get(i);
               newAtoms[i] = newMol.NewAtom(at);
               oldAtomToNewMap[at.GetIdx()] = i;
            }

            // create copy of bonds
            for(int i=0; i<resBonds.size(); i++)
            {  OEBondBase b = resBonds.get(i);
               OEAtomBase at1 = b.GetBgn(); 
               OEAtomBase at2 = b.GetEnd(); 
               newMol.NewBond(newAtoms[oldAtomToNewMap[at1.GetIdx()]],
                              newAtoms[oldAtomToNewMap[at2.GetIdx()]],
                              b.GetOrder());
            }

            // add implicit H if bond was removed
            float[] xyz = new float[ 3 ] ;
            for(int i=0; i<resAtoms.size(); i++)
            {  OEAtomBase oldAt = resAtoms.get(i);
               OEAtomBase newAt = newAtoms[i];
               if(oldAt.GetDegree() > newAt.GetDegree())
               {  OEAtomBase newH = newMol.NewAtom(1);
                  newMol.GetCoords(newAt,xyz);
                  newMol.SetCoords(newH,xyz);
                  newMol.NewBond(newAt, newH, 1);
               }
                  
            }
            
            newMol.SetTitle(fullResName);
            oechem.OEAssignImplicitHydrogens(newMol);
            oechem.OEAddExplicitHydrogens(newMol);
            oechem.OESet3DHydrogenGeom(newMol);
            oemolostream ofs = new oemolostream(String.format("%s_%s.sdf", 
                                                         baseName, fullResName));
            oechem.OEWriteMolecule(ofs, newMol);
            ofs.close();
         }

      }
      ifs.close();
   }

   /**
    * Mark any bonds batching the smartsPat and beeing in the same residue as deleted
    *   (fmsDeleted).
    */
   private static void markIntraResidueBondsDeleted(OESubSearch smartsPat, OEGraphMol mol)
   {  // delete all C=O-Calpha bonds
      for( OEMatchBaseIter matchIt = smartsPat.Match(mol, true ); matchIt.hasNext(); )
      {  for(OEMatchPairBondIter bondMatchIt= matchIt.next().GetBonds(); bondMatchIt.hasNext(); )
         {  OEBondBase bond = bondMatchIt.next().getTarget();
            OEAtomBase at1 = bond.GetBgn();
            OEAtomBase at2 = bond.GetEnd();
            if( oechem.OEAtomGetResidue(at1).GetResidueNumber() == oechem.OEAtomGetResidue(at2).GetResidueNumber()
                && oechem.OEAtomGetResidue(at1).GetChainID() == oechem.OEAtomGetResidue(at2).GetChainID()
                && AMINOAcidCodes.contains(oechem.OEAtomGetResidue(at1).GetName().toLowerCase()))
            {  bond.SetBoolData("fmsDeleted", true);
            }
         }
      }
   }
   
   /**
    * Mark any residue bonds batching the smartsPat as deleted (fmsDeleted).
    */
   private static void markResidueBondsDeleted(OESubSearch smartsPat, OEGraphMol mol)
   {  // delete all C=O-Calpha bonds
      for( OEMatchBaseIter matchIt = smartsPat.Match(mol, true ); matchIt.hasNext(); )
      {  for(OEMatchPairBondIter bondMatchIt= matchIt.next().GetBonds(); bondMatchIt.hasNext(); )
         {  OEBondBase bond = bondMatchIt.next().getTarget();
            OEAtomBase at1 = bond.GetBgn();
            if( AMINOAcidCodes.contains(oechem.OEAtomGetResidue(at1).GetName().toLowerCase()))
            {  bond.SetBoolData("fmsDeleted", true);
            }
         }
      }
   }
   
   private static void getFragmentAtoms(OEAtomBase atom, 
                  ArrayList<OEAtomBase> resAtoms, ArrayList<OEBondBase> resBonds)
   {  if(atom.GetBoolData("gfaVisited")) return;
      
      resAtoms.add(atom);
      atom.SetBoolData("gfaVisited", true);
      
      for(OEBondBaseIter bIt = atom.GetBonds(); bIt.hasNext(); )
      {  OEBondBase b = bIt.next();
         if(b.GetBoolData("fmsVisited")) continue;
         if(b.GetBoolData("fmsDeleted")) continue;
         
         b.SetBoolData("fmsVisited", true);
         resBonds.add(b);

         OEAtomBase at2 = b.GetNbr(atom);
         if(! at2.GetBoolData("fmsVisited"))
            getFragmentAtoms(at2, resAtoms, resBonds);
      }
   }
}

