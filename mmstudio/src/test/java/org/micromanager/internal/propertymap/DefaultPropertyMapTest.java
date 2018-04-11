package org.micromanager.internal.propertymap;

import org.junit.Test;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultPropertyMapTest {
   @Test
   public void testBooleanRoundTrip() {
      PropertyMap pmap = PropertyMaps.builder().putBoolean("test", true).build();
      assertTrue(pmap.containsBoolean("test"));
      assertEquals(true, pmap.getBoolean("test", false));
   }

   @Test
   public void testByteRoundTrip() {
      PropertyMap pmap = PropertyMaps.builder().putByte("test", (byte) 42).build();
      assertTrue(pmap.containsByte("test"));
      assertTrue(pmap.containsNumber("test"));
      assertEquals((byte) 42, pmap.getByte("test", (byte) 21));
      assertEquals(42, pmap.getAsNumber("test", 21).intValue());

      assertEquals(byte.class, pmap.getValueTypeForKey("test"));

      pmap = PropertyMaps.builder().putByte("null", null).build();
      assertFalse(pmap.containsKey("null"));
   }

   @Test
   public void testShortRoundTrip() {
      PropertyMap pmap = PropertyMaps.builder().putShort("test", (short) 42).build();
      assertTrue(pmap.containsShort("test"));
      assertTrue(pmap.containsNumber("test"));
      assertEquals((short) 42, pmap.getShort("test", (short) 21));
      assertEquals(42, pmap.getAsNumber("test", 21).intValue());

      assertEquals(short.class, pmap.getValueTypeForKey("test"));
   }

   @Test
   public void testGetAsNumber() {
      PropertyMap pmap = PropertyMaps.builder().
         putByte("aByte", (byte) 42).
         putShort("aShort", (short) 43).
         putInteger("anInt", 44).
         putLong("aLong", (long) 45).
         putFloat("aFloat", 46.0f).
         putDouble("aDouble", 47.0).
         build();

      assertTrue(pmap.containsNumber("aByte"));
      assertTrue(pmap.containsNumber("aShort"));
      assertTrue(pmap.containsNumber("anInt"));
      assertTrue(pmap.containsNumber("aLong"));
      assertTrue(pmap.containsNumber("aFloat"));
      assertTrue(pmap.containsNumber("aDouble"));

      assertEquals(42, pmap.getAsNumber("aByte", 99).intValue());
      assertEquals(43, pmap.getAsNumber("aShort", 99).intValue());
      assertEquals(44, pmap.getAsNumber("anInt", 99).intValue());
      assertEquals(45, pmap.getAsNumber("aLong", 99).intValue());
      assertEquals(46, pmap.getAsNumber("aFloat", 99).intValue());
      assertEquals(47, pmap.getAsNumber("aDouble", 99).intValue());
   }
}
