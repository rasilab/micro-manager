package org.micromanager.internal.propertymap;

import org.junit.jupiter.api.Test;
import org.micromanager.PropertyMap;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class LegacyPropertyMap1DeserializerTest {
   @Test
   public void testEmptyInput() {
      assertThrows(IOException.class, () -> LegacyPropertyMap1Deserializer.fromJSON(""));
   }

   @Test
   public void testEmptyObject() throws IOException {
      PropertyMap pmap = LegacyPropertyMap1Deserializer.fromJSON("{}");
      assertEquals(0, pmap.keySet().size());
   }

   @Test
   public void testNonObject() {
      assertThrows(IOException.class, () -> LegacyPropertyMap1Deserializer.fromJSON("[]"));
      assertThrows(IOException.class, () -> LegacyPropertyMap1Deserializer.fromJSON("123"));
      assertThrows(IOException.class, () -> LegacyPropertyMap1Deserializer.fromJSON("null"));
      assertThrows(IOException.class, () -> LegacyPropertyMap1Deserializer.fromJSON("{\"foo\": \"bar\"}"));
      assertThrows(IOException.class, () -> LegacyPropertyMap1Deserializer.fromJSON("\"baz\""));
   }

   @Test
   public void testDeserialize() throws IOException {
      PropertyMap pmap = LegacyPropertyMap1Deserializer.fromJSON(String.join("\n",
         "{",
         "\"foo\": {",
         "\"PropType\": \"String\",",
         "\"PropVal\": \"FOO\"",
         "},",
         "\"bar\": {",
         "\"PropType\": \"Integer array\",",
         "\"PropVal\": [1, 2, 3]",
         "}",
         "}"));
      assertEquals(2, pmap.keySet().size());

      assertEquals("FOO", pmap.getString("foo", "not FOO"));
      assertIterableEquals(IntStream.of(1, 2, 3).mapToObj(i -> i).collect(Collectors.toList()),
         pmap.getIntegerList("bar", Collections.emptyList()));
   }
}
