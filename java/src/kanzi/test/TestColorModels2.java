package kanzi.test;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.ColorModelType;
import kanzi.util.color.ColorModelConverter;
import kanzi.util.color.RGBColorModelConverter;
import kanzi.util.color.ReversibleYUVColorModelConverter;
import kanzi.util.color.XYZColorModelConverter;
import kanzi.util.color.YCbCrColorModelConverter;
import kanzi.util.image.ImageUtils;


/**
 *
 * @author fred
 */
public class TestColorModels2
{
   public static void main(String[] args)
   {
      try
      {
         String fileName = (args.length > 0) ? args[0] : "r:\\monet.jpg";         

         // Load image (PPM/PGM supported)
         String type = fileName.substring(fileName.lastIndexOf(".")+1);
         System.out.println(fileName);
         ImageUtils.ImageInfo ii = ImageUtils.loadImage(new FileInputStream(fileName), type);

         boolean display = true;
         final int w = ii.width & -8;
         final int h = ii.height & -8;
         System.out.println(w+"x"+h);
         GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
         GraphicsConfiguration gc = gs.getDefaultConfiguration();
         BufferedImage img1 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
         img1.getRaster().setDataElements(0, 0, w, h, ii.data);
         JFrame frame1 = new JFrame("Original");
         frame1.setBounds(30, 30, w, h);
         ImageIcon newIcon1 = new ImageIcon(img1);
         frame1.add(new JLabel(newIcon1));
         frame1.setVisible(display);

         ColorModelConverter cvt;
         ColorModelType cType;
         int adjust;
         cvt = new ReversibleYUVColorModelConverter(w, h); cType = ColorModelType.YUV444; adjust = 128;
         cvt = new XYZColorModelConverter(w, h); cType = ColorModelType.XYZ; adjust = 0;
         cvt = new RGBColorModelConverter(w, h); cType = ColorModelType.RGB; adjust = 0;
         //cvt = new YCoCgColorModelConverter(w, h); cType = ColorModelType.YUV444; adjust = 128;
         //cvt = new YIQColorModelConverter(w, h); cType = ColorModelType.YUV444; adjust = 128;
         cvt = new YCbCrColorModelConverter(w, h); cType = ColorModelType.YUV444; adjust = 0;       
         //cvt = new YSbSrColorModelConverter(w, h); cType = ColorModelType.YUV444; adjust = 128;         
         int[] rgb = ii.data;
         int[] y = new int[w*h];
         int[] u = new int[w*h];
         int[] v = new int[w*h];
         //new ImageUtils(w, h).toGrey(rgb);
         cvt.convertRGBtoYUV(rgb, y, u, v, cType);
         long sumY = 0;
         long sumU = 0;
         long sumV = 0;

         for (int i=0; i<w*h; i++)
         {
            int val;
            val = y[i] & 0xFF;
            sumY += val;
            y[i] = (val<<16) | (val<<8) | val;
            val = (u[i]+adjust) & 0xFF;
            u[i] = (val<<16) | (val<<8) | val;
            sumU += val;
            val = (v[i]+adjust) & 0xFF;
            v[i] = (val<<16) | (val<<8) | val;
            sumV += val;
         }
         
         float avgY = (float)(sumY)/(w*h);
         float avgU = (float)(sumU)/(w*h);
         float avgV = (float)(sumV)/(w*h);
         String title;
         BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
         img2.getRaster().setDataElements(0, 0, w, h, y);
         float nrjPix1 = Math.abs(avgY - (float) 128);
         title = cvt + " 1   " + nrjPix1;
         System.out.println(title);
         JFrame frame2 = new JFrame(title);
         frame2.setBounds(Math.min(w+50, 800), 30, w, h);
         ImageIcon newIcon2 = new ImageIcon(img2);
         frame2.add(new JLabel(newIcon2));
         frame2.setVisible(display);
         BufferedImage img3 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
         img3.getRaster().setDataElements(0, 0, w, h, u);
         float nrjPix2 =  Math.abs(avgU - (float) 128);
         title = cvt + " 2   " + nrjPix2;
         System.out.println(title);
         JFrame frame3 = new JFrame(title);
         frame3.setBounds(30, Math.min(600, h+50), w, h);
         ImageIcon newIcon3 = new ImageIcon(img3);
         frame3.add(new JLabel(newIcon3));
         frame3.setVisible(display);
         BufferedImage img4 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
         img4.getRaster().setDataElements(0, 0, w, h, v);
         float nrjPix3 =  Math.abs(avgV - (float) 128);
         title = cvt + " 3   " + nrjPix3;
         System.out.println(title);
         JFrame frame4 = new JFrame(title);
         frame4.setBounds(Math.min(w+50, 800), Math.min(600, h+50), w, h);
         ImageIcon newIcon4 = new ImageIcon(img4);
         frame4.add(new JLabel(newIcon4));
         frame4.setVisible(display);
         float ypercent = ((float)nrjPix1*100.0f) / (float)(nrjPix1+nrjPix2+nrjPix3);
         System.out.println("Energy in channel 1: " + ypercent + "%");
         float upercent = ((float)nrjPix2*100.0f) / (float)(nrjPix1+nrjPix2+nrjPix3);
         System.out.println("Energy in channel 1: " + upercent + "%");
         float vpercent = ((float)nrjPix3*100.0f) / (float)(nrjPix1+nrjPix2+nrjPix3);
         System.out.println("Energy in channel 1: " + vpercent + "%");
         
         if (display)
            Thread.sleep(90000);         
      }      
      catch (Exception e)
      {
         e.printStackTrace();
      }

      System.exit(0);
   }
}
