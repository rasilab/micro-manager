/*
Copyright (c) 2010-2017, Regents of the University of California
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies,
either expressed or implied, of the FreeBSD Project.
 */

package edu.ucsf.valelab.gaussianfit.datasettransformations;

import edu.ucsf.valelab.gaussianfit.DataCollectionForm;
import edu.ucsf.valelab.gaussianfit.data.RowData;
import edu.ucsf.valelab.gaussianfit.data.SpotData;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPoint2D;
import edu.ucsf.valelab.gaussianfit.utils.ListUtils;
import edu.ucsf.valelab.gaussianfit.utils.ReportingUtils;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author nico
 */
public class PairFilter {
   
   public static AtomicBoolean isRunning_ = new AtomicBoolean(false);
   
   public static void resetLock() {
      isRunning_.set(false);
   }

   /**
    * Creates a new data set that only contains spot pairs that match our
    * criteria.
    * The filter finds pairs of a spot in channel 1 and 2 (in the same frame and
    * position) within a distance maxdistance.  It then divides the image in
    * n quadrants (where n is a square of an integer), and calculates the 
    * average distance between the spots within this quadrant. Pairs with 
    * a distance that is greater than deviationMax * the standard deviation 
    * away from the mean, are rejected, all others make it into the output.
    *
    *
    * @param rowData - input source of data
    * @param maxDistance - maximum distance of separation between spots in two
    * channels for them to be considered a pair
    * @param deviationMax - maximum deviation from the mean (expressed in
    * standard deviations) above which a pair is rejected
    * @param nrQuadrants - Number of quadrants in which the image should be
    * divided (i.e., each quadrant will be filtered by itself) Valid numbers are
    * 1, and squares of integers (i.e. 4, 9, 16).
    *
    */
   public static void filter(final RowData rowData, final double maxDistance,
           final double deviationMax, final int nrQuadrants) {

      if (rowData.spotList_.size() <= 1) {
         return;
      }

      final int sqrtNrQuadrants = (int) Math.sqrt(nrQuadrants);
      if (nrQuadrants != (sqrtNrQuadrants * sqrtNrQuadrants)) {
         ReportingUtils.showError("nrQuadrants should be a square of an integer");
         return;
      }

      final double qSize = rowData.width_ * rowData.pixelSizeNm_ / sqrtNrQuadrants;

      ij.IJ.showStatus("Executing pair selection");

      Runnable doWorkRunnable = new Runnable() {

         @Override
         public void run() {
            try {
               List<SpotData> correctedData = new ArrayList<>();
               final int nrChannels = rowData.nrChannels_;

               for (int frame = 1; frame <= rowData.nrFrames_; frame++) {
                  ij.IJ.showProgress(frame, rowData.nrFrames_);

                  // Get points from both channels in each frame as ArrayLists 
                  // split last channel into the nrQuadrants
                  Map<Integer, List<SpotData>> gsChLast = new HashMap<>(nrQuadrants);
                  for (int q = 0; q < nrQuadrants; q++) {
                     gsChLast.put(q, new ArrayList<>());
                  }
                  // index 1 to last - 1 by position
                  // channel - position - List of Points or spots
                  // Note that channel is zero-based in these data structures
                  List<List<List<Point2D.Double>>> xyPointsF = 
                          new ArrayList<>(nrChannels - 1);
                  List<List<List<SpotData>>> xySpotsF = 
                          new ArrayList<>(nrChannels - 1);
                  for (int c = 1; c < nrChannels; c++) {
                     xyPointsF.add(new ArrayList<>(rowData.nrPositions_));
                     xySpotsF.add(new ArrayList<>(rowData.nrPositions_));
                     for (int position = 1; position <= rowData.nrPositions_; position++) {
                        xyPointsF.get(c - 1).add(position - 1, new ArrayList<>());
                        xySpotsF.get(c - 1).add(position - 1, new ArrayList<>());
                     }
                  }
                  /*
                  ArrayList<List<Point2D.Double>> xyPointsCh2 = 
                          new ArrayList<List<Point2D.Double>>(rowData.nrPositions_);
                  ArrayList<List<SpotData>> xySpotsCh2 = 
                          new ArrayList<List<SpotData>>(rowData.nrPositions_);
                  for (int position = 1; position <= rowData.nrPositions_; position++) {
                     xyPointsCh2.add(position - 1, new ArrayList<Point2D.Double>());
                     xySpotsCh2.add(position - 1, new ArrayList<SpotData>());
                  }
                  */

                  for (SpotData gs : rowData.spotList_) {
                     if (gs.getFrame() == frame) {
                        if (gs.getChannel() == nrChannels) {
                           int yOffset = (int) Math.floor(gs.getYCenter() / qSize);
                           int xOffset = (int) Math.floor(gs.getXCenter() / qSize);
                           int q = yOffset * sqrtNrQuadrants + xOffset;
                           if (q >= 0 && q < nrQuadrants) {
                              gsChLast.get(q).add(gs);
                           }
                        } else if (gs.getChannel() < nrChannels) {
                           xySpotsF.get(gs.getChannel() - 1).get(gs.getPosition() - 1).add(gs);
                           Point2D.Double point = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
                           xyPointsF.get(gs.getChannel() - 1).get(gs.getPosition() - 1).add(point);
                        }
                     }
                  }

                  for (int c = 1; c < nrChannels; c++) {
                     if (xyPointsF.get(c - 1).isEmpty()) {
                        ReportingUtils.logError("Pairs function in Localization plugin: no points found in second channel in frame " + frame);
                        continue;
                     }
                  }

                  // we have the points of the last channel in each quadrant
                  // find matching pairs for each previous channel, 
                  // and do statistics on each quadrant
                  // only keep pairs that match what was requested
                  
                  // First set up the nearestPoint maps for all channels and positions
                  List<List<NearestPoint2D>> npsByChannelAndPosition = 
                          new ArrayList<>(nrChannels - 1);
                  for (int c = 1; c < nrChannels; c++) {
                     npsByChannelAndPosition.add(c - 1, new ArrayList<>(rowData.nrPositions_));
                     for (int position = 1; position <= rowData.nrPositions_; position++) {
                        npsByChannelAndPosition.get(c - 1).add(position - 1, 
                             new NearestPoint2D(xyPointsF.get(c - 1).get(position - 1), maxDistance));
                     }
                  }
                  
                  // go through the quadrants and find pairs in channel 1 to last - 1
                  for (int q = 0; q < nrQuadrants; q++) {
                     ij.IJ.showProgress( (q+1) * frame, (nrQuadrants + 1) * rowData.nrFrames_);
                     // Find matching points between all first channels and the last
                     Iterator it2 = gsChLast.get(q).iterator();
                     List<List<Double>> distances = new ArrayList<>();
                     List<List<Double>> orientations = new ArrayList<>();
                     for (int c = 1; c < nrChannels; c++) {
                        distances.add(new ArrayList<>());
                        orientations.add(new ArrayList<>());
                     }

                     while (it2.hasNext()) {
                        SpotData gs = (SpotData) it2.next();
                        Point2D.Double pChLast = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
                        for (int c = 1; c < nrChannels; c++) {
                           Point2D.Double pChF = npsByChannelAndPosition.
                                   get(c - 1).get(gs.getPosition() - 1).
                                   findKDWSE(pChLast);
                           if (pChF != null) {
                              double d2 = NearestPoint2D.distance2(pChLast, pChF);
                              double d = Math.sqrt(d2);
                              distances.get(c - 1).add(d);
                              orientations.get(c - 1).add(NearestPoint2D.orientation(pChLast, pChF));
                           }
                        }
                     }
                     List<Double> distAvg = new ArrayList<>(distances.size());
                     List<Double> distStd = new ArrayList<>(distances.size());
                     List<Double> orientationAvg = new ArrayList<>(orientations.size());
                     for (int c = 1; c < nrChannels; c++) {
                        distAvg.add(c - 1, ListUtils.listAvg(distances.get(c - 1)));
                        distStd.add(c - 1, ListUtils.listStdDev(distances.get(c - 1), distAvg.get(c - 1)));
                        orientationAvg.add(ListUtils.listAvg(orientations.get(c - 1)));
                     }

                     // now repeat going through the list and apply the criteria
                     it2 = gsChLast.get(q).iterator();
                     while (it2.hasNext()) {
                        SpotData gs = (SpotData) it2.next();
                        Point2D.Double pChLast = new Point2D.Double(gs.getXCenter(), gs.getYCenter());
                        boolean foundAll = true;
                        List<Point2D.Double> pChF = new ArrayList<>(nrChannels - 1);
                        for (int c = 1; c < nrChannels && foundAll; c++) {
                           Point2D.Double pCh = 
                                npsByChannelAndPosition.get(c - 1).
                                        get(gs.getPosition() - 1).findKDWSE(pChLast);
                           if (pCh != null) {
                              pChF.add(c - 1, pCh);
                           } else {
                              foundAll = false;
                           }
                        }
                        if (foundAll) {
                           boolean qualifies = true;
                           for (int c = 1; c < nrChannels; c++) {
                              double d2 = NearestPoint2D.distance2(pChLast, pChF.get(c - 1));
                              double d = Math.sqrt(d2);
                              // we can possibly add the same criterium for orientation
                              if (! (d > distAvg.get(c - 1) - deviationMax * distStd.get(c - 1)
                                   && d < distAvg.get(c - 1) + deviationMax * distStd.get(c - 1))) {
                                 qualifies = false;
                              }
                           }
                           if (qualifies) {
                              correctedData.add(gs);
                              // we have to find the matching spots in the other channels
                              for (int c = 1; c < nrChannels; c++) {
                                 for (SpotData gsChF : xySpotsF.get(c - 1).get(gs.getPosition() - 1)) {
                                    if (gsChF.getFrame() == frame) {
                                       if (gsChF.getXCenter() == pChF.get(c - 1).x
                                               && gsChF.getYCenter() == pChF.get(c - 1).y) {
                                          correctedData.add(gsChF);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               // Add transformed data to data overview window
               RowData.Builder builder = rowData.copy();
               builder.setName(rowData.getName() + "-Pair-Corrected").
                       setSpotList(correctedData);
               DataCollectionForm.getInstance().addSpotData(builder);

               ij.IJ.showStatus("Finished pair correction");
            } catch (OutOfMemoryError oom) {
               System.gc();
               ij.IJ.error("Out of Memory");
            }
            isRunning_.set(false);
         }
      };

      if (!isRunning_.get()) {
         isRunning_.set(true);
         (new Thread(doWorkRunnable)).start();
      } else {
         
         // TODO: let the user know
      }
   }

}
