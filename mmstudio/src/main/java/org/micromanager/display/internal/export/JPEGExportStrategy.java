package org.micromanager.display.internal.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import static javax.imageio.ImageWriteParam.MODE_DEFAULT;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 *
 * @author Mark A. Tsuchida
 */
class JPEGExportStrategy extends AbstractFileExportStrategy {
   public static ExportStrategy create(File path, float quality) {
      return new JPEGExportStrategy(path, quality);
   }

   public static ExportStrategy create(File path) {
      return create(path, 0.0f);
   }

   private JPEGExportStrategy(File path, float quality) {
      super(path, "jpg", ImageIO.getImageWritersByFormatName("jpeg").next(),
            makeParam(quality));
   }

   private static ImageWriteParam makeParam(float quality) {
      ImageWriteParam param = ImageIO.getImageWritersByFormatName("jpeg").
            next().getDefaultWriteParam();
      param.setCompressionMode(quality == 0.0f ? MODE_DEFAULT : MODE_EXPLICIT);
      param.setCompressionQuality(quality);
      return param;
   }
}