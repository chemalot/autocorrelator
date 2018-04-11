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

import groovy.lang.*;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import openeye.oechem.*;

import org.apache.commons.cli.*;
import org.apache.commons.cli.CommandLineParser;
import org.codehaus.groovy.control.CompilationFailedException;

public class SDFGroovy
{
   /*
    * SDFGroovy -in fName.sdf -out fNam.out -c 'groovy'
    *
    * Will read the sd file and create a tab separated file with columns for
    * each tag in the sd file. The sd file is read twice, first to find the tags.
    *  Then to create the html.
    */
   private static final String INTROText =
       "SDFGroovy -in fName.sdf -out fNam.out -c 'groovy'|-f groovyFile\n"
      +     "   [-falseOut fName]\n"
      +     "   [-exception printToTrue|printToFalse|dropRecord|stop]\n"
      +     "   args\n"
      +"Will execute the groovy commands for each record of the input file.\n"
      +"Any additional arguments are passed by calling 'static void init(args...)'\n"
      +"    if the init() function exists.\n"
      +"If a help() function exists.\n"
      +"SD fields are passed in and can be referenced with:\n"
      +"$tagName or $<tagName for varables passed into groovy.\n"
      +    "\tEmpty and non-existent tags result in an empty string.\n"
      +"$>tagName for output variables written back to the sd file\n"
      +"$<>tagName for in/out variables\n"
      +"$TITLE can be used to access the molFile title\n"
      +"$mol can be used to access the OEMolBase of the molFile\n"
      +"For reading tags with special characters use tVal($mol,'tagName')\n"
      +"For writing tags with special characters use setVal($mol,'tagName', val)\n"
      +"For computing averages the special avg(String, ...) can be used.\n"
      +"If the script returns false the record will not be printed to the out file.\n"
      +"\n";

   static enum EXCEPTIONHandling
   {  printToTrue("printToTrue"),
      printToFalse("printToFalse"),
      dropRecord("dropRecord"),
      stop("stop");

      private final String name;

      EXCEPTIONHandling(String name)
      {  this.name = name;
      }

      public String getName()
      {  return name;
      }

      public static EXCEPTIONHandling getByName(String name)
      {  EXCEPTIONHandling eh = ehMap.get(name);
         if( eh == null ) throw new Error("Unknown Exception handling: " + name);

         return eh;
      }

      private static final Map<String, EXCEPTIONHandling> ehMap =
                                       new HashMap<String, EXCEPTIONHandling>();
      static
      {  EXCEPTIONHandling[] types = EXCEPTIONHandling.values();
         for(int i=0; i<types.length; i++)
            ehMap.put(types[i].getName(), types[i]);
      }
   }

   /**Valid format for variable names.*/
   public static final String VARNAME_FORM  = "[a-zA-Z_][a-zA-Z_0-9]*";

   public static final String TASK_VAR_FLAG = "\\$(>|<>)?";

   /**Valid format for variables in task instructions.*/
   public static final String TASK_VAR      = TASK_VAR_FLAG + VARNAME_FORM;

   /**Valid format specific for input variables in task instructions.*/
   public static final String TASK_INVAR    = "\\$" + VARNAME_FORM;

   /**Valid format specific for output variables in task instructions.*/
   public static final String TASK_OUTVAR   = "\\$>" + VARNAME_FORM;

   /**Valid format specific for in/output variables in task instructions.*/
   public static final String TASK_INOUTVAR = "\\$<>" + VARNAME_FORM;

   public static final Pattern VARIABLEPat    = Pattern.compile( TASK_VAR );
   public static final Pattern INVariablePat  = Pattern.compile( TASK_INVAR );
   public static final Pattern OUTVariablePat = Pattern.compile( TASK_OUTVAR );



