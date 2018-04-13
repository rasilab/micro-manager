package org.micromanager.data.internal.schema;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.micromanager.PropertyMap;
import org.micromanager.PropertyMaps;
import org.micromanager.data.internal.PropertyKey;

import java.io.IOException;
import java.io.StringReader;

final class LegacyJSONSchemaSerializer {
   private LegacyJSONSchemaSerializer() {}

   static PropertyMap fromJSON(String json, LegacyJSONSchema schema) throws IOException {
      try {
         JsonReader reader = new JsonReader(new StringReader(json));
         reader.setLenient(true);
         JsonParser parser = new JsonParser();
         return fromGson(parser.parse(reader).getAsJsonObject(), schema);
      }
      catch (JsonParseException | IllegalStateException e) {
         throw new IOException("Invalid data", e);
      }
   }

   static String toJSON(PropertyMap pmap, LegacyJSONSchema schema) {
      Gson gson = new GsonBuilder().
         disableHtmlEscaping().
         setPrettyPrinting().
         create();
      return gson.toJson(toGson(pmap, schema));
   }

   static PropertyMap fromGson(JsonElement je, LegacyJSONSchema schema) {
      PropertyMap.Builder builder = PropertyMaps.builder();

      for (PropertyKey key : schema.getRequiredInputKeys()) {
         key.extractFromGsonObject(je.getAsJsonObject(), builder);
      }

      for (PropertyKey key : schema.getOptionalInputKeys()) {
         key.extractFromGsonObject(je.getAsJsonObject(), builder);
      }

      return builder.build();
   }

   static JsonElement toGson(PropertyMap pmap, LegacyJSONSchema schema) {
      JsonObject jo = new JsonObject();
      addToGson(jo, pmap, schema);
      return jo;
   }

   static void addToGson(JsonObject jo, PropertyMap pmap, LegacyJSONSchema schema) {
      for (PropertyKey key : schema.getOutputKeys()) {
         key.storeInGsonObject(pmap, jo);
      }
   }
}
