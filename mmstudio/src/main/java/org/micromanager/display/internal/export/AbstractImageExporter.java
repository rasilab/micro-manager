package org.micromanager.display.internal.export;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.awt.image.BufferedImage;
import java.beans.ExceptionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import org.micromanager.data.Coordinates;
import org.micromanager.data.Coords;
import org.micromanager.display.ImageExporter;
import org.micromanager.internal.utils.ReportingUtils;


// This may later be moved into the API, if viewers other than DisplayWindow
// have a use for it. However, we should also think twice before exposing
// ListenableFuture to the API (what is a better way?).
/**
 * Common base implementation for {@code ImageExporter}.
 *
 * @author Mark A. Tsuchida
 */
public abstract class AbstractImageExporter implements ImageExporter {
   private final ExportStrategy exportStrategy_;
   private final String filenamePrefix_;
   private final ImmutableList<Looper> loopers_;


   private static class Looper {
      private final String axis_;
      private final int start_;
      private final int stop_;

      static Looper create(String axis, int start, int stop) {
         return new Looper(axis, start, stop);
      }

      private Looper(String axis, int start, int stop) {
         axis_ = axis;
         start_ = start;
         stop_ = stop;
      }

      long size() {
         return stop_ - start_;
      }

      Stream<Coords> coordsStream(Coords base, List<Looper> inners) {
         Coords.Builder b = base.copyBuilder();
         Stream<Coords> stream = IntStream.range(start_, stop_).
               mapToObj(i -> b.index(axis_, i).build());
         if (inners.isEmpty()) {
            return stream;
         }
         Looper next = inners.get(0);
         List<Looper> rest = inners.subList(1, inners.size());
         return stream.flatMap(coords -> next.coordsStream(coords, rest));
      }
   }


   public static abstract class Builder implements ImageExporter.Builder {
      private Destination dest_ = Destination.IMAGEJ_RGB_STACK;
      private float jpegQuality_ = 1.0f;
      private String filenamePrefix_ = "exported_";
      private File path_;
      private final List<Looper> loopers_ = new ArrayList<>();

      @Override
      public Builder output(Destination dest) {
         Preconditions.checkNotNull(dest);
         dest_ = dest;
         return this;
      }

      @Override
      public Builder jpegQuality(float quality) {
         Preconditions.checkArgument(0.0f <= quality && quality <= 1.0f,
               "JPEG quality must be in the range 1-10");
         jpegQuality_ = quality;
         return this;
      }

      @Override
      public Builder filenamePrefix(String prefix) {
         filenamePrefix_ = (prefix == null) ? "" : prefix;
         return this;
      }

      @Override
      public Builder outputPath(File path) {
         path_ = path;
         return this;
      }

      @Override
      public Builder loop(String axis, int start, int stop) {
         loopers_.add(Looper.create(axis, start, stop));
         return this;
      }
   }


   protected AbstractImageExporter(Builder builder) {
      switch (builder.dest_) {
         case PNG_FILES:
            exportStrategy_ = PNGExportStrategy.create(builder.path_);
            break;
         case JPEG_FILES:
            exportStrategy_ = JPEGExportStrategy.create(builder.path_,
                  builder.jpegQuality_);
            break;
         case IMAGEJ_RGB_STACK:
            exportStrategy_ =
                  ImageJ1RGBImageStackExportStrategy.create(
                        builder.filenamePrefix_);
            break;
         default:
            throw new AssertionError(builder.dest_.name());
      }
      filenamePrefix_ = builder.filenamePrefix_;
      loopers_ = ImmutableList.copyOf(builder.loopers_);
   }


   @Override
   public void close() throws IOException {
      if (exportStrategy_ != null) {
         exportStrategy_.close();
      }
   }

   private boolean isSingleFrameExport() {
      // We will be exporting a single frame if the dataset has no axes at
      // all, or if a length-1 interval of a single axis was requested.
      return loopers_.isEmpty() ||
            (loopers_.size() == 1 && loopers_.get(0).size() == 1);
   }

   private static final int FILENAME_NUMBER_LEN = 6;
   private String makeFilename(long i) {
      if (isSingleFrameExport()) {
         return filenamePrefix_;
      };
      String format = String.format("%%s_%%0%dd", FILENAME_NUMBER_LEN);
      return String.format(format, filenamePrefix_, i);
   }

   @Override
   public boolean willPotentiallyOverwriteFiles() {
      String regex = Pattern.quote(filenamePrefix_);
      if (!isSingleFrameExport()) {
         regex += String.format("_[0-9]{%d}", FILENAME_NUMBER_LEN);
      }
      return exportStrategy_.willPotentiallyOverwriteFiles(regex);
   }

   @Override
   public void run(Runnable completionHandler, ExceptionListener errorHandler) {
      Preconditions.checkNotNull(completionHandler);
      Preconditions.checkNotNull(errorHandler);

      Iterator<Coords> iter;
      if (loopers_.isEmpty()) {
         iter = Stream.of(Coordinates.emptyCoords()).iterator();
      }
      else {
         iter = loopers_.get(0).coordsStream(
            Coordinates.emptyCoords(),
            loopers_.subList(1, loopers_.size())).iterator();
      }

      // Because we might be on the EDT, and the rendering may occur on the
      // EDT, we cannot simply loop. Instead, we schedule the next frame each
      // time a frame is rendered.
      SwingUtilities.invokeLater(() -> {
         runNext(0, iter, completionHandler, errorHandler);
      });
   }

   private void runNext(int index, Iterator<Coords> iter,
         Runnable completion, ExceptionListener error)
   {
      if (!iter.hasNext()) {
         completion.run();
         return;
      }
      Coords coords = iter.next();
      ListenableFuture<BufferedImage> future = render(coords);
      future.addListener(() -> {
         try {
            BufferedImage image = future.get();
            exportFrame(makeFilename(index), coords, image);
            runNext(index + 1, iter, completion, error);
         }
         catch (IOException e) {
            error.exceptionThrown(e);
         }
         catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
               error.exceptionThrown((Exception) t);
            }
            else {
               throw new RuntimeException(t);
            }
         }
         catch (InterruptedException notUsed) {
         }
      },
            MoreExecutors.directExecutor());
   }

   private void exportFrame(String filename, Coords coords,
         BufferedImage image) throws IOException
   {
      ReportingUtils.logDebugMessage(String.format("Exporting %s to %s", coords, filename));
      exportStrategy_.write(filename, coords, image);
   }

   protected abstract ListenableFuture<BufferedImage> render(Coords coords);
}