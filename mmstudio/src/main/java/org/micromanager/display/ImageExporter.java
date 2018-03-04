///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     Display implementation
//-----------------------------------------------------------------------------
//
// AUTHOR:       Chris Weisiger, 2016; Mark Tsuchida, 2018
//
// COPYRIGHT:    (c) 2016-2018 Open Imaging, Inc.
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.display;

import java.beans.ExceptionListener;
import java.io.File;
import java.io.IOException;


/**
 * Export displayed images from a viewer.
 */
public interface ImageExporter extends AutoCloseable {
   /**
    * Allowed export destinations.
    */
   public enum Destination {
      IMAGEJ_RGB_STACK,
      PNG_FILES,
      JPEG_FILES,
   }

   /**
    * Builder for ImageExporter.
    *
    * Typically you will obtain a builder from a {@code DataViewer} that
    * supports export.
    */
   public interface Builder {
      Builder output(Destination dest);
      /**
       * Set JPEG quality.
       * @param quality JPEG quality, 0.0-1.0
       * @return this builder, for chaining
       */
      Builder jpegQuality(float quality);
      Builder filenamePrefix(String prefix);
      Builder outputPath(File path);
      /**
       * Add an axis and range to loop over.
       * @param axis
       * @param start the starting coordinate, inclusive
       * @param stop the stopping coordinate, exclusive
       * @return this builder, for chaining
       */
      Builder loop(String axis, int start, int stop);

      /**
       * Create the {@code ImageExporter}.
       * @return the exporter
       * @throws IllegalArgumentException if this builder does not contain
       * valid settings (e.g. output path missing when exporting to a file)
       */
      ImageExporter build() throws IllegalArgumentException;
   }

   boolean willPotentiallyOverwriteFiles();

   /**
    * Run the export.
    *
    * If the destination is set to files, any existing files with the same
    * names will be overwritten (no error is reported). UI code calling this
    * method is responsible for confirming whether to overwrite.
    * <p>
    * This method will return immediately, and either the completion handler
    * or the error handler will be called later, on the EDT.
    * <p>
    * The user interface may not be accessible while the export is running.
    *
    * @param completionHandler called when the export has finished
    * @param errorHandler called if there is an error ({@code IOException}
    * should be handled)
    */
   void run(Runnable completionHandler, ExceptionListener errorHandler);

   @Override
   void close() throws IOException;
}