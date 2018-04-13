package org.micromanager.data.internal.schema;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.micromanager.PropertyMap;
import org.micromanager.data.internal.PropertyKey;

import java.io.IOException;
import java.util.Collection;

/**
 * Represent required and optional keys in property-map-like data.
 *
 * This is not a schema in the usual sense (JSON or XML schema),
 * but rather a simple list of PropertyKey items, which themselves
 * define part of the actual data to be read or written.
 */
public interface LegacyJSONSchema {
   Collection<PropertyKey> getRequiredInputKeys();
   Collection<PropertyKey> getOptionalInputKeys();
   Collection<PropertyKey> getOutputKeys();

   default PropertyMap fromJSON(String json) throws IOException {
      return LegacyJSONSchemaSerializer.fromJSON(json, this);
   }

   default String toJSON(PropertyMap pmap) {
      return LegacyJSONSchemaSerializer.toJSON(pmap, this);
   }

   default PropertyMap fromGson(JsonElement je) {
      return LegacyJSONSchemaSerializer.fromGson(je, this);
   }

   default JsonElement toGson(PropertyMap pmap) {
      return LegacyJSONSchemaSerializer.toGson(pmap, this);
   }

   default void addToGson(JsonObject jo, PropertyMap pmap) {
      LegacyJSONSchemaSerializer.addToGson(jo, pmap, this);
   }
}
