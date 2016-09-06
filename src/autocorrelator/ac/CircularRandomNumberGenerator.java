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


/**
 * Random Number Generator which guaranties that the all numbers between 0 and the 
 * maxVal are generated once before the whole cycle is repeated.
 * 
 */
 public class CircularRandomNumberGenerator
{   private long max;
    private long modMask;         // take mod by AND
    private long a;
    private static final long c = 1;
    private long x;               // current state
    private static final long MAXBITS = 63;

    public CircularRandomNumberGenerator(long maxVal)
   {   this( maxVal, System.currentTimeMillis());
   }

    public CircularRandomNumberGenerator(long maxVal, long Seed)
   {   this.max = maxVal;
       this.x   = Seed;

       long modulus;

       modulus = 1;
       while( modulus <= max && modulus > 0)
      {   modulus *= 2;
      }

       modMask = modulus-1;
       if( modMask < 0 ) modMask=(long)(Math.pow(2,MAXBITS)-1);  // mask only sign bit


       // a needs to be one of 1 + i * 4    with i = any of 1,2,3...
       // set to value near by 5/6 max
       a = max / 2 + max / 3;               // 5/6 max
       a -= a % 4 - 1;                      // subtract to get i*4 + 1

       x = Seed;

       // System.err.println("a=" + a);
       // System.err.println("c=" + c);
       // System.err.println("ModMask=" + modMask);
       // System.err.println("max=" + max);
       // System.err.println("maxbits=" + MAXBITS);
   }

    public long nextValue()
   {   long res;

       do
      {   res = a * x + c;
          res &= modMask;
          x   = res;
      } while( res > max );           // statistically needs to be repeated at
                                      // most 1 time

       return res;
   }



/*****************************************************************************
   static public void main(String args[])
  {   long i;
      long maxval = Integer.parseInt(args[0]);
      CircularRandomNumberGenerator CRNG =
          new CircularRandomNumberGenerator( maxval );

      for( i=0; i<=maxval+2; i++ )
     {   System.out.println( i + "\t" + CRNG.nextValue() );
     }
  }
 *****************************************************************************/
}
