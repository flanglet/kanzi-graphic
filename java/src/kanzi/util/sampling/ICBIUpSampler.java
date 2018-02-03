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

package kanzi.util.sampling;


public class ICBIUpSampler implements UpSampler
{
    private final int width;
    private final int height;
    private final int stride;
    private final int offset;


    public ICBIUpSampler(int width, int height)
    {
        this(width, height, width, 0);
    }

    
    public ICBIUpSampler(int width, int height, int stride, int offset)
    {
      if (height < 8)
         throw new IllegalArgumentException("The height must be at least 8");

      if (width < 8)
         throw new IllegalArgumentException("The width must be at least 8");

      if (offset < 0)
         throw new IllegalArgumentException("The offset must be at least 0");

      if (stride < width)
         throw new IllegalArgumentException("The stride must be at least as big as the width");

      if ((height & 7) != 0)
         throw new IllegalArgumentException("The height must be a multiple of 8");

      if ((width & 7) != 0)
         throw new IllegalArgumentException("The width must be a multiple of 8");

      this.height = height;
      this.width = width;
      this.stride = stride;
      this.offset = offset;
    }
    
    
    @Override
    // Supports in place resampling
    public void superSampleVertical(int[] input, int[] output)
    {
       this.superSampleVertical(input, output, this.width, this.height, this.stride);
    }
    
    
    private void superSampleVertical(int[] input, int[] output, int sw, int sh, int st)
    {
      final int st2 = st + st;
      final int dw = sw;
      final int dh = sh * 2; 
      int iOffs = st * (sh - 1) + this.offset;
      int oOffs = dw * (dh - 1);

      // Rows h-1, h-2, h-3
      System.arraycopy(input, iOffs, output, oOffs, sw);
      oOffs -= dw;                    
      System.arraycopy(input, iOffs, output, oOffs, sw);
      oOffs -= dw;
      iOffs -= st;   
  
      for (int i=0; i<sw; i++)
         output[oOffs+i] = (input[iOffs+i] + input[iOffs+st+i]) >> 1;
      
      oOffs -= dw;                                              
      
      for (int j=sh-3; j>0; j--)
      {
         // Copy 
         System.arraycopy(input, iOffs, output, oOffs, sw);
         oOffs -= dw;    
         iOffs -= st;

         // Interpolate
         for (int i=0; i<sw; i++)
         {
            final int p0 = input[iOffs+i-st];
            final int p1 = input[iOffs+i];
            final int p2 = input[iOffs+i+st];
            final int p3 = input[iOffs+i+st2];

            //output[oOffs+i] = (int) (p1 + 0.5 * x*(p2 - p0 + x*(2.0*p0 - 5.0*p1 + 4.0*p2 - p3 + x*(3.0*(p1 - p2) + p3 - p0))));
            final int val = (p1<<4) + (p2<<2) - (p1<<3) - (p1<<1) + (p2<<3) - (p3<<1) + 
               ((p1<<1) + p1 - (p2<<1) - p2 + p3 - p0);
            output[oOffs+i] = (val < 0) ? 0 : ((val >= 4087) ? 255 : (val+8)>>4); 
         } 
         
         oOffs -= dw;        
      }    
      
      // Rows 1, 2, 3
      for (int i=0; i<sw; i++)
         output[oOffs+i] = (input[iOffs+st+i] + input[iOffs+i]) >> 1;
      
      oOffs -= dw;        
      System.arraycopy(input, iOffs, output, oOffs, sw);            
      oOffs -= dw;        
      System.arraycopy(input, iOffs, output, oOffs, sw);               
    }


