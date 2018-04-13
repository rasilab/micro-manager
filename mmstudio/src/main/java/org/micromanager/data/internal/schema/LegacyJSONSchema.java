package org.micromanager.data.internal.schema;

import org.micromanager.data.internal.PropertyKey;

import java.util.Collection;

/**
 * Represent required and optional keys in property-map-like data.
 *
 * This is not a schema in the usual sense (JSON or XML schema),
 * but rather a simple list of PropertyKey items, which themselves
 * define part of the actual data to be read or written.
 */
interface LegacyJSONSchema {
   Collection<PropertyKey> getRequiredInputKeys();
   Collection<PropertyKey> getOptionalInputKeys();
   Collection<PropertyKey> getOutputKeys();
}
