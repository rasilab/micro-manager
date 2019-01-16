/*
 * Copyright (c) 2015-2017, Regents the University of California
 * Author: Nico Stuurman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.ucsf.valelab.gaussianfit.datasetdisplay;

import edu.ucsf.valelab.gaussianfit.DataCollectionForm;
import edu.ucsf.valelab.gaussianfit.ResultsTableListener;
import edu.ucsf.valelab.gaussianfit.Terms;
import edu.ucsf.valelab.gaussianfit.data.GsSpotPair;
import edu.ucsf.valelab.gaussianfit.data.SpotData;
import edu.ucsf.valelab.gaussianfit.data.SpotsByPosition;
import edu.ucsf.valelab.gaussianfit.fitting.FittingException;
import edu.ucsf.valelab.gaussianfit.fitting.Gaussian1DecdFitter;
import edu.ucsf.valelab.gaussianfit.fitting.P2DEcdfFitter;
import edu.ucsf.valelab.gaussianfit.fitting.P2DFitter;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPoint2D;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPointByData;
import edu.ucsf.valelab.gaussianfit.utils.GaussianUtils;
import edu.ucsf.valelab.gaussianfit.utils.ListUtils;
import edu.ucsf.valelab.gaussianfit.utils.NumberUtils;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Arrow;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.micromanager.internal.MMStudio;
import org.micromanager.internal.utils.ReportingUtils;

/**
 *
 * @author nico
 */
public class ParticlePairLister {

   final private int[] rows_;
   final private Double maxDistanceNm_; //maximum distance in nm for two spots in different
                                        // channels to be considered a pair
   final private Boolean showPairs_;
   final private Boolean showSummary_;
   final private Boolean showOverlay_;
   final private Boolean showXYHistogram_;
   final private Boolean p2dDistanceCalc_;
   final private Boolean p2dSingleFrames_;
   final private Double registrationError_; 
   final private Boolean showHistogram_;
   final private Boolean bootstrap_;
   

   public static class Builder {

      private int[] rows_;
      private Double maxDistanceNm_ = 100.0; 
      private Boolean showPairs_ = false;
      private Boolean showSummary_ = false;
      private Boolean showOverlay_ = false;
      private Boolean p2dDistanceCalc_ = false;
      private Boolean showXYHistogram_ = false;
      private Boolean p2dSingleFrames_ = false;
      private Double registrationError_ = 0.0;
      private Boolean showHistogram_ = false;
      private Boolean bootstrap_ = false;
       
      
      public ParticlePairLister build() {
         return new ParticlePairLister(this);
      }

      public Builder rows(int[] rows) {
         rows_ = rows;
         return this;
      }

      public Builder maxDistanceNm(Double maxDistanceNm) {
         maxDistanceNm_ = maxDistanceNm;
         return this;
      }
      
      public Builder showPairs(Boolean showPairs) {
         showPairs_ = showPairs;
         return this;
      }
      
      public Builder showHistogram(Boolean show) {
         showHistogram_ = show;
         return this;
      }
      
      public Builder showXYHistogram(Boolean show) {
         showXYHistogram_ = show;
         return this;
      }

      public Builder showSummary(Boolean showSummary) {
         showSummary_ = showSummary;
         return this;
      }

      public Builder showOverlay(Boolean showOverlay) {
         showOverlay_ = showOverlay;
         return this;
      }

      public Builder p2d(Boolean p2d) {
         p2dDistanceCalc_ = p2d;
         return this;
      }

      public Builder p2dSingleFrames(Boolean p2dSingleFrames) {
         p2dSingleFrames_ = p2dSingleFrames;
         return this;
      }


      public Builder registrationError(Double registrationError) {
         registrationError_ = registrationError;
         return this;
      }
      
      public Builder bootstrap(Boolean bootstrap) {
         bootstrap_ = bootstrap;
         return this;
      }

   }

   public ParticlePairLister(Builder builder) {
      rows_ = builder.rows_;
      maxDistanceNm_ = builder.maxDistanceNm_;
      showPairs_ = builder.showPairs_;
      showSummary_ = builder.showSummary_;
      showOverlay_ = builder.showOverlay_;
      p2dDistanceCalc_ = builder.p2dDistanceCalc_;
      showXYHistogram_ = builder.showXYHistogram_;
      p2dSingleFrames_ = builder.p2dSingleFrames_;
      registrationError_ = builder.registrationError_;
      showHistogram_ = builder.showHistogram_;
      bootstrap_ = builder.bootstrap_;
   }

