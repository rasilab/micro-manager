///////////////////////////////////////////////////////////////////////////////
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nico Stuurman, 2018
//
// COPYRIGHT:    Regents of the University of California, 2018
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

package org.micromanager.internal.pixelcalibrator;

import com.google.common.eventbus.Subscribe;
import edu.ucsf.valelab.gaussianfit.utils.NumberUtils;
import edu.ucsf.valelab.gaussianfit.utils.ReportingUtils;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import mmcorej.CMMCore;
import net.miginfocom.swing.MigLayout;
import org.micromanager.Studio;
import org.micromanager.display.DisplayWindow;
import org.micromanager.display.internal.displaywindow.DisplayController;
import org.micromanager.display.internal.event.DisplayMouseEvent;
import org.micromanager.internal.utils.AffineUtils;
import org.micromanager.internal.utils.MMFrame;

/**
 *
 * @author NicoLocal
 */
public class ManualSimpleCalibrationThread extends CalibrationThread {
   private final Studio studio_;
   private final CMMCore core_;
   private final PixelCalibratorDialog dialog_;
   private DialogFrame dialogFrame_;
   private DisplayController dc_;
   
   private final JLabel explanationLabel_;
   private Point2D.Double initialStagePosition_;
   private final int nrPoints_;
   private final Point2D[] points_;
   
   private int counter_;

   

   ManualSimpleCalibrationThread(Studio studio, PixelCalibratorDialog dialog) {
      studio_ = studio;
      core_ = studio_.getCMMCore();
      dialog_ = dialog;
      explanationLabel_ = new JLabel();
      nrPoints_ = 3;
      points_ = new Point2D[nrPoints_];
   }