   public static void main(String [] args) throws Exception {
      long start = System.currentTimeMillis();

      // create command line Options object
      Options options = new Options();
      Option opt = new Option("in",true, "input sd file");
      options.addOption(opt);

      opt = new Option("out",true, "output file for return value true or null");
      options.addOption(opt);

      opt = new Option("falseOut",true, "output file for return value false");
      options.addOption(opt);

      opt = new Option("c",true, "groovy script line");
      options.addOption(opt);

      opt = new Option("f",true, "groovy script file");
      options.addOption(opt);

      opt = new Option("exception", true, "exception handling (Default: stop)");
      options.addOption(opt);

      opt = new Option("h", false, "print help message");
      options.addOption(opt);

      CommandLineParser parser = new BasicParser();
      CommandLine cmd = null;
      try {
         cmd = parser.parse( options, args);
      } catch(Exception e) {
         exitWithHelp(e.getMessage(), options);
      }
      if( !cmd.hasOption("c") && !cmd.hasOption("f"))
         exitWithHelp("-c or -f must be given!", options);

      if( cmd.hasOption("c") && cmd.hasOption("f"))
         exitWithHelp("Only one of -c or -f may be given!", options);

      String groovyStrg;
      if( cmd.hasOption("c"))
         groovyStrg = cmd.getOptionValue("c");
      else
         groovyStrg = fileToString(cmd.getOptionValue("f"));

      Set<String> inFileds  = new HashSet<String>();
      Set<String> outFields = new HashSet<String>();
      Script script = getGroovyScript(groovyStrg, inFileds, outFields);

      if( cmd.hasOption("h"))
      {  callScriptHelp(script);
         exitWithHelp("", options);
      }

      if( ! cmd.hasOption("in") || ! cmd.hasOption("out") )
      {  callScriptHelp(script);
         exitWithHelp("-in and -out must be given", options);
      }

      String[] scriptArgs = cmd.getArgs();
      String inFile = cmd.getOptionValue("in");
      String outFile = cmd.getOptionValue("out");
      String falseOutFile = cmd.getOptionValue("falseOut");
      EXCEPTIONHandling eHandling = EXCEPTIONHandling.stop;
      if( cmd.hasOption("exception"))
         eHandling = EXCEPTIONHandling.getByName(cmd.getOptionValue("exception"));


      callScriptInit(script, scriptArgs);

      oemolistream ifs = new oemolistream(inFile);
      oemolostream ofs = new oemolostream(outFile);
      oemolostream falseOFS = null;
      if( falseOutFile != null ) falseOFS = new oemolostream(falseOutFile);

      OEMolBase mol = new OEGraphMol();

      int iCounter = 0;
      int oCounter = 0;
      int foCounter = 0;
      while(oechem.OEReadMolecule(ifs , mol) )
      {  iCounter++;

         Binding binding = getFieldBindings(mol, inFileds, outFields);

         script.setBinding(binding);
         boolean printToTrue = true;
         boolean printToFalse = true;
         try
         {  Object ret = script.run();
            if( ret == null || ! (ret instanceof Boolean) )
            {  printToFalse = false;
            } else if( ((Boolean)ret).booleanValue() )
            {  printToFalse = false;
            } else   // ret = false
            {  printToTrue = false;
            }

            setOutputFields(mol, binding, outFields);
         } catch(Exception e)
         {  switch( eHandling )
            {  case stop :
                  throw e;
               case printToTrue :
                  printToFalse = false;
                  System.err.println(e.getMessage());
               break;
               case printToFalse :
                  printToTrue = false;
                  System.err.println(e.getMessage());
               break;
               case dropRecord :
                  printToTrue = false;
                  printToFalse = false;
                  System.err.println(e.getMessage());
               break;
               default:
                  assert false;
               break;
            }
         }

         if( printToTrue )
         {  oechem.OEWriteMolecule(ofs, mol);
            oCounter++;
         }
         if( falseOFS != null && printToFalse )
         {  oechem.OEWriteMolecule(falseOFS, mol);
            foCounter++;
         }
      }

      System.err.printf("SDFGroovy: Input %d, output %d,%d structures in %dsec\n",
               iCounter, oCounter, foCounter, (System.currentTimeMillis()-start)/1000);

      if( falseOFS != null ) falseOFS.close();
      ofs.close();
      ifs.close();
      mol.delete();
   }


   private static void setOutputFields(OEMolBase mol, Binding binding,
                                       Set<String> outFields)
   {  Iterator<String> it = outFields.iterator();
      while( it.hasNext() )
      {  String tag= it.next();
         Object d = binding.getVariable( tag );
         if( "mol".equals(tag))
         {   // ignore as OEMolBase can be changed inplace
         }else if( "TITLE".equals(tag) )
         {  if( d == null ) d = "";
               mol.SetTitle(d.toString());
         } else
         {  if( d == null ) d = "";

            oechem.OEDeleteSDData(mol, tag);
            oechem.OESetSDData(mol, tag, d.toString());
         }
      }
   }


