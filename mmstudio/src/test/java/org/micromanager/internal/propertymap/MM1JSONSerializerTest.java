package org.micromanager.internal.propertymap;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MM1JSONSerializerTest {
   @Test
   public void testScalarDeserialization() throws IOException {
      PropertyMap pmap = MM1JSONSerializer.fromJSON(String.join("\n",
         "{",
         "  \"foo\": \"FOO\",",
         "  \"integer\": 123,",
         "  \"floatingpoint\": 0.5,",
         "  \"bar\": true",
         "}"));

      assertEquals("FOO", pmap.getString("foo", "not FOO"));
      assertTrue(pmap.containsLong("integer"));
      assertEquals(123, pmap.getAsNumber("integer", null).intValue());
      assertTrue(pmap.containsDouble("floatingpoint"));
      assertEquals(0.5, pmap.getAsNumber("floatingpoint", null).doubleValue());
      assertEquals(true, pmap.getBoolean("bar", false));
   }

   @Test
   public void testArrayDeserialization() throws IOException {
      PropertyMap pmap = MM1JSONSerializer.fromJSON(String.join("\n",
         "{",
         "  \"foo\": [1, 2, 3],",
         "  \"bar\": [1.5, 2.5, 3.5],",
         "  \"baz\": [1, 2, 3.5]",
         "}"));

      assertIterableEquals(IntStream.of(1, 2, 3).mapToObj(i -> (long) i).collect(Collectors.toList()),
         pmap.getLongList("foo", Collections.emptyList()));
      assertIterableEquals(DoubleStream.of(1.5, 2.5, 3.5).mapToObj(f -> f).collect(Collectors.toList()),
         pmap.getDoubleList("bar", Collections.emptyList()));
      assertIterableEquals(DoubleStream.of(1.0, 2.0, 3.5).mapToObj(f -> f).collect(Collectors.toList()),
         pmap.getDoubleList("baz", Collections.emptyList()));
   }

   @Test
   public void testScalarSerialization() {
      PropertyMap pmap = PropertyMaps.builder().
         putString("foo", "FOO").
         putInteger("integer", 123).
         putFloat("floatingpoint", 0.5f).
         putBoolean("bar", true).
         build();
      String json = MM1JSONSerializer.toJSON(pmap);

      assertEquals("FOO", JsonPath.read(json, "$.foo"));
      assertEquals(Integer.valueOf(123), JsonPath.read(json, "$.integer"));
      assertEquals(Double.valueOf(0.5), JsonPath.read(json, "$.floatingpoint"));
      assertEquals(Boolean.TRUE, JsonPath.read(json, "$.bar"));
   }

   @Test
   public void testArraySerialization() {
      PropertyMap pmap = PropertyMaps.builder().
         putStringList("foo", Stream.of("a", "b", "c").collect(Collectors.toList())).
         putIntegerList("bar", IntStream.of(1, 2, 3).toArray()).
         build();
      String json = MM1JSONSerializer.toJSON(pmap);

      assertIterableEquals(Stream.of("a", "b", "c").collect(Collectors.toList()),
         JsonPath.read(json, "$.foo"));
      assertIterableEquals(IntStream.of(1, 2, 3).mapToObj(i -> i).collect(Collectors.toList()),
         JsonPath.read(json, "$.bar"));
   }
}
