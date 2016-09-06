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


 public class MixedBaseNumberConverter
{  private int[] bases;
   public  long  maxVal;

   public MixedBaseNumberConverter(int[] bases)
  {   int i;
      this.bases = bases;

      maxVal=bases[0];
      for(i=1; i<bases.length; i++ )
     {   maxVal *= bases[i];
     }
      maxVal--;
  }

   public long getMaxValue()
  {   return maxVal;
  }

  /**
   * translate the number from Mixed based Digits to a long.
   *
   * @param mixedDigits array containing a digit each eg. mixedDigits[0] is the
   * first digit which is a number from 0 to bases[0]-1
   *
   * @return the long representation (a number from 0 to maxVal-1)
   */
   public long getLong(int [] mixedDigits )
  {   long res;
      int  i;

      res=mixedDigits[bases.length-1];
      for(i=bases.length-2; i>=0; i--)
     {   res = res * bases[i]+ mixedDigits[i];
     }
      return res;
  }

  /**
   * transforms the longNum into a sequence of digits of the mixed based number.
   *
   * longNum should be &gt;=0 and &lt;= maxVal.
   *
   */
   public int[] getMixedBasedDigits( long longNum )
  {   int[] res = new int[ bases.length ];
      int   i;

      for( i=0; i<bases.length; i++ )
     {   res[i] = (int)(longNum % bases[i]);
         longNum = longNum / bases[i];
     }

      return res;
  }


/***************************************************************************
   static public void main(String args[])
  {   int[] Bases = { 12, 99, 59 };
      MixedBaseNumberConverter MBNC = new MixedBaseNumberConverter( Bases );
      long i;
      int  pos;
      int[]  MNums = new int[ Bases.length ];

      System.out.println( "MaxVal = " + MBNC.MaxVal );

      for( i=0; i <= MBNC.MaxVal; i++ )
     {   System.out.print( i + "\t" );

         MNums = MBNC.getMixedBaseNumber( i );
         System.out.print( MBNC.getDez( MNums ) + "\t Nums:\t" );

         for( pos=0; pos<Bases.length; pos++ )
        {   System.out.print( MNums[pos] + "\t" );
        }
         System.out.println();
     }
  }
 ***************************************************************************/
}