    @Override
    // Supports in place resampling
    public void superSampleHorizontal(int[] input, int[] output)
    {
       this.superSampleHorizontal(input, output, this.width, this.height, this.stride);
    }
    
   
    private void superSampleHorizontal(int[] input, int[] output, int sw, int sh, int st)
    {
       final int dw = sw * 2;
       final int dh = sh;
       int iOffs = st * (sh - 1) + this.offset;
       int oOffs = dw * (dh - 1);

       for (int j=sh-1; j>=0; j--)
       {
         // Columns w-1, w-2, w-3
         int val = input[iOffs+sw-1];
         output[oOffs+dw-1] = val;
         output[oOffs+dw-2] = val;             
         output[oOffs+dw-3] = (val+input[iOffs+sw-2]) >> 1;

         for (int i=sw-3; i>0; i--)
         {
            final int idx = oOffs + (i << 1);
            final int p0 = input[iOffs+i-1];
            final int p1 = input[iOffs+i];
            final int p2 = input[iOffs+i+1];
            final int p3 = input[iOffs+i+2];

            // Copy
            output[idx+2] = p2;

            // Interpolate
            //output[idx+1] = (int) (p1 + 0.5 * x*(p2 - p0 + x*(2.0*p0 - 5.0*p1 + 4.0*p2 - p3 + x*(3.0*(p1 - p2) + p3 - p0))));
            val = (p1<<4) + (p2<<2) - (p1<<3) - (p1<<1) + (p2<<3) - (p3<<1) + 
               ((p1<<1) + p1 - (p2<<1) - p2 + p3 - p0);
            output[idx+1] =  (val < 0) ? 0 : ((val >= 4087) ? 255 : (val+8)>>4); 
         }

         // Columns 1, 2, 3
         val = input[iOffs];
         output[oOffs+2] = (val + input[iOffs+1]) >> 1;             
         output[oOffs+1] = val;             
         output[oOffs] = val;             
         iOffs -= st;
         oOffs -= dw;
       }
    }


