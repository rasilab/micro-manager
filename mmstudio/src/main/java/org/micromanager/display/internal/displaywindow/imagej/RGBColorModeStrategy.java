/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.micromanager.display.internal.displaywindow.imagej;

import com.google.common.base.Preconditions;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author mark
 */
class RGBColorModeStrategy implements ColorModeStrategy {
   private ImagePlus imagePlus_;

   private final List<Integer> minima_;
   private final List<Integer> maxima_;

   private final int[][] cachedLUTs_ = new int[3][];
   private ImageProcessor unscaledRGBImage_;

   static ColorModeStrategy create() {
      return new RGBColorModeStrategy();
   }

   private RGBColorModeStrategy() {
      minima_ = new ArrayList<Integer>(Collections.nCopies(3, 0));
      maxima_ = new ArrayList<Integer>(Collections.nCopies(3, 255));
   }

   private int[][] getRGBLUTs() {
      for (int i = 0; i < 3; ++i) {
         if (cachedLUTs_[i] != null) {
            continue;
         }
         cachedLUTs_[i] = new int[256];
         int min = Math.max(0, minima_.get(i));
         int max = Math.min(255, maxima_.get(i));
         if (min == max) {
            if (min == 0) {
               ++max;
            }
            else {
               --min;
            }
         }
         for (int k = 0; k < 256; ++k) {
            cachedLUTs_[i][k] = Math.max(0, Math.min(255,
                  255 * (k - min) / (max - min)));
         }
      }
      return cachedLUTs_;
   }

   private void apply() {
      // ColorProcessor has no way of applying a LUT, so we modify the pixel
      // values directly, keeping a copy of the original image.
      // (We previously used a trick and stored the original image in the
      // ColorProcessor's "snapshot", but that leaks internal behavior if the
      // user should select Edit > Undo.)
      int[][] luts = getRGBLUTs();
      if (unscaledRGBImage_ == null) {
         unscaledRGBImage_ = imagePlus_.getProcessor();
      }
      ColorProcessor scaled = (ColorProcessor) unscaledRGBImage_.duplicate();
      for (int i = 0; i < 3; ++i) {
         scaled.applyTable(luts[i], 1 << (2 - i));
      }
      imagePlus_.setProcessor(scaled);
   }

   @Override
   public boolean isRGBStrategy() {
      return true;
   }

   @Override
   public void attachToImagePlus(ImagePlus imagePlus) {
      Preconditions.checkArgument(!(imagePlus instanceof CompositeImage));
      Preconditions.checkArgument(
            imagePlus.getProcessor() instanceof ColorProcessor);

      unscaledRGBImage_ = null;
      imagePlus_ = imagePlus;
      apply();
   }

   @Override
   public void applyHiLoHighlight(boolean enable) {
      // Not supported
   }

   @Override
   public void applyColor(int component, Color color) {
   }

   @Override
   public void applyScaling(int component, int min, int max) {
      Preconditions.checkArgument(min >= 0);
      Preconditions.checkArgument(max >= min);
      if (min == minima_.get(component) && max == maxima_.get(component)) {
         return;
      }
      minima_.set(component, min);
      maxima_.set(component, max);
      cachedLUTs_[component] = null; // invalidate
      apply();
   }

   @Override
   public void applyGamma(int component, double gamma) {
      if (gamma != 1.0) {
         throw new UnsupportedOperationException("Gamma is not supported for RGB");
      }
   }

   @Override
   public void applyVisibleInComposite(int component, boolean visible) {
   }

   @Override
   public void displayedImageDidChange() {
      unscaledRGBImage_ = null;
      apply();
   }

   @Override
   public void releaseImagePlus() {
      imagePlus_ = null;
      for (int i = 0; i < 3; ++i) {
         cachedLUTs_[i] = null;
      }
      unscaledRGBImage_ = null;
   }
}