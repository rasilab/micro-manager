package org.micromanager.display.internal.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.micromanager.data.Coords;

/**
 *
 * @author Mark A. Tsuchida
 */
abstract class AbstractFileExportStrategy implements ExportStrategy {
   private final File path_;
   private final String extension_;
   private final ImageWriter writer_;
   private final ImageWriteParam param_;

   protected AbstractFileExportStrategy(File path, String extension,
         ImageWriter writer, ImageWriteParam writeParam)
   {
      path_ = path;
      extension_ = extension;
      writer_ = writer;
      param_ = writeParam;
   }

   @Override
   public boolean willPotentiallyOverwriteFiles(String filenameRegex) {
      if (!path_.exists()) {
         return false;
      }
      if (!path_.isDirectory()) {
         // If our "directory" exists but is not a directory, writing will
         // fail -- but that's different from overwriting. When invoked from
         // the UI, the user would only have been allowed to select
         // directories. So do the logically correct thing here.
         return false;
      }
      // Path exists and is a directory.
      // Although at least on Linux the filesystem can be case sensitive, we
      // do a conservative check since we are testing for "potential".
      Pattern pattern = Pattern.compile(filenameRegex + Pattern.quote("." +
            extension_), Pattern.CASE_INSENSITIVE);
      return path_.listFiles((dir, name) -> pattern.matcher(name).matches()).
            length > 0;
   }

   @Override
   public void write(String filename, Coords coords, BufferedImage image)
         throws IOException
   {
      File dest = new File(path_, filename + "." + extension_);
      try (ImageOutputStream stream = ImageIO.createImageOutputStream(dest)) {
         writer_.setOutput(stream);
         writer_.write(null, new IIOImage(image, null, null), param_);
      }
   }

   @Override
   public void close() {
      writer_.dispose();
   }
}