   /**
    * Retrieve tag values from sd fields in mol and set them in the groovy binding
    *   so that they can be accessed as variables inside the groovy script.
    * @param args array of command line arguments to be made available to groovy.
    * @param mol molecule object with sd tags.
    * @param inFileds   Set with input variables = tagNames
    * @param outFields  Set with output variable = tagNames
    */
   private static Binding getFieldBindings(OEMolBase mol,
                                    Set<String> inFileds, Set<String> outFields)
   {  Binding binding = new Binding();

      // set input variables
      Iterator<String> it = inFileds.iterator();
      while( it.hasNext() )
      {  String tag= it.next();
         if( "mol".equals(tag) )
            binding.setVariable( tag, mol);
         else if( "TITLE".equals(tag))
            binding.setVariable( tag, mol.GetTitle());
         else
            binding.setVariable( tag, oechem.OEGetSDData(mol, tag) );
      }

      // set output variables to null
      it = outFields.iterator();
      while( it.hasNext() )
      {  String tag= it.next();
         if( inFileds.contains( tag ) )
            continue;   // already in inlist

         binding.setVariable( tag, null );
      }
      return binding;
   }


   /**
    * Parse groovyStrng for input and output variables marked by $&lt;&gt;
    *    and create {@link Script} object.
    * @param groovyStrg string containing groovy script
    * @param initArgs arguments to be passed to init() method if exists
    * @param outFields Set to be filled with output variable names
    * @return pre compiled groovy script
    */
   private static Script getGroovyScript(String groovyStrg,
            Set<String> inFields, Set<String> outFields)
   {  StringBuffer sb  = new StringBuffer( groovyStrg.length() );
      Matcher varMatch = VARIABLEPat.matcher( groovyStrg );

      // Identify in and out variables in script by looking for
      // $(>|<>)?[a-zA-Z_][a-zA-Z_0-9]*
      while( varMatch.find() )
      {  String var = varMatch.group();

         /*Remove the prefix $(>|<>)? to get correct variable names.*/
         String varName = var.replaceAll( TASK_VAR_FLAG, "" );

         if( INVariablePat.matcher( var ).find() )   //input
            inFields.add( varName );
         else if( OUTVariablePat.matcher( var ).find() )   //output
            outFields.add( varName );
         else   //input & output
         {  inFields.add( varName );
            outFields.add( varName );
         }
         varMatch.appendReplacement( sb, varName );
      }
      varMatch.appendTail( sb );

      groovyStrg = "import static autocorrelator.apps.SDFGroovyHelper.*;import com.aestel.utility.DataFormat;\n "
                  + sb.toString();
      GroovyShell shell = new GroovyShell();
      Script script;
      try
      {  script = shell.parse(groovyStrg);
      }catch( CompilationFailedException e )
      {  System.err.println(groovyStrg);
         throw e;
      }
      return script;
   }


   private static void callScriptInit(Script script, String[] initArgs)
   {  MetaMethod m = script.getMetaClass().getMetaMethod("init", new String[]{});
      if( m != null )
         script.invokeMethod("init", initArgs);
   }

   private static void callScriptHelp(Script script)
   {  MetaMethod m = script.getMetaClass().getMetaMethod("help", null);
      if( m != null )
      {  script.invokeMethod("help", null);
         System.exit(1);
      }
   }


   private static void exitWithHelp(String msg, Options options)
   {  System.err.println(msg);
      HelpFormatter formatter = new HelpFormatter();
      formatter.setWidth(120);
      formatter.printHelp( INTROText, options );

      StringBuilder sb = new StringBuilder();
      Class<SDFGroovyHelper> helper = SDFGroovyHelper.class;
      for(Method m : helper.getMethods())
      {  if( (m.getModifiers() & Modifier.STATIC) == 0 || (m.getModifiers() & Modifier.PUBLIC) == 0 )
            continue;

         String meth = m.toString();
         meth = meth.replaceAll("public static final ", "");
         meth = meth.replaceAll("java\\.lang\\.", "");
         meth = meth.replaceAll("java\\.math\\.", "");
         meth = meth.replaceAll("autocorrelator\\.apps\\.SDFGroovyHelper.", "");
         sb.append('\t').append(meth).append('\n');
      }
      System.out.println("Imported internal functions:");
      System.out.println(sb);
      System.exit(1);

   }


   private static String fileToString(String fName) throws IOException
   {  InputStream in = new BufferedInputStream(new FileInputStream(fName));
      StringBuilder sb = new StringBuilder(2000);
      byte[] buf = new byte[2048];

      int len;
      while((len=in.read(buf)) > -1)
         sb.append(new String(buf,0,len));

      in.close();
      String grvy = sb.toString();
      // deal with unix #! execution option
      while(grvy.startsWith("#"))
         grvy = grvy.replaceFirst("^[^\n\r]*[\n\r]+", "");

      return grvy;
   }
}

