
package edu.ucsf.valelab.gaussianfit.datasetdisplay;

/**
 *
 * @author nico
 */
public class Square {

   int x_, y_, width_;

   public Square(int x, int y, int width) {
      x_ = x;
      y_ = y;
      width_ = width;
   }

   public int getX() {
      return x_;
   }

   public int getY() {
      return y_;
   }

   public int getWidth() {
      return width_;
   }
}
