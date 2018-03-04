package org.micromanager.display.internal.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Mark A. Tsuchida
 */
class PNGExportStrategy extends AbstractFileExportStrategy {
   public static ExportStrategy create(File path) {
      return new PNGExportStrategy(path);
   }

   private PNGExportStrategy(File path) {
      super(path, "png", ImageIO.getImageWritersByFormatName("png").next(),
            null);
   }
}