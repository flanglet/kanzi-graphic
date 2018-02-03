/*
Copyright 2011-2017 Frederic Langlet
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package kanzi.transform;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import kanzi.ByteTransform;
import kanzi.SliceByteArray;
import kanzi.entropy.Predictor;
import kanzi.entropy.TPAQPredictor;


/**
 *
 * @author fred
 */
public class PredictorTransform implements ByteTransform
{
   private final Predictor predictor;
   
   
   public PredictorTransform(Predictor p) 
   {
      this.predictor = p;
   }
   
   @Override
   public boolean forward(SliceByteArray src, SliceByteArray dst)
   {
        if ((!SliceByteArray.isValid(src)) || (!SliceByteArray.isValid(dst)))
           return false;

        if (src.array == dst.array)
           return false;
        
        final int count = src.length;
        
        if (dst.index + count > dst.array.length)
           return false;
        
        final byte[] input = src.array;
        final byte[] output = dst.array;
        final int srcIdx = src.index;
        final int dstIdx = dst.index;
        final Predictor p = this.predictor;
int zeros1 = 0;        
int zeros2 = 0;   
int bin = 0;
int[] za = new int[9];
int[] zb = new int[9];
        
        for (int i=0; i<count; i++)
        {
           int in = input[srcIdx+i] & 0xFF;
           int out = 0;
           int zeros3 = 0, zeros4 = 0;
           int pr;
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 7);
           p.update((in>>7)&1);
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 6);
           p.update((in>>6)&1);
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 5);
           p.update((in>>5)&1);
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 4);
           p.update((in>>4)&1);
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 3);
           p.update((in>>3)&1);
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 2);
           p.update((in>>2)&1);
           pr = p.get();
           out |= (((2047-pr) >>> 31) << 1);
           p.update((in>>1)&1);
           pr = p.get();
           out |= (((2047-pr) >>>31));
           p.update(in&1);
//System.out.println(in+" "+out+" "+(in^out));
           out ^= in;
           if (((in>>7)&1) == 1) bin++;           
           if (((in>>7)&1) == 0) zeros3++;
           if (((in>>6)&1) == 0) zeros3++;
           if (((in>>5)&1) == 0) zeros3++;
           if (((in>>4)&1) == 0) zeros3++;
           if (((in>>3)&1) == 0) zeros3++;
           if (((in>>2)&1) == 0) zeros3++;
           if (((in>>1)&1) == 0) zeros3++;
           if (((in>>0)&1) == 0) zeros3++;
           if (((out>>7)&1) == 0) zeros4++;
           if (((out>>6)&1) == 0) zeros4++;
           if (((out>>5)&1) == 0) zeros4++;
           if (((out>>4)&1) == 0) zeros4++;
           if (((out>>3)&1) == 0) zeros4++;
           if (((out>>2)&1) == 0) zeros4++;
           if (((out>>1)&1) == 0) zeros4++;
           if (((out>>0)&1) == 0) zeros4++;
           zb[zeros3]++;
           zeros1 += zeros3;
           za[zeros4]++;
           zeros2 += zeros4;
           output[dstIdx+i] = (byte) out;
        }
        
        // Distribution per number of bits=1 in one byte
        for (int i=0; i<=8; i++)
           System.out.println(i+" zeros per byte before: "+zb[i]);

        System.out.println("");
        
        for (int i=0; i<=8; i++)
           System.out.println(i+" zeros per byte after: "+za[i]);
        
        System.out.println("");
        System.out.println("binary before: "+bin);
        System.out.println("zero bits before: "+((float)zeros1/(count*8)));        
        System.out.println("zero bits after:  "+((float)zeros2/(count*8)));        
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
        
       if (dst.index + count > dst.array.length)
           return false;      
       
       return true;
   }
   
   
   public static void main(String[] args) throws Exception
   {
      {
         int[] zz = new int[9];
         
         for (int i=0; i<256; i++)
         {
            int z = 0;
            if (((i>>7)&1) == 0) z++;
            if (((i>>6)&1) == 0) z++;
            if (((i>>5)&1) == 0) z++;
            if (((i>>4)&1) == 0) z++;
            if (((i>>3)&1) == 0) z++;
            if (((i>>2)&1) == 0) z++;
            if (((i>>1)&1) == 0) z++;
            if (((i>>0)&1) == 0) z++;
            zz[z]++;
         }
         
         System.out.println("");
      }
      String fileName = (args.length > 1) ? args[1] : "r:\\enwik8";
      FileInputStream fis = new FileInputStream(fileName);
      FileOutputStream fos = new FileOutputStream(fileName+".out");
      byte[] buf1 = new byte[10000000];
      byte[] buf2 = new byte[10000000];
      int read;
      PredictorTransform pt = new PredictorTransform(new TPAQPredictor());
         
      while ((read = fis.read(buf1)) > 0) 
      {
         SliceByteArray sba1 = new SliceByteArray(buf1, read, 0);
         SliceByteArray sba2 = new SliceByteArray(buf2, read, 0);
         pt.forward(sba1, sba2);
         fos.write(buf2, 0, read);
      }
      
      fis.close();
      fos.close();
   }   
}
