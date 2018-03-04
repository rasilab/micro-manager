package org.micromanager.display.internal.export;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Pattern;
import org.micromanager.data.Coords;

/**
 * Pluggable strategies for exporting images.
 * @author Mark A. Tsuchida
 */
interface ExportStrategy extends AutoCloseable {
   boolean willPotentiallyOverwriteFiles(String filenameRegEx);

   void write(String filename, Coords coords, BufferedImage image)
         throws IOException;

   @Override
   void close() throws IOException;
}