   public Builder copy() {
      return new Builder().
              rows(rows_).
              maxDistanceNm(maxDistanceNm_).
              showPairs(showPairs_).
              showSummary(showSummary_).
              showOverlay(showOverlay_).
              p2d(p2dDistanceCalc_).
              showHistogram(showHistogram_).
              p2dSingleFrames(p2dSingleFrames_).
              registrationError(registrationError_).
              showXYHistogram(showXYHistogram_).
              bootstrap(bootstrap_);
   }

      
   /**
    * Cycles through the spots of the selected data set and finds the most
    * nearby spot in channel 2. It will list this as a pair if the two spots are
    * within maxDistanceNm_ of each other.
    *
    * Once all pairs are found, it will go through all frames and try to build
    * up tracks. If the spot is within maxDistanceNm_ between frames, the code
    * will consider the particle to be identical.
    *
    * All "tracks" of particles will be listed
    *
    * Distances are calculated using various flavors of the P2D implementation.
    * See: https://doi.org/10.1101/234740 
    * 
    * Needed input variables are set through a builder.
    *
    */
   public void listParticlePairTracks() {

      Runnable doWorkRunnable = new Runnable() {

         @Override
         public void run() {

            final DataCollectionForm dc = DataCollectionForm.getInstance();

            // Show Particle List as linked Results Table
            ResultsTable rt = new ResultsTable();
            rt.reset();
            rt.setPrecision(2);

            // Show Particle Summary as Linked Results Table
            ResultsTable rt2 = new ResultsTable();
            rt2.reset();
            rt2.setPrecision(1);

            // Saves output of P2D fitting            
            ResultsTable rt3 = new ResultsTable();
            rt3.reset();
            rt3.setPrecision(2);

            int rowCounter = 0;
            for (int row : rows_) {
               rowCounter++;
               ij.IJ.showStatus("Creating Pairs for row " + rowCounter);

               // index spots by position
               SpotsByPosition spotsByPosition = 
                       PairOrganizer.spotsByPosition(dc.getSpotData(row).spotList_);
               List<Integer> positions = spotsByPosition.positionsUsed_;
               final int nrChannels = dc.getSpotData(row).nrChannels_;
               
               // position, channel, frame, List of GsSpotPairs
               Map<Integer, List<List<List<GsSpotPair>>>> spotPairsByFrame
                       = PairOrganizer.spotPairsByFrame(dc, row, maxDistanceNm_, spotsByPosition);           

               Map<Integer, Map<Integer, Map<Integer, List <List<GsSpotPair>>>>> allPairsIndexed
                       = PairOrganizer.spotPairsByFrameAndChannel(dc, row, maxDistanceNm_, spotsByPosition);
               
               
               if (showPairs_ ) {
                  ResultsTable pairTable = new ResultsTable();
                  pairTable.setPrecision(2);
                  for (int pos : positions) {
                     for (int ch1 : allPairsIndexed.get(pos).keySet()) {
                        for (int ch2 : allPairsIndexed.get(pos).get(ch1).keySet())
                        
                        for (List<GsSpotPair> pairList : allPairsIndexed.get(pos).get(ch1).get(ch2)) {
                           for (GsSpotPair pair : pairList) {
                              pairTable.incrementCounter();
                              pairTable.addValue(Terms.FRAME, pair.getFirstSpot().getFrame());
                              pairTable.addValue(Terms.SLICE, pair.getFirstSpot().getSlice());
                              pairTable.addValue(Terms.CHANNEL1, pair.getFirstSpot().getChannel());
                              pairTable.addValue(Terms.CHANNEL2, pair.getSecondSpot().getChannel());
                              pairTable.addValue(Terms.POSITION, pair.getFirstSpot().getPosition());
                              pairTable.addValue(Terms.XPIX, pair.getFirstSpot().getX());
                              pairTable.addValue(Terms.YPIX, pair.getFirstSpot().getY());
                              pairTable.addValue("X1", pair.getFirstSpot().getXCenter());
                              pairTable.addValue("Y1", pair.getFirstSpot().getYCenter());
                              if (pair.getFirstSpot().hasKey(SpotData.Keys.INTEGRALAPERTURESIGMA)) {
                                 pairTable.addValue("Sigma1", pair.getFirstSpot().
                                         getValue(SpotData.Keys.INTEGRALAPERTURESIGMA));
                              }
                              pairTable.addValue("X2", pair.getSecondSpot().getXCenter());
                              pairTable.addValue("Y2", pair.getSecondSpot().getYCenter());
                              if (pair.getSecondSpot().hasKey(SpotData.Keys.INTEGRALAPERTURESIGMA)) {
                                 pairTable.addValue("Sigma2", pair.getSecondSpot().
                                         getValue(SpotData.Keys.INTEGRALAPERTURESIGMA));
                              }
                              double d2 = NearestPoint2D.distance2(pair.getFirstPoint(), pair.getSecondPoint());
                              double d = Math.sqrt(d2);
                              pairTable.addValue("Distance", d);
                              pairTable.addValue("Orientation (sine)",
                                      NearestPoint2D.orientation(pair.getFirstPoint(), pair.getSecondPoint()));
                           }
                        }
                     }
                  }
                  //  show Pairs panel and attach listener
                  TextPanel tp;
                  TextWindow win;

                  String rtName = "Pairs found in " + dc.getSpotData(row).getName();
                  pairTable.show(rtName);
                  ImagePlus siPlus = ij.WindowManager.getImage(dc.getSpotData(row).title_);
                  Frame frame = WindowManager.getFrame(rtName);
                  if (frame != null && frame instanceof TextWindow && siPlus != null) {
                     win = (TextWindow) frame;
                     tp = win.getTextPanel();

                     // TODO: the following does not work, there is some voodoo going on here
                     for (MouseListener ms : tp.getMouseListeners()) {
                        tp.removeMouseListener(ms);
                    }
                     for (KeyListener ks : tp.getKeyListeners()) {
                        tp.removeKeyListener(ks);
                     }

                     ResultsTableListener myk = new ResultsTableListener(
                             dc.getSpotData(row).dw_, siPlus,
                             pairTable, win, dc.getSpotData(row).halfSize_);
                     tp.addKeyListener(myk);
                     tp.addMouseListener(myk);
                     frame.toFront();
                  }
               }  // end of show pairs
               
               // We have all pairs, assemble in tracks
               ij.IJ.showStatus("Analyzing pairs for row " + rowCounter);
               
               List<List<GsSpotPair>> tracks = PairOrganizer.pairTracks(allPairsIndexed, nrChannels, 
                       dc.getSpotData(row).nrFrames_, maxDistanceNm_, spotsByPosition);
 
               if (tracks.isEmpty()) {
                  MMStudio.getInstance().alerts().postAlert("P2D fit error", 
                            null, "ID: " + dc.getSpotData(row).ID_ + 
                            ", No Pairs found");
               }

               ImagePlus siPlus = ij.WindowManager.getImage(dc.getSpotData(row).title_);
               if (showOverlay_) {
                  if (siPlus != null && siPlus.getOverlay() != null) {
                     siPlus.getOverlay().clear();
                  }
                  Arrow.setDefaultWidth(0.5);
               }

               Iterator<List<GsSpotPair>> itTracks = tracks.iterator();
               int spotId = 0;
               List<Double> allDistances = new ArrayList<>(
                       tracks.size() * dc.getSpotData(row).nrFrames_);
               List<Double> allSigmas = new ArrayList<>(
                       tracks.size() * dc.getSpotData(row).nrFrames_);
               List<Double> sigmasFirstSpot  = new ArrayList<>(
                       tracks.size() * dc.getSpotData(row).nrFrames_);
               List<Double> sigmasSecondSpot = new ArrayList<>(
                       tracks.size() * dc.getSpotData(row).nrFrames_);
               List<Double> vectorDistances = new ArrayList<>(
                       tracks.size() );
               while (itTracks.hasNext()) {
                  List<GsSpotPair> track = itTracks.next();
                  ArrayList<Double> distances = new ArrayList<>();
                  ArrayList<Double> xDiff = new ArrayList<>();
                  ArrayList<Double> yDiff = new ArrayList<>();
                  ArrayList<Double> sigmas = new ArrayList<>();
                  for (GsSpotPair pair : track) {
                     double distance = Math.sqrt(
                             NearestPoint2D.distance2(pair.getFirstPoint(), pair.getSecondPoint()));
                     distances.add(distance);
                     allDistances.add(distance);
                     xDiff.add(pair.getFirstPoint().getX() - pair.getSecondPoint().getX());
                     yDiff.add(pair.getFirstPoint().getY() - pair.getSecondPoint().getY());
                     if (pair.getFirstSpot().hasKey(SpotData.Keys.INTEGRALAPERTURESIGMA)
                             && pair.getSecondSpot().hasKey(SpotData.Keys.INTEGRALAPERTURESIGMA)) {
                        double sigma1 = pair.getFirstSpot().getValue(SpotData.Keys.INTEGRALAPERTURESIGMA);
                        double sigma2 = pair.getSecondSpot().getValue(SpotData.Keys.INTEGRALAPERTURESIGMA);
                        double sigma = Math.sqrt(sigma1 * sigma1
                                + sigma2 * sigma2
                                + registrationError_ * registrationError_);
                        sigmas.add(sigma);
                        allSigmas.add(sigma);
                        sigmasFirstSpot.add(sigma1);
                        sigmasSecondSpot.add(sigma2);
                     }
                  }
                  GsSpotPair pair = track.get(0);
                  rt2.incrementCounter();
                  rt2.addValue("Row ID", dc.getSpotData(row).ID_);
                  rt2.addValue("Spot ID", spotId);
                  rt2.addValue(Terms.FRAME, pair.getFirstSpot().getFrame());
                  rt2.addValue(Terms.SLICE, pair.getFirstSpot().getSlice());
                  rt2.addValue(Terms.CHANNEL1, pair.getFirstSpot().getChannel());
                  rt2.addValue(Terms.CHANNEL2, pair.getSecondSpot().getChannel());
                  rt2.addValue(Terms.POSITION, pair.getFirstSpot().getPosition());
                  rt2.addValue(Terms.XPIX, pair.getFirstSpot().getX());
                  rt2.addValue(Terms.YPIX, pair.getFirstSpot().getY());
                  rt2.addValue("n", track.size());
                 
                  // Average of Euclidean distances in this strack
                  double avg = ListUtils.listAvg(distances);
                  rt2.addValue("Distance-Avg", avg);
                  // Standard Deviation of Euclidean distances in this track
                  double std = ListUtils.listStdDev(distances, avg);
                  rt2.addValue("Distance-StdDev", std);
                 
                  // Average of weighted sigmas: Sqrt(sigma1(^2) + sigma2(^2) in this track
                  if (sigmas.size() > 0) {
                     double avgSigma = ListUtils.listAvg(sigmas);
                     rt2.addValue("Distance Uncertainty", avgSigma);
                  }
                  // only needed when using p2d - multiframe
                  if (p2dDistanceCalc_ && !p2dSingleFrames_) {
                     double xDiffAvg = ListUtils.listAvg(xDiff);
                     double yDiffAvg = ListUtils.listAvg(yDiff);
                     double vectorDistanceAvg = (Math.sqrt( xDiffAvg * xDiffAvg + 
                             yDiffAvg * yDiffAvg));
                     vectorDistances.add(vectorDistanceAvg);
                     rt2.addValue("Distance-VectorAvg", vectorDistanceAvg);
                  }

                  if (showOverlay_) {
                     /* draw arrows in overlay */
                     double mag = 100.0;  // factor that sets magnification of the arrow
                     double factor = mag * 1 / dc.getSpotData(row).pixelSizeNm_;  // factor relating mag and pixelSize
                     int xStart = track.get(0).getFirstSpot().getX();
                     int yStart = track.get(0).getFirstSpot().getY();

                     Arrow arrow = new Arrow(xStart, yStart,
                             xStart + (factor * ListUtils.listAvg(xDiff)),
                             yStart + (factor * ListUtils.listAvg(yDiff)));
                     arrow.setHeadSize(3);
                     arrow.setOutline(false);
                     if (siPlus != null && siPlus.getOverlay() == null) {
                        siPlus.setOverlay(arrow, Color.yellow, 1, Color.yellow);
                     } else if (siPlus != null && siPlus.getOverlay() != null) {
                        siPlus.getOverlay().add(arrow);
                     }
                  }
                  spotId++;
               }

               if (showOverlay_) {
                  if (siPlus != null) {
                     siPlus.setHideOverlay(false);
                  }
               }

               if (showXYHistogram_) {
                  Map<Integer, Map<Integer, List<GsSpotPair>>> allPossiblePairs
                          = PairOrganizer.allPossiblePairs(dc, row, 
                                  maxDistanceNm_, spotsByPosition);
                  int screenOffset = 50;
                  for (int ch1 = 1; ch1 < nrChannels; ch1++) {
                     for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
                        List<Double> xDiff = new ArrayList<>();
                        List<Double> yDiff = new ArrayList<>();
                        for (GsSpotPair pair : allPossiblePairs.get(ch1).get(ch2)) {
                           xDiff.add(pair.getFirstPoint().getX()
                                   - pair.getSecondPoint().getX());
                           yDiff.add(pair.getFirstPoint().getY()
                                   - pair.getSecondPoint().getY());

                        }
                        if (xDiff.size() > 4 && yDiff.size() > 4) {
                           try {
                              double[] xDiffArray = ListUtils.toArray(xDiff);
                              double[] xGaussian = fitGaussianToData(xDiffArray,
                                      -maxDistanceNm_,
                                      maxDistanceNm_);
                              GaussianUtils.plotGaussian("Gaussian fit of X distances of: "
                                      + dc.getSpotData(row).getName() + ". channel "
                                      + ch1 + " versus " + ch2,
                                      xDiffArray,
                                      xDiffArray[0] - 1.0,
                                      xDiffArray[xDiffArray.length - 1] + 1.0,
                                      xGaussian, 50 + screenOffset, 300 + screenOffset);
                              double[] yDiffArray = ListUtils.toArray(yDiff);
                              double[] yGaussian = fitGaussianToData(yDiffArray,
                                      -maxDistanceNm_,
                                      maxDistanceNm_);
                              GaussianUtils.plotGaussian("Gaussian fit of Y distances of: "
                                      + dc.getSpotData(row).getName() + ". channel "
                                      + ch1 + " versus " + ch2,
                                      yDiffArray,
                                      yDiffArray[0] - 1.0,
                                      yDiffArray[yDiffArray.length - 1] + 1.0,
                                      //-5.0 * yGaussian[1],
                                      //5.0 * yGaussian[1],
                                      yGaussian, 750 + screenOffset, 300 + screenOffset);
                              final double combinedError = Math.sqrt(
                                      xGaussian[0] * xGaussian[0]
                                      + yGaussian[0] * yGaussian[0]);
                              ij.IJ.log(dc.getSpotData(row).getName() + "-Ch" + (ch1 + 1)
                                      + " X-error: "
                                      + NumberUtils.doubleToDisplayString(xGaussian[0], 3)
                                      + "nm, sigma: "
                                      + NumberUtils.doubleToDisplayString(xGaussian[1], 3)
                                      + "nm, Y-error: "
                                      + NumberUtils.doubleToDisplayString(yGaussian[0], 3)
                                      + "nm, sigma: "
                                      + NumberUtils.doubleToDisplayString(yGaussian[1], 3)
                                      + "nm, combined error: "
                                      + NumberUtils.doubleToDisplayString(combinedError, 3)
                                      + "nm.");
                              screenOffset += 50;

                           } catch (FittingException ex) {
                              ReportingUtils.showError("Failed to fit Gaussian, try decreasing the Maximum Distance value");
                           }
                        }
                     }
                  }
               }

               if (showSummary_) {
                  String rtName = dc.getSpotData(row).getName() + " Particle Summary";
                  rt2.show(rtName);
                  siPlus = ij.WindowManager.getImage(dc.getSpotData(row).title_);
                  Frame frame = WindowManager.getFrame(rtName);
                  if (frame != null && frame instanceof TextWindow && siPlus != null) {
                     TextWindow win = (TextWindow) frame;
                     TextPanel tp = win.getTextPanel();

                     // TODO: the following does not work, there is some voodoo going on here
                     for (MouseListener ms : tp.getMouseListeners()) {
                        tp.removeMouseListener(ms);
                     }
                     for (KeyListener ks : tp.getKeyListeners()) {
                        tp.removeKeyListener(ks);
                     }

                     ResultsTableListener myk = new ResultsTableListener(
                             dc.getSpotData(row).dw_, siPlus,
                             rt2, win, dc.getSpotData(row).halfSize_);
                     tp.addKeyListener(myk);
                     tp.addMouseListener(myk);
                     frame.toFront();
                  }
               }

               /**
                * *************** Single frame calculations ******************
                */
               if (p2dDistanceCalc_ && p2dSingleFrames_ && allDistances.size() > 0) {
                  double[] d = ListUtils.toArray(allDistances);
                  double[] sigmas = ListUtils.toArray(allSigmas);
                  if (d.length != sigmas.length) {
                     ReportingUtils.showError("Internal Error: number of distances and sigmas not identical\n"
                             + "Data may not contain Mortenson Integral Sigma from Aperture intensity");
                     return;
                  }

                  P2DFitter p2df = new P2DFitter(d, sigmas, true, false, maxDistanceNm_);

                  // Generate a population average of the distance sigma
                  double distMean = ListUtils.listAvg(allDistances);
                  double sfsAvg = ListUtils.listAvg(sigmasFirstSpot);
                  double sSsAvg = ListUtils.listAvg(sigmasSecondSpot);
                  double sfsStdDev = ListUtils.listStdDev(sigmasFirstSpot, sfsAvg);
                  double sSsStdDev = ListUtils.listStdDev(sigmasSecondSpot, sSsAvg);
                  double distStd = Math.sqrt(sfsAvg * sfsAvg + sSsAvg * sSsAvg
                          + sfsStdDev * sfsStdDev + sSsStdDev * sSsStdDev
                          + registrationError_ * registrationError_);

                  p2df.setStartParams(distMean, distStd);

                  try {
                     double[] p2dfResult = p2df.solve();
                     double mu = p2dfResult[0];
                     double[] muSigma = {mu, distStd};

                     // calculate standard devication accoring to Fisher information theory
                     // algorithm by Jongmin Sung
                     // stdDev (of mu) = 1 / sqrt(info), where info:
                     // info = abs (LL(mu + dmu) + LL (mu-dmu) - 2 * LL(mu) / dmu^2)
                     // where dmu of 0.001nm should be small enough
                     double dMu = 0.001;
                     double[] llTest = {mu - dMu, mu, mu + dMu};
                     double[] llResult = p2df.logLikelihood(p2dfResult, llTest);
                     double info = Math.abs(
                             llResult[2] + llResult[0] - 2 * llResult[1])
                             / (dMu * dMu);
                     double fisherStdDev = 1 / Math.sqrt(info);

                     // Uncomment the following to plot loglikelihood
                     /*
                     double sigmaRange = 4.0 * distStd / Math.sqrt(d.length);
                     double resolution = 0.001 * distStd;
                     double[] distances;
                     distances = p2df.getDistances(mu - sigmaRange, resolution, mu + sigmaRange);
                     double[] logLikelihood = p2df.logLikelihood(p2dfResult, distances);

                     XYSeries data = new XYSeries("distances(nm)");
                     for (int i = 0; i < distances.length && i < logLikelihood.length; i++) {
                         data.add(distances[i], logLikelihood[i]);
                     }
                     GaussianUtils.plotData("Log Likelihood for " + dc.getSpotData(row).getName(), 
                                      data, "Distance (nm)", "Likelihood", 100, 100);
                     */
                     /*
                     // Confidence interval calculation as in matlab code by Stirling Churchman
                     
                     int indexOfMaxLogLikelihood = CalcUtils.maxIndex(logLikelihood);
                     int[] halfMax = CalcUtils.indicesToValuesClosest(logLikelihood,
                             logLikelihood[indexOfMaxLogLikelihood] - 0.5);
                     double dist1 = distances[halfMax[0]];
                     double dist2 = distances[halfMax[1]];
                     double lowConflim = mu - dist1;
                     double highConflim = dist2 - mu;
                     if (lowConflim < 0.0) {
                        lowConflim = mu - dist2;
                        highConflim = dist1 - mu;
                     }
                      */
                     String msg1 = "P2D fit for " + dc.getSpotData(row).getName();
                     String msg2 = "n = " + allDistances.size() + ", mu = "
                             + NumberUtils.doubleToDisplayString(mu, 2)
                             + "\u00b1"
                             + NumberUtils.doubleToDisplayString(fisherStdDev, 2)
                             + "  nm, sigma = "
                             + NumberUtils.doubleToDisplayString(distStd, 2)
                             + " nm, ";
                     MMStudio.getInstance().alerts().postAlert(msg1, null, msg2);

                     MMStudio.getInstance().alerts().postAlert("Gaussian distribution for "
                             + dc.getSpotData(row).getName(),
                             null,
                             "n = " + allDistances.size()
                             + ", avg = "
                             + NumberUtils.doubleToDisplayString(distMean, 2)
                             + " nm, std = "
                             + NumberUtils.doubleToDisplayString(distStd, 2) + " nm");

                     /*
                     // we have a good estimate of mu, use this to estimate an average std      
                     P2DFitter p2df2 = new P2DFitter(d, null, false, true, maxDistanceNm_);
                     p2df2.setStartParams(mu, distStd);

                     double[] p2df2Result = p2df2.solve();
                     // Confidence interval calculation as in matlab code by Stirling Churchman
                     double[] fitResult2 = new double[] {mu, p2df2Result[0]};
                      */
                     // plot function and histogram
                     if (showHistogram_) {
                        GaussianUtils.plotP2D("P2D fit of: "
                                + dc.getSpotData(row).getName() + " distances",
                                d, maxDistanceNm_, muSigma);
                     }

                     // The following is used to output results in a machine readable fashion
                     // Uncomment when needed:
                     rt3.incrementCounter();
                     rt3.addValue("Max. Dist.", maxDistanceNm_);
                     rt3.addValue("File", dc.getSpotData(row).getName());
                     String useVect = p2dSingleFrames_ ? "no" : "yes";
                     rt3.addValue("Vect. Dist.", useVect);
                     String fittedSigma = "yes";
                     rt3.addValue("Fit Sigma", fittedSigma);
                     rt3.addValue("Sigma from data", "yes");
                     rt3.addValue("Registration error", registrationError_);
                     rt3.addValue("n", allDistances.size());
                     rt3.addValue("Frames", dc.getSpotData(row).nrFrames_);
                     rt3.addValue("Positions", dc.getSpotData(row).nrPositions_);
                     rt3.addValue("mu", mu);
                     rt3.addValue("stdDev", fisherStdDev);
                     rt3.addValue("sigma", distStd);

                     rt3.show("P2D Summary");

                  } catch (FittingException fe) {
                     String msg = "ID: " + dc.getSpotData(row).ID_
                             + ", Failed to fit p2d function";
                     MMStudio.getInstance().alerts().postAlert("P2D fit error",
                             null, msg);
                     if (row == rows_[rows_.length - 1]) {
                        ReportingUtils.showError(msg);
                     }
                  } catch (TooManyEvaluationsException tmee) {
                     String msg = "ID: " + dc.getSpotData(row).ID_
                             + ", Too many evaluations while fitting";
                     MMStudio.getInstance().alerts().postAlert("P2D fit error",
                             null, msg);
                     if (row == rows_[rows_.length - 1]) {
                        ReportingUtils.showError(msg);
                     }
                  }
               }

               /**
                * **************** P2D - Multi-Frame **********************
                */
               if (p2dDistanceCalc_ && !p2dSingleFrames_ && allDistances.size() > 0) {
                  double[] p2dfResult = {0.0, 0.0};
                  try {
                     p2dfResult = p2dLeastSquareFit(vectorDistances, maxDistanceNm_);
                  } catch (FittingException fe) {
                     String msg = "ID: " + dc.getSpotData(row).ID_
                             + ", Failed to fit p2d function";
                     MMStudio.getInstance().alerts().postAlert("P2D fit error",
                             null, msg);
                     if (row == rows_[rows_.length - 1]) {
                        ReportingUtils.showError(msg);
                     }
                  } catch (TooManyEvaluationsException tmee) {
                     String msg = "ID: " + dc.getSpotData(row).ID_
                             + ", Too many evaluations while fitting";
                     MMStudio.getInstance().alerts().postAlert("P2D fit error",
                             null, msg);
                     if (row == rows_[rows_.length - 1]) {
                        ReportingUtils.showError(msg);
                     }
                  }

                  double mu = p2dfResult[0];
                  double sigma = p2dfResult[1];

                  // calculate standard devication of MLE fit 
                  // accoring to Fisher information theory
                  // algorithm by Jongmin Sung
                  // stdDev (of mu) = 1 / sqrt(info), where info:
                  // info = abs (LL(mu + dmu) + LL (mu-dmu) - 2 * LL(mu) / dmu^2)
                  // where dmu of 0.001nm should be small enough
                  // double dMu = 0.001;
                  // double[] llTest = {mu - dMu, mu, mu + dMu};
                  // double[] llResult = p2df.logLikelihood(p2dfResult, llTest);
                  // double info = Math.abs(
                  //       llResult[2] + llResult[0] - 2 * llResult[1])
                  //       / (dMu * dMu);
                  // double fisherStdDev = 1 / Math.sqrt(info);
                  double muEstimate = 0.0;
                  double stdDevEstimate = 0.0;
                  boolean bootstrapSucceeded = false;
                  if (bootstrap_) {
                     ij.IJ.showStatus("Bootstrap analysis running");
                     int counter = 0;
                     int errorCounter = 0;
                     final int nrRuns = 1000;
                     final int maxNrErrors = 10;
                     List<Double> mus = new ArrayList<>();
                     double[] bootsTrapResult;
                     while (counter < nrRuns && errorCounter < maxNrErrors) {
                        List bootstrapList = ListUtils.listToListForBootstrap(vectorDistances);
                        try {
                           bootsTrapResult = p2dLeastSquareFit(bootstrapList, maxDistanceNm_);
                           mus.add(bootsTrapResult[0]);
                           counter++;
                           if (counter % 25 == 0) {
                              ij.IJ.showProgress(counter, nrRuns);
                           }
                        } catch (FittingException fe) {
                           String msg = "ID: " + dc.getSpotData(row).ID_
                                   + ", Failed to fit p2d function";
                           MMStudio.getInstance().alerts().postAlert("P2D fit error",
                                   null, msg);
                           if (row == rows_[rows_.length - 1]) {
                              ReportingUtils.showError(msg);
                           }
                           errorCounter++;
                        } catch (TooManyEvaluationsException tmee) {
                           String msg = "ID: " + dc.getSpotData(row).ID_
                                   + ", Too many evaluations while fitting";
                           MMStudio.getInstance().alerts().postAlert("P2D fit error",
                                   null, msg);
                           if (row == rows_[rows_.length - 1]) {
                              ReportingUtils.showError(msg);
                           }
                           errorCounter++;
                        } catch (NumberIsTooLargeException nitle) {
                           String msg = "ID: " + dc.getSpotData(row).ID_
                                   + ", Internal error during bootystrapping";
                           MMStudio.getInstance().alerts().postAlert("P2D fit error",
                                   null, msg);
                           if (row == rows_[rows_.length - 1]) {
                              ReportingUtils.showError(msg);
                           }
                           errorCounter++;
                        }
                     }
                     if (errorCounter >= 100) {
                        MMStudio.getInstance().alerts().postAlert("Boostrapping error",
                                null, "Bootstrap analysis failed due to too many errors");
                     } else {
                        muEstimate = ListUtils.listAvg(mus);
                        stdDevEstimate = ListUtils.listStdDev(mus, muEstimate);
                        bootstrapSucceeded = true;
                     }
                  }

                  ij.IJ.showStatus("");

                  String msg1 = "P2D fit for " + dc.getSpotData(row).getName();
                  String msg2 = "n = " + allDistances.size() + ", mu = "
                          + NumberUtils.doubleToDisplayString(mu, 2)
                          // + "\u00b1"
                          //+ NumberUtils.doubleToDisplayString(fisherStdDev, 2)
                          + " nm, sigma = "
                          + NumberUtils.doubleToDisplayString(sigma, 2)
                          + " nm";
                  MMStudio.getInstance().alerts().postAlert(msg1, null, msg2);

                  // plot function and histogram
                  if (showHistogram_) {
                     GaussianUtils.plotP2D("P2D fit of: "
                             + dc.getSpotData(row).getName() + " distances",
                             ListUtils.toArray(vectorDistances), maxDistanceNm_,
                             p2dfResult);
                  }

                  rt3.incrementCounter();
                  rt3.addValue("Max. Dist.", maxDistanceNm_);
                  rt3.addValue("File", dc.getSpotData(row).getName());
                  String useVect = p2dSingleFrames_ ? "no" : "yes";
                  rt3.addValue("Vect. Dist.", useVect);
                  rt3.addValue("Fit Sigma", "no");
                  rt3.addValue("Sigma from data", "no");
                  rt3.addValue("n", allDistances.size());
                  rt3.addValue("Frames", dc.getSpotData(row).nrFrames_);
                  rt3.addValue("Positions", dc.getSpotData(row).nrPositions_);
                  rt3.addValue("mu", mu);
                  //rt3.addValue("stdDev", fisherStdDev);
                  rt3.addValue("sigma", sigma);
                  if (bootstrapSucceeded) {
                     rt3.addValue("bootstrap Mu", muEstimate);
                     rt3.addValue("bootstrap StdDev", stdDevEstimate);
                  }

                  rt3.show("P2D Summary");

               }  // end of p2dCalc using multiple frames

               ij.IJ.showProgress(100.0);
               ij.IJ.showStatus("Done listing pairs");

            }  // end of for (row : rows)
         }
      };

      (new Thread(doWorkRunnable)).start();

   }

   /**
    * Fits a list of numbers to a Gaussian function using Maximum Likelihood
    *
    * @param input
    * @param max
    * @param min
    * @return fitresult, double[0] is mu, double[1] is sigma
    * @throws FittingException
    */
   public static double[] fitGaussianToData(final double[] input,
           final double min, final double max) throws FittingException {
      // fit vector distances with gaussian function

      Gaussian1DecdFitter gf = new Gaussian1DecdFitter(input);
      //gf.setLowerBound(min);
      double avg = ListUtils.avg(input);
      gf.setStartParams(avg, ListUtils.stdDev(input, avg));
      return gf.solve();
   }

   public static double[] p2dLeastSquareFit(List distances, double maxDistance)
           throws FittingException, TooManyEvaluationsException {
      double[] d = ListUtils.toArray(distances);

      double vectMean = ListUtils.listAvg(distances);
      double stdDev = ListUtils.listStdDev(distances,
              vectMean);

      P2DEcdfFitter p2decdf = new P2DEcdfFitter(d, vectMean, stdDev,
              maxDistance);

      return p2decdf.solve();

   }

}
