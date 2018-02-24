package org.micromanager.display.internal.displaywindow.imagej;

import ij.ImagePlus;
import java.awt.Color;

/**
 * A color mode that doesn't do anything, representing the state where the
 * color mode has not been set.
 *
 * @author mark
 */
class NullColorModeStrategy implements ColorModeStrategy {
   public static ColorModeStrategy create() {
      return new NullColorModeStrategy();
   }

   private NullColorModeStrategy() {
   }

   @Override
   public void attachToImagePlus(ImagePlus imagePlus) {
   }

   @Override
   public void applyHiLoHighlight(boolean enable) {
   }

   @Override
   public void applyColor(int index, Color color) {
   }

   @Override
   public void applyScaling(int index, int min, int max) {
   }

   @Override
   public void applyGamma(int index, double gamma) {
   }

   @Override
   public void applyVisibleInComposite(int index, boolean visible) {
   }

   @Override
   public void displayedImageDidChange() {
   }

   @Override
   public void releaseImagePlus() {
   }
}