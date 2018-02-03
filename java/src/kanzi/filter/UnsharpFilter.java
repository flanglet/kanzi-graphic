/*
Copyright 2011-2013 Frederic Langlet
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

package kanzi.filter;

import kanzi.SliceIntArray;
import kanzi.IntFilter;


public final class UnsharpFilter implements IntFilter
{
    private final int width;
    private final int height;
    private final int stride;
    private final int threshold256;
    private final int scale256; // multiplier of pixel difference times 256
    private final IntFilter blurFilter;
    private int[] buf;
    

    public UnsharpFilter(int width, int height)
    {
       this(width, height, width, 2, 60, 100);
    }


    public UnsharpFilter(int width, int height, int stride)
    {
       this(width, height, stride, 2, 60, 100);
    }


    // multiplier of pixel difference in percent.
    public UnsharpFilter(int width, int height, int stride, int sigma, int threshold, int scale)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");

        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");
        
        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");

        if ((scale < 100) || (scale > 1000))
            throw new IllegalArgumentException("The scale must be in [100.1000]");

        this.height = height;
        this.width = width;
        this.stride = stride;
        //this.blurFilter = new BlurFilter(width, height, stride, radius);
        this.blurFilter = new GaussianFilter(width, height, 16*sigma);
        this.threshold256 = threshold << 8;
        this.scale256 = (scale << 8) / 100;
        this.buf = new int[0];
    }


   @Override
   public boolean apply(SliceIntArray input, SliceIntArray output)
   {
      if ((!SliceIntArray.isValid(input)) || (!SliceIntArray.isValid(output)))
         return false;
      
      if (this.buf.length < this.width*this.height)
         this.buf = new int[this.width*this.height];

      // Compute blurred image
      if (this.blurFilter.apply(input, new SliceIntArray(this.buf, this.buf.length, 0)) == false)
         return false;

      final int[] src = input.array;
      final int[] dst = output.array;
      int srcIdx = input.index;
      int dstIdx = output.index;
      final int h = this.height;
      final int w = this.width;
      final int st = this.stride;

      for (int y=h; y>0; y--)
      {
         for (int x=0; x<w; x++)
         {
            final int rgb0 = src[srcIdx+x];
            final int r0 = (rgb0>>16) & 0xFF;
            final int g0 = (rgb0>>8)  & 0xFF;
            final int b0 =  rgb0      & 0xFF;
            final int rgb1 = this.buf[srcIdx+x]; // blurred image
            int r1 = (rgb1>>16) & 0xFF;
            int g1 = (rgb1>>8)  & 0xFF;
            int b1 =  rgb1      & 0xFF;
            int diff, absdiff;
            diff = (r1 - r0) * this.scale256;
            absdiff = (diff + (diff >> 31)) ^ (diff >> 31); // abs(diff)

            if (absdiff < this.threshold256)
            {
               r1 = r0;
            }
            else
            {
               r1 = r0 + (diff>>8);               
               r1 = (r1 >= 255) ? 255 : r1 & ~(r1 >> 31);
            }
            
            diff = (g1 - g0) * this.scale256;
            absdiff = (diff + (diff >> 31)) ^ (diff >> 31); // abs(diff)

            if (absdiff < this.threshold256)
            {
               g1 = g0;
            }
            else
            {
               g1 = g0 + (diff>>8);              
               g1 = (g1 >= 255) ? 255 : g1 & ~(g1 >> 31);
            }
            
            diff = (b1 - b0) * this.scale256;
            absdiff = (diff + (diff >> 31)) ^ (diff >> 31); // abs(diff)

            if (absdiff < this.threshold256)
            {
               b1 = b0;
            }
            else
            {
               b1 = b0 + (diff>>8);             
               b1 = (b1 >= 255) ? 255 : b1 & ~(b1 >> 31);
            }
            
            // Estimate luminance (multiplied by 8)                    
            final int lum0 = (2*r0 + 7*g0 + b0);
            final int lum1 = (2*r1 + 7*g1 + b1);
            
            // Adjust luminance
            if ((lum1 > 1) && (lum0 != lum1))
            {
               r1 = (r1*lum0) / lum1;
               r1 = (r1 >= 255) ? 255 : r1;
               g1 = (g1*lum0) / lum1;
               g1 = (g1 >= 255) ? 255 : g1;
               b1 = (b1*lum0) / lum1;
               b1 = (b1 >= 255) ? 255 : b1;
            }
            
            dst[dstIdx+x] = (r1 << 16) | (g1 << 8) | b1;
         }

         srcIdx += st;
         dstIdx += st;
      } 

      return true;
   }

}
