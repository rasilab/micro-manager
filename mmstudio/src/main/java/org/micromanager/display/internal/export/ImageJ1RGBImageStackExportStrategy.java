package org.micromanager.display.internal.export;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Pattern;
import org.micromanager.data.Coords;

/**
 *
 * @author Mark A. Tsuchida
 */
class ImageJ1RGBImageStackExportStrategy implements ExportStrategy {
   private final String name_;
   private ImageStack stack_; // Create upon receiving first image

   public static ExportStrategy create(String name) {
      return new ImageJ1RGBImageStackExportStrategy(name);
   }

   private ImageJ1RGBImageStackExportStrategy(String name) {
      name_ = name;
   }

   @Override
   public boolean willPotentiallyOverwriteFiles(String pattern) {
      return false;
   }

   @Override
   public void write(String filename, Coords coords, BufferedImage image)
         throws IOException
   {
      if (stack_ == null) {
         stack_ = new ImageStack(image.getWidth(), image.getHeight());
      }
      stack_.addSlice(new ColorProcessor(image));
   }

   @Override
   public void close() throws IOException {
      // ImagePlus throws if given an empty stack
      if (stack_.getSize() > 0) {
         try {
            ImagePlus iplus = new ImagePlus(name_, stack_);
            iplus.changes = true; // Prompt user to save on close
            iplus.show();
         }
         catch (Exception e) {
            throw new IOException("Could not create ImageJ image", e);
         }
      }
      stack_ = null;
   }
}