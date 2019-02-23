package edu.ucsf.valelab.gaussianfit.datasetdisplay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;
import org.micromanager.data.Image;
import org.micromanager.display.DisplaySettings;
import org.micromanager.display.overlay.AbstractOverlay;

/**
 *
 * @author nico
 */
public class TrackOverlay extends AbstractOverlay {

   Square square;

   @Override
   public String getTitle() {
      return "Track Overlay";
   }

   /**
    * {@inheritDoc}
    * <p>
    * This default implementation draws nothing. Override to draw the overlay
    * graphics.
    */
   @Override
   public void paintOverlay(final Graphics2D g, final Rectangle screenRect,
           DisplaySettings displaySettings,
           List<Image> images, Image primaryImage,
           Rectangle2D.Float imageViewPort) {
      g.setColor(Color.RED);

      final double zoomRatio = screenRect.width / imageViewPort.width;

      Square s = square;
      int zoomedWidth = (int) (zoomRatio * s.getWidth());
      int halfWidth = (int) (0.5 * zoomedWidth);
      int x = (int) (zoomRatio * (s.getX() - imageViewPort.x)) - halfWidth;
      int y = (int) (zoomRatio * (s.getY() - imageViewPort.y)) - halfWidth;
      g.drawRect(x, y, zoomedWidth, zoomedWidth);

   }

   public void setSquare(int x, int y, int width) {
      square = new Square(x, y, width);
   }

   public void clearSquare() {
      square = null;
   }

}
