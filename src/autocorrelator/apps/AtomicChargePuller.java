package autocorrelator.apps;

import openeye.oechem.OEAtomBase;
import openeye.oechem.OEGraphMol;
import openeye.oechem.OEMatchBaseIter;
import openeye.oechem.OEMatchPairAtomIter;
import openeye.oechem.OEMatchPairBondIter;
import openeye.oechem.OESubSearch;
import openeye.oechem.oechem;
import openeye.oechem.oemolistream;
import openeye.oechem.oemolostream;

public class AtomicChargePuller
{

   private static final String EXPLAIN=
            "atomicChargePuller.csh -in fn -out fn [-v] smarts\n" +
            "  -v..........output if no match\n" +
            "\t-in.........input file (any OE filetype),  for sdtin use .type.\n" +
            "\t-out........output file (any OE filetype), for stdout use .type.\n" +
            "\n";

      public static void main(String[] args)
      {  CommandLineParser cParser;
         String[] modes    = { };
         String[] parms    = { "-in", "-out" };
         String[] reqParms = {"-in", "-out" };

         cParser = new CommandLineParser(EXPLAIN,0,0,args,modes,parms,reqParms);

         String in = cParser.getValue("-in");
         String out = cParser.getValue("-out");
         

         args = cParser.getRestArgs();
         if( args.length != 1 )
         {  System.err.println("Exactly one smarts must be given!\n"
                              +EXPLAIN );
            System.exit(1);
         }

         oemolistream ifs = new oemolistream(in);
         oemolostream ofs = new oemolostream(out);

         int count = 0;
         OEGraphMol mol = new OEGraphMol();
         OESubSearch ss = new OESubSearch(args[0]);
         if(! ss.IsValid())
            throw new Error("Invalid Smarts " + args[0]);

         while (oechem.OEReadMolecule(ifs, mol))
         {  count++;
            double charge = 0.000;
                  for( OEMatchBaseIter matchIt = ss.Match(mol, true ); matchIt.hasNext(); )
                  {
                     for(OEMatchPairAtomIter atomMatchIt= matchIt.next().GetAtoms(); atomMatchIt.hasNext(); )
                     {
                        // Get Position 1 and break;
                        OEAtomBase at = atomMatchIt.next().getTarget();
                        charge = at.GetPartialCharge();
                        System.err.println("Atom Type: " + at.GetAtomicNum());
                     }
                  }
                  oechem.OESetSDData(mol, "AtomCharge", Double.toString(charge));
                  
               //} else {
               //   oechem.OESetSDData(mol, "AtomCharge", "");
               //}

               oechem.OEWriteMolecule(ofs, mol);
               if ( ( count % 100 ) == 0 ) System.err.println(count + " molecules examined");
         }
         
         ifs.close();
         ofs.close();

         mol.delete();
         ss.delete();
      }
   
}
