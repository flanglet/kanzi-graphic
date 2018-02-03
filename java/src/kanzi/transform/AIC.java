/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kanzi.transform;

import kanzi.ByteTransform;
import kanzi.SliceByteArray;


/**
 *
 * @author fred
 */
public class AIC implements ByteTransform
{
   private final int[] S2R;
   private final int[] P;
   
   
   public AIC()
   {
      this.P = new int[256];
      this.S2R = new int[256];
   }
   
   
   @Override
   public boolean forward(SliceByteArray src, SliceByteArray dst)
   { 	
      if ((!SliceByteArray.isValid(src)) || (!SliceByteArray.isValid(dst)))
         return false;

      if (src.array == dst.array)
         return false;

      final int count = src.length;

      if (dst.length < count)
         return false;

      if (dst.index + count > dst.array.length)
         return false;

      final byte[] input = src.array;
      final byte[] output = dst.array;
      final int srcIdx = src.index;
      final int dstIdx = dst.index;
      
      for (int k=0; k<256; k++)
      {
         P[k] = k;
         S2R[k] = k;
      }
      
      for(int i = 0; i < count; i++)
      {
         byte c = input[srcIdx+i];
         int idx = S2R[c&0xFF];	
         output[dstIdx+i] = (byte) idx;
         
         if (idx > 0)
         {
            int LB = idx >> 1;	// lower bound
            do { S2R[P[idx]=P[idx-1]] = idx; } while(LB < --idx);
            P[LB] = c;
            S2R[P[LB]=c&0xFF] = LB;
         }
      }
      
       src.index += count;
       dst.index += count;
       return true;
   }

  @Override
   public boolean inverse(SliceByteArray src, SliceByteArray dst)
   {
      if ((!SliceByteArray.isValid(src)) || (!SliceByteArray.isValid(dst)))
         return false;

      if (src.array == dst.array)
         return false;

      final int count = src.length;

      if (dst.length < count)
         return false;

      if (dst.index + count > dst.array.length)
         return false;

      final byte[] input = src.array;
      final byte[] output = dst.array;
      final int srcIdx = src.index;
      final int dstIdx = dst.index;
      
      for (int k=0; k<256; k++) 
         P[k] = k;
      
      for (int i=0; i<count; i++)
      {
         byte idx = input[srcIdx+i];
         int c = P[idx&0xFF];
         output[dstIdx+i] = (byte) c;
         
         if (idx > 0)
         {
            int LB = idx >> 1;
            do { P[idx] = P[idx-1]; } while(LB < --idx);
            P[LB] = c;
         }
      }
       
      src.index += count;
      dst.index += count;
      return true;
   }
}