   @Override
   public void run() {
      synchronized (this) {
         progress_ = 0;
      }
      result_ = null;
      counter_ = 0;
      
      try {
         initialStagePosition_ = core_.getXYStagePosition();
      } catch (Exception ex) {
         ReportingUtils.showError("Failed to set stage position. Can not calibrate without working stage");
         dialog_.calibrationFailed(true);
         return;
      }

      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            dialogFrame_  = new DialogFrame(this);
         }
      });

      DisplayWindow display = studio_.live().getDisplay();
      if (display == null) {
         studio_.live().snap(true);
         long waitUntil = System.currentTimeMillis() + 5000;
         display = studio_.live().getDisplay();
         while (display == null && System.currentTimeMillis() < waitUntil) {
            try {
               display = studio_.live().getDisplay();
               Thread.sleep(100);
            } catch (InterruptedException ex) {
               
            }
         }
         if (display == null) {
            ReportingUtils.showError("Preview window did not open. Is the exposure time very long?");
            dialogFrame_.dispose();
         } else if (display instanceof DisplayController) {
            dc_ = (DisplayController) display;
            dc_.registerForEvents(this);
         }
      }
   }
   
   @Subscribe
   public void processMouseEvent(DisplayMouseEvent dme) {
      if (dme.getEvent().getClickCount() == 1 && dme.getEvent().getButton() == 1) {
         int modifiersEx = dme.getEvent().getModifiersEx();
         boolean pressed  = InputEvent.BUTTON1_DOWN_MASK == (modifiersEx & InputEvent.BUTTON1_DOWN_MASK);
         if (pressed) {
            points_[counter_] = dme.getCenterLocation();
            double d = dialog_.getCalibratedPixelSize();
            String label1Text = "";
            try {
               int minSize = (int) Math.min(
                       core_.getImageWidth(), core_.getImageHeight());
               int nrPixels = minSize / 4;
               switch (counter_) {
                  case 0:
                     points_[counter_] = dme.getCenterLocation();
                     core_.setRelativeXYPosition(d * nrPixels, 0.0);
                     label1Text = "<html>Perfect!  The stage was moved " + d * nrPixels
                             + " microns along the x axis.<br>" + " Click on the same object";
                     break;
                  case 1:
                     points_[counter_] = dme.getCenterLocation();
                     core_.setRelativeXYPosition(-d * nrPixels, d * nrPixels);
                     label1Text = "<html>Nice!  The stage was moved " + d * nrPixels
                             + " microns along the y axis.<br>" + " Click on the same object";
                     break;
                  case 2:
                     points_[counter_] = dme.getCenterLocation();
                     core_.setRelativeXYPosition(0, -d * nrPixels);
                     // Done!  now calculate affine transform, and ask the user
                     // if OK.
                     super.result_ = calculateAffineTransform(d, points_);
                     if (result_ == null) {
                        label1Text = "<html>Could not figure out orientation. <br>"
                             + "Try again?";
                        if (dialogFrame_ != null) {
                           dialogFrame_.setOKButtonVisible(true);
                        }
                     } else {
                        dialog_.calibrationDone();
                        return;
                     }
                     
                     break;
               }
               counter_++;

               studio_.live().snap(true);

               if (dialogFrame_ != null) {

                  dialogFrame_.setLabelText(label1Text);
               }
            } catch (Exception ex) {

            }
         }
      }
   }
   
   private AffineTransform calculateAffineTransform(double pixelSize, 
           Point2D[] points) {
      AffineTransform at = AffineUtils.doubleToAffine(AffineUtils.noTransform());
      boolean rotate = Math.abs(points[1].getX() - points[0].getX()) < 
              Math.abs(points[1].getY() - points[0].getY() );
      // sanity check for rotate
      if (! ( rotate == Math.abs(points[2].getY() - points[0].getY()) < 
              Math.abs(points[2].getX() - points[2].getX() ) ) ) {
         return null;
      }
      int xDirection = 1;
      int yDirection = 1;
      if (!rotate) {
         if (points[1].getX() < points[0].getX()) {
            xDirection = -1;
         }
         if (points[2].getY() < points[0].getY()) {
            yDirection = -1;
         }
      } else {
         if (points[1].getX() < points[0].getX()) {
            yDirection = -1;
         }
         if (points[2].getY() < points[0].getY()) {
            xDirection = -1;
         }
      }
      
      at.scale(xDirection * pixelSize, yDirection * pixelSize);
      at.rotate(Math.PI * 0.5);
            
      return at;
   }

   private class DialogFrame extends MMFrame {
      private final Object caller_;
      private final JButton okButton_;
      
      public DialogFrame(Object caller) {
         caller_ = caller;
         super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         super.setLayout(new MigLayout());
         String label1Text = "<html>This method creates an affine transform based on"
                 + " a <br>pixelSize of "
                 + NumberUtils.doubleToDisplayString(dialog_.getCalibratedPixelSize(), 4)
                 + " &mu; per pixel.  If this is not "
                 + "correct, <br>please cancel and first set the correct pixelSize.<br><br>"
                 + "Focus the image in the Preview window and use the <br>mouse pointer to click "
                 + "on an object somehwere <br>near the center of the image.";
         explanationLabel_.setText(label1Text);
         super.add(explanationLabel_, "span 2, wrap");
         
         okButton_ = new JButton("OK");
         okButton_.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae){
               counter_ = 0;
               okButton_.setVisible(false);
            }
         });
         okButton_.setVisible(false);
         super.add(okButton_, "tag ok");
         
         JButton cancelButton = new JButton("Cancel");
         cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               dispose();
            }
         });
         super.add(cancelButton, "tag cancel, wrap");
         super.pack();
         super.loadAndRestorePosition(200, 200);
         super.setVisible(true);
      }
      
      public void setLabelText(String newText) {
         if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
               @Override
               public void run() {
                  setLabelText(newText);
               }
             });
         }
         explanationLabel_.setText(newText);
      }

      @Override
      public void dispose() {
         super.dispose();
         if (dc_ != null) {
            dc_.unregisterForEvents(caller_);
         }
         dialog_.calibrationFailed(true);
      }
      
      public void setOKButtonVisible(boolean visible) {
         okButton_.setVisible(visible);
      }

   }

   private synchronized void incrementProgress() {
      progress_++;
      dialog_.update();
   }

   synchronized void setProgress(int value) {
      progress_ = value;
   }
   
   private class CalibrationFailedException extends Exception {

      private static final long serialVersionUID = 4749723616733251885L;

      public CalibrationFailedException(String msg) {
         super(msg);
         if (initialStagePosition_ != null) {
            try {
               core_.setXYPosition( initialStagePosition_.x, initialStagePosition_.y);
               studio_.live().snap(true);
            } catch (Exception ex) {
               // annoying but at this point better to not bother the user 
               // with failure after failure
            }
         }

      }
   }
}