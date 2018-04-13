package org.micromanager.internal.propertymap;

import org.junit.Test;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

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

   private enum Foo {
      BAR,
   }

   @Test
   public void testNullDefaultValues() {
      // Make sure 'null' works as a valid default value in scalar getters

      PropertyMap pmap = PropertyMaps.emptyPropertyMap();

      // Immutable base type
      assertNull(pmap.getString("foo", null));

      // Mutable base type
      assertNull(pmap.getRectangle("foo", null));

      // StringAsEnum
      assertNull(pmap.getStringAsEnum("foo", Foo.class, null));
   }

   @Test
   public void testNullListDefaultValues() {
      PropertyMap pmap = PropertyMaps.emptyPropertyMap();

      // Primitive array
      assertEquals(0, pmap.getBooleanList("foo", (boolean[]) null).length);

      // Boxed list
      assertEquals(0, pmap.getBooleanList("foo", (List<Boolean>) null).size());

      // Number list
      assertEquals(0, pmap.getAsNumberList("foo", (Number[]) null).size());
      assertEquals(0, pmap.getAsNumberList("foo", (List<Number>) null).size());

      // Generic list
      assertEquals(0, pmap.getStringList("foo", (String[]) null).size());
      assertEquals(0, pmap.getStringList("foo", (List<String>) null).size());

      // StringAsEnum
      assertEquals(0, pmap.getStringListAsEnumList("foo", Foo.class,
         (Foo[]) null).size());
      assertEquals(0, pmap.getStringListAsEnumList("foo", Foo.class,
         (List<Foo>) null).size());
   }
}
