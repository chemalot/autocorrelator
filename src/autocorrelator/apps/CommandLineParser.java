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



import java.util.HashMap;

/**
 * CommandLineParser parses a list of strings for command line arguements
 * which must beginn with -.
 *
 * CammandLineParser will convert any argument name in the
 * <code>singleComs</code> <code>doubleComs</code>
 * <code>requiredDoubleComs</code> arrays into UPPER CASE.
 *
 *  public static void main(String[] args) throws SQLException
 *  {
 *     CommandLineParser cParser;
 *     String        uRL;
 *     String        user;
 *     String        pw;
 *
 *     String[] modes = {"-IgnoreExist",  "-PrintSQL"};
 *     String[] parms = {"-User", "-Pw", "-URL" };
 *     String[] reqParms = {"-User", "-Pw", "-URL" };
 *
 *     cParser= new CommandLineParser(EXPLAIN, 0, 0, args,
 *                                    modes, parms, reqParms);
 *
 *      uRL  = cParser.getValue("-URL");
 *      user = cParser.getValue("-User");
 *      pw   = cParser.getValue("-Pw");
 *
 *      if(cParser.wasGiven("-RestrictTerm"))
 *          restrictionTerm += " AND " + cParser.getValue("-RestrictTerm");
 *
 *      if(cParser.wasGiven("-MinFTK"))
 *          restrictionTerm += " AND a.Fact_Type_Key >=" +
 *                             cParser.getValue("-MinFTK");
 *
 *      if(cParser.wasGiven("-MaxFTK"))
 *          restrictionTerm += " AND a.Fact_Type_Key <=" +
 *                             cParser.getValue("-MaxFTK");
 *
 */
public class CommandLineParser
{
    private HashMap<String,String> singleComHash = new HashMap<String,String>();
    private HashMap<String,String> doubleComHash = new HashMap<String,String>();
    private String[] rest;

    /**
     * @return the value of a name value parameter eg "true" in "-validate true".
     *   or null if no such parameter was given.
     */
    public String getValue(String doubleCom)
    { return doubleComHash.get(doubleCom.toUpperCase());
    }

    /**
     * returns all command names present on the command line in UPPER CASE.
     *
     * The "-" is included.
     */
    public String[] getSingleComArgs()
    { return singleComHash.keySet().toArray(new String[0]);
    }

    /**
     * @return true if a command name was present on the command line.
     */
    public boolean wasGiven(String com)
    {
        return    singleComHash.containsKey(com.toUpperCase())
               || doubleComHash.containsKey(com.toUpperCase());
    }

    /**
     * @return an array of Strings with the arguments after the last command
     * on the command line.
     */
    public String[] getRestArgs()
    {
        return rest;
    }

    /**
     * Parse the command line arguments and commands.
     *
     * Commands are words begining with a hyphen, there are single word e.g.
     * "-countLines" and double word commands eg. "-validate true".
     *
     * this will print the explain text and exit(1) if a unknown command is given,
     * to many or few arguments are remaining after parsing or any of the required
     * double word commands is not found.
     *
     * @param minRestArgs minumum number of arguments required to be present after
     *                    parsing the commands.
     * @param maxRestArgs maximum number of arguments allowed to be present after
     *                    parsing the commands.
     * @param explain text to be printed in case of errors.
     * @param singleComs single word command names.
     * @param doubleComs double word command names.
     * @param requiredDoubleComs double word commands which are required.
     */
    public CommandLineParser(
        String explain, int minRestArgs, int maxRestArgs,
        String[] args, String[] singleComs, String[] doubleComs,
        String requiredDoubleComs[] )
    {
        int i;
        String com;

        for (i=0; i<singleComs.length; i++)
        {   singleComs[i] = singleComs[i].toUpperCase();
            singleComHash.put(singleComs[i],null);
        }

        for (i=0; i<doubleComs.length; i++)
        {   doubleComs[i] = doubleComs[i].toUpperCase();
            doubleComHash.put(doubleComs[i],null);
        }

        i=0;
        while( i < args.length && args[i].charAt(0) == '-' )
        {   com = args[i].toUpperCase();

            if(singleComHash.containsKey(com))
            {   singleComHash.put(com,com);

            }else if(doubleComHash.containsKey(com))
            {
                if(i+1 >= args.length)
                {   System.err.println("\n" + args[i] + " requires value");
                    System.err.println(explain);
                    System.exit(1);
                }

                doubleComHash.put(com,args[++i]);
            }else
            {
                System.err.println("\n" + args[i] + " unknown parameter");
                System.err.println(explain);
                System.exit(1);
            }

            i++;
        }

        if( args.length - i < minRestArgs )
        {
            System.err.println("\n" + "To few arguments");
            System.err.println(explain);
            System.exit(1);
        }

        if( maxRestArgs > 0 && args.length - i > maxRestArgs )
        {
            System.err.println("\n" + "To many arguments: " +(args.length - i));
            System.err.println(explain);
            System.exit(1);
        }

        rest = new String[args.length-i];
        System.arraycopy(args,i,rest,0,args.length-i);

        for (i=0; i<requiredDoubleComs.length; i++)
        {
            requiredDoubleComs[i] = requiredDoubleComs[i].toUpperCase();

            if( doubleComHash.get(requiredDoubleComs[i]) == null)
            {
                System.err.println("\n"+ requiredDoubleComs[i] +" not given");
                System.err.println(explain);
                System.exit(1);
            }
        }

        // remove unset elements
        for (i=0; i<singleComs.length; i++)
        {
            if(singleComHash.get(singleComs[i]) == null)
                singleComHash.remove(singleComs[i]);
        }

        for (i=0; i<doubleComs.length; i++)
        {
            if(doubleComHash.get(doubleComs[i]) == null)
                doubleComHash.remove(doubleComs[i]);
        }
    }
}