    // p00 o p10 o p20 o p30 o
    //  o  o  o  o  o  o  o  o
    // p01 o p11 o p21 o p31 o
    //  o  o  o  x  o  o  o  o
    // p02 o p12 o p22 o p32 o
    //  o  o  o  o  o  o  o  o
    // p03 o p13 o p23 o p33 o
    //  o  o  o  o  o  o  o  o
    @Override
    public void superSample(int[] input, int[] output)
    {
       // Positions (2x, 2y) and (2x+1, 2y+1)
       this.superSampleStep1(input, output);
       
       // Positions (2x+1, 2y) and (2x, 2y+1)
  ///     this.superSampleStep2(output, output);
    }
    
    
    private void superSampleStep1(int[] input, int[] output)
    {
       final int sw = this.width;
       final int sh = this.height;
       final int st = this.stride;
       final int dw = sw + sw;
       final int dw2 = dw + dw;
       final int dh = sh + sh;
       int iOffs = st * (sh - 3) + this.offset;
       int oOffs = dw * (dh - 3);

         // Last 3 rows, only horizontal interpolation
        for (int i=sw-1; i>=0; i--)
        {
           final int valA = input[iOffs+i];
           final int valB = (i == sw-1) ? valA : input[iOffs+i+1];
           final int valAB = (valA + valB) >> 1;
           int k = oOffs + (i << 1);
           output[k]    = valA;
           output[k+1]  = valAB;
           k += dw;
           output[k]    = valA;
           output[k+1]  = valAB;
        }

        int last = input[iOffs+sw-1];
        output[oOffs+dw-1] = last;
        output[oOffs+dw-2] = last;             
        output[oOffs+dw-3] = (last+input[iOffs+sw-2]) >> 1;
//
//
//          iOffs -= st;
//          oOffs -= dw;
    
       // Step 1
       for (int j=sh-3; j>0; j--)
       {      
         final int iOffs0 = iOffs - st;
         final int iOffs1 = iOffs0 + st;
         final int iOffs2 = iOffs1 + st;
         final int iOffs3 = iOffs2 + st;
         int p10 = input[iOffs0+sw-3];
         int p20 = input[iOffs0+sw-2];
         int p30 = input[iOffs0+sw-1];
         int p11 = input[iOffs1+sw-3];
         int p21 = input[iOffs1+sw-2];
         int p31 = input[iOffs1+sw-1];
         int p12 = input[iOffs2+sw-3];
         int p22 = input[iOffs2+sw-2];
         int p32 = input[iOffs2+sw-1];
         int p13 = input[iOffs3+sw-3];
         int p23 = input[iOffs3+sw-2];
         int p33 = input[iOffs3+sw-1];
         
         // Columns w-1, w-2, w-3, w-4
         output[oOffs-dw+dw-1] = p30;
         output[oOffs-dw+dw-2] = p30;             
         output[oOffs-dw+dw-3] = (p30+p20) >> 1;         
         output[oOffs-dw+dw-4] = p20;         
         output[oOffs+dw-1] = p31;
         output[oOffs+dw-2] = p31;             
         output[oOffs+dw-3] = (p31+p21) >> 1;         
         output[oOffs+dw-4] = p21;      
         output[oOffs+dw+dw-1] = p32;
         output[oOffs+dw+dw-2] = p32;             
         output[oOffs+dw+dw-3] = (p32+p22) >> 1;         
         output[oOffs+dw+dw-4] = p22;          
         output[oOffs+dw2+dw-1] = p33;
         output[oOffs+dw2+dw-2] = p33;             
         output[oOffs+dw2+dw-3] = (p33+p23) >> 1;         
         output[oOffs+dw2+dw-4] = p23;           
             
         for (int i=sw-4; i>0; i--)
         {
            final int idx = oOffs + (i << 1);
            final int p00 = input[iOffs0+i];
            final int p01 = input[iOffs1+i];
            final int p02 = input[iOffs2+i];
            final int p03 = input[iOffs3+i];
            final int valD1 = (p02 + p11 + p20) - 3*(p12 + p21) + (p13 + p22 + p31);
            final int valD2 = (p10 + p21 + p32) - 3*(p11 + p22) + (p01 + p12 + p23);
            final int valD = (valD1 > valD2) ? (p11 + p22) >> 1 : (p12 + p21) >> 1; 
            
            output[idx-dw+2] = p11;
            
            // a
            output[idx-dw+3] = p11 + (((p21<<2) - (p01<<2) + ((p01<<2) - (p11<<3) - (p11<<1) + 
               (p21<<3) - (p31<<1) + ((p11<<1) + p11 - (p21<<1) - p21 + p31 - p01)) + 8) >> 4);
//output[idx-dw+3] = (p11+p21)/2;           
            // b
            output[idx+2] = p11 + (((p12<<2) - (p10<<2) + ((p10<<2) - (p11<<3) - (p11<<1) + 
               (p12<<3) - (p13<<1) + ((p11<<1) + p11 - (p12<<1) - p12 + p13 - p10)) + 8) >> 4);
//output[idx+2] =(p11+p12)/2;
            // c
            output[idx+3] = valD;           
            
            // Slide window
            p30 = p20; p20 = p10; p10 = p00;
            p31 = p21; p21 = p11; p11 = p01;
            p32 = p22; p22 = p12; p12 = p02;
            p33 = p23; p23 = p13; p13 = p03;                       
         }  
          
         // Columns 1, 2, 3
//         int val = input[iOffs];
//         output[oOffs+st+2] = (val + input[iOffs+1]) >> 1;             
//         output[oOffs+st+1] = val;             
//         output[oOffs+st] = val;   
//         output[oOffs+2] = (val + input[iOffs+1]) >> 1;             
//         output[oOffs+1] = val;             
//         output[oOffs] = val;   
        
         iOffs -= st;
         oOffs -= dw;
         oOffs -= dw;
       } 
    }

    
    private void superSampleStep2(int[] input, int[] output)
    {
       final int sw = this.width;
       final int sh = this.height;
       final int st = this.stride;
       final int dw = sw + sw;
       final int dw2 = dw + dw;
       final int dh = sh + sh;
       int iOffs = st * (sh - 3) + this.offset;
       int oOffs = dw * (dh - 3);

         // Last 3 rows, only horizontal interpolation
        for (int i=sw-1; i>=0; i--)
        {
           final int valA = input[iOffs+i];
           final int valB = (i == sw-1) ? valA : input[iOffs+i+1];
           final int valAB = (valA + valB) >> 1;
           int k = oOffs + (i << 1);
           output[k]    = valA;
           output[k+1]  = valAB;
           k += dw;
           output[k]    = valA;
           output[k+1]  = valAB;
        }

        int last = input[iOffs+sw-1];
        output[oOffs+dw-1] = last;
        output[oOffs+dw-2] = last;             
        output[oOffs+dw-3] = (last+input[iOffs+sw-2]) >> 1;
//
//
//          iOffs -= st;
//          oOffs -= dw;
    
       // Step 1
       for (int j=sh-3; j>0; j--)
       {      
         final int iOffs0 = iOffs - st;
         final int iOffs1 = iOffs0 + st;
         final int iOffs2 = iOffs1 + st;
         final int iOffs3 = iOffs2 + st;
         int p10 = input[iOffs0+sw-3];
         int p20 = input[iOffs0+sw-2];
         int p30 = input[iOffs0+sw-1];
         int p11 = input[iOffs1+sw-3];
         int p21 = input[iOffs1+sw-2];
         int p31 = input[iOffs1+sw-1];
         int p12 = input[iOffs2+sw-3];
         int p22 = input[iOffs2+sw-2];
         int p32 = input[iOffs2+sw-1];
         int p13 = input[iOffs3+sw-3];
         int p23 = input[iOffs3+sw-2];
         int p33 = input[iOffs3+sw-1];
         
         // Columns w-1, w-2, w-3, w-4
         output[oOffs-dw+dw-1] = p30;
         output[oOffs-dw+dw-2] = p30;             
         output[oOffs-dw+dw-3] = (p30+p20) >> 1;         
         output[oOffs-dw+dw-4] = p20;         
         output[oOffs+dw-1] = p31;
         output[oOffs+dw-2] = p31;             
         output[oOffs+dw-3] = (p31+p21) >> 1;         
         output[oOffs+dw-4] = p21;      
         output[oOffs+dw+dw-1] = p32;
         output[oOffs+dw+dw-2] = p32;             
         output[oOffs+dw+dw-3] = (p32+p22) >> 1;         
         output[oOffs+dw+dw-4] = p22;          
         output[oOffs+dw2+dw-1] = p33;
         output[oOffs+dw2+dw-2] = p33;             
         output[oOffs+dw2+dw-3] = (p33+p23) >> 1;         
         output[oOffs+dw2+dw-4] = p23;           
             
         for (int i=sw-4; i>0; i--)
         {
            final int idx = oOffs + (i << 1);
            final int p00 = input[iOffs0+i];
            final int p01 = input[iOffs1+i];
            final int p02 = input[iOffs2+i];
            final int p03 = input[iOffs3+i];
            final int valD1 = (p02 + p11 + p20) - 3*(p12 + p21) + (p13 + p22 + p31);
            final int valD2 = (p10 + p21 + p32) - 3*(p11 + p22) + (p01 + p12 + p23);
            final int valD = (valD1 > valD2) ? (p11 + p22) >> 1 : (p12 + p21) >> 1; 
            
            output[idx-dw+2] = p11;
            
            // a
            output[idx-dw+3] = p11 + (((p21<<2) - (p01<<2) + ((p01<<2) - (p11<<3) - (p11<<1) + 
               (p21<<3) - (p31<<1) + ((p11<<1) + p11 - (p21<<1) - p21 + p31 - p01)) + 8) >> 4);
//output[idx-dw+3] = (p11+p21)/2;           
            // b
            output[idx+2] = p11 + (((p12<<2) - (p10<<2) + ((p10<<2) - (p11<<3) - (p11<<1) + 
               (p12<<3) - (p13<<1) + ((p11<<1) + p11 - (p12<<1) - p12 + p13 - p10)) + 8) >> 4);
//output[idx+2] =(p11+p12)/2;
            // c
            output[idx+3] = valD;           
            
            // Slide window
            p30 = p20; p20 = p10; p10 = p00;
            p31 = p21; p21 = p11; p11 = p01;
            p32 = p22; p22 = p12; p12 = p02;
            p33 = p23; p23 = p13; p13 = p03;                       
         }  
          
         // Columns 1, 2, 3
//         int val = input[iOffs];
//         output[oOffs+st+2] = (val + input[iOffs+1]) >> 1;             
//         output[oOffs+st+1] = val;             
//         output[oOffs+st] = val;   
//         output[oOffs+2] = (val + input[iOffs+1]) >> 1;             
//         output[oOffs+1] = val;             
//         output[oOffs] = val;   
        
         iOffs -= st;
         oOffs -= dw;
         oOffs -= dw;
       } 
    }
    
    
    @Override
    public boolean supportsScalingFactor(int factor)
    {
        return (factor == 2);
    }
}