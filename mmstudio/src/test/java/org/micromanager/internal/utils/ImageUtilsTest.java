package org.micromanager.internal.utils;

import static org.junit.Assert.*;
import org.junit.Test;

public class ImageUtilsTest {
   @Test
   public void unsignedByteValueIsCorrect() {
      assertEquals(0x00, ImageUtils.unsignedValue((byte) 0x00));
      assertEquals(0x01, ImageUtils.unsignedValue((byte) 0x01));
      assertEquals(0xff, ImageUtils.unsignedValue((byte) 0xff));
      assertEquals(0xfe, ImageUtils.unsignedValue((byte) 0xfe));
      assertEquals(0x7f, ImageUtils.unsignedValue((byte) 0x7f));
      assertEquals(0x80, ImageUtils.unsignedValue((byte) 0x80));
   }

   @Test
   public void unsignedShortValueIsCorrect() {
      assertEquals(0x0000, ImageUtils.unsignedValue((short) 0x0000));
      assertEquals(0x0001, ImageUtils.unsignedValue((short) 0x0001));
      assertEquals(0xffff, ImageUtils.unsignedValue((short) 0xffff));
      assertEquals(0xfffe, ImageUtils.unsignedValue((short) 0xfffe));
      assertEquals(0x7fff, ImageUtils.unsignedValue((short) 0x7fff));
      assertEquals(0x8000, ImageUtils.unsignedValue((short) 0x8000));
   }

   @Test
   public void intToRGBA32UnpackingByteOrderIsCorrect() {
      // Since Java is big-endian, the most significant byte is red.
      int pixel = 0;
      pixel += 10 << 24; // red
      pixel += 20 << 16; // green
      pixel += 30 << 8; // blue
      pixel += 40; // unused

      int[] pixels = new int[1]; // Single pixel "image"
      pixels[0] = pixel;

      byte[] result = ImageUtils.convertRGB32IntToBytes(pixels);

      assertEquals(10, result[0]);
      assertEquals(20, result[1]);
      assertEquals(30, result[2]);
      assertEquals(40, result[3]);
   }

   @Test
   public void rgba32ToIntPackingByteOrderIsCorrect() {
      byte[] image = new byte[] { 10, 20, 30, 40 }; // Single pixel "image"
      int[] result = ImageUtils.convertRGB32BytesToInt(image);
      int i = result[0];

      // Java is big-endian, so the most significant byte is red.
      assertEquals(10 << 24, 0xFF000000 & i);
      assertEquals(20 << 16, 0x00FF0000 & i);
      assertEquals(30 <<  8, 0x0000FF00 & i);
      assertEquals(40      , 0x000000FF & i);
   }
}
