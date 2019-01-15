package edu.ucsf.valelab.gaussianfit.datasetdisplay;

import edu.ucsf.valelab.gaussianfit.DataCollectionForm;
import edu.ucsf.valelab.gaussianfit.data.GsSpotPair;
import edu.ucsf.valelab.gaussianfit.data.SpotData;
import edu.ucsf.valelab.gaussianfit.data.SpotsByPosition;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPoint2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection of static function to help organize the complicated
 * sets of pairs extracted from the data
 * Pairs need to be indexed by image position, frame nr, and 
 * combination of channels
 * @author nico
 */
public class PairOrganizer {

   /**
    * Indexes alls spots by Position
    * @param spotData List with all the raw SpotData
    * @return Data structure that holds a lits of all positions used, as
    * well as a Map with spotData, indexed by position
    */
   public static SpotsByPosition spotsByPosition(List<SpotData> spotData) {
      // index spots by position
      Map<Integer, List<SpotData>> spotListsByPosition = new HashMap<>();
      // and keep track of the positions that are actually used
      List<Integer> positions = new ArrayList<>();
      for (SpotData spot : spotData) {
         if (positions.indexOf(spot.getPosition()) == -1) {
            positions.add(spot.getPosition());
         }
         if (spotListsByPosition.get(spot.getPosition()) == null) {
            spotListsByPosition.put(spot.getPosition(), new ArrayList<>());
         }
         spotListsByPosition.get(spot.getPosition()).add(spot);
      }
      Collections.sort(positions);
      return new SpotsByPosition(spotListsByPosition, positions);
   }
   

   /**
    * Generates a list of all possible spot pairs (i.e. channel 1-2, 1-3, 2-3
    * for 3 channel data).  Data structure is indexed by channel 1 and channel 2
    * 
    * @param dc Data Collection form containing the raw data
    * @param row Row number of the data in the DataCollection from to be analyzed
    * @param maxDistanceNm_ distance between spots has to be smaller than this
    * @param spotsByP spots indexed by position.  Can be null
    * @return Lists of GsSpotPair, indexed by the two channels to which both spots of the pait belong
    */
   public static Map<Integer, Map<Integer, List<GsSpotPair>>>
           allPossiblePairs(final DataCollectionForm dc, final int row, 
                   final Double maxDistanceNm_, SpotsByPosition spotsByP) {

      // channel1, channel2, List of GsSpotPairs
      final Map<Integer, Map<Integer,  List<GsSpotPair>>> spotPairs = new HashMap<>();
      final int nrChannels = dc.getSpotData(row).nrChannels_;
      for (int c1 = 1; c1 < nrChannels; c1++) {
         spotPairs.put(c1, new HashMap<>(nrChannels - c1));
         for (int c2 = c1 + 1; c2 <= nrChannels; c2++) {
            spotPairs.get(c1).put(c2, new ArrayList<>());
         }
      }
      
      SpotsByPosition spotsByPosition = spotsByP;
      if (spotsByPosition == null) {
         spotsByPosition = PairOrganizer.spotsByPosition(dc.getSpotData(row).spotList_);
      }

      List<Integer> positions = spotsByPosition.positionsUsed_;
      final int maxPos = positions.get(positions.size() - 1);

      // Go through positions and frames to find all pairs
      for (int pos : positions) {
         for (int frame = 1; frame <= dc.getSpotData(row).nrFrames_; frame++) {
            ij.IJ.showProgress(pos * dc.getSpotData(row).nrFrames_ + frame,
                    maxPos * dc.getSpotData(row).nrFrames_);

            // Get points from all channels as ArrayLists
            Map<Integer, List<SpotData>> spotsByCh = new HashMap<>(nrChannels);
            Map<Integer, List<Point2D.Double>> pointsByCh = new HashMap<>(nrChannels);
            for (int ch1 = 1; ch1 <= nrChannels; ch1++) {
               spotsByCh.put(ch1, new ArrayList<>());
               pointsByCh.put(ch1, new ArrayList<>());
            }

            for (SpotData gs : spotsByPosition.spotListByPosition_.get(pos)) {
               if (gs.getFrame() == frame) {
                  spotsByCh.get(gs.getChannel()).add(gs);
                  pointsByCh.get(gs.getChannel()).add(
                          new Point2D.Double(gs.getXCenter(), gs.getYCenter()));
               }
            }

            // Find matching points in the ArrayLists
            for (int chLast = 2; chLast <= nrChannels; chLast++) {
               NearestPoint2D np = new NearestPoint2D(pointsByCh.get(chLast),
                       maxDistanceNm_);
               final List<SpotData> spotsLastCh = spotsByCh.get(chLast);
               for (int ch1 = 1; ch1 < chLast; ch1++) {
                  for (SpotData spot : spotsByCh.get(ch1)) {
                     Point2D.Double pFirst = new Point2D.Double(
                             spot.getXCenter(), spot.getYCenter());
                     Point2D.Double pLast = np.findKDWSE(pFirst);
                     if (pLast != null) {
                        // find this point in the ch2 spot list
                        SpotData chLastSpot = null;
                        for (int i = 0; i < spotsLastCh.size() && chLastSpot == null; i++) {
                           if (pLast.x == spotsLastCh.get(i).getXCenter()
                                   && pLast.y == spotsLastCh.get(i).getYCenter()) {
                              chLastSpot = spotsLastCh.get(i);
                           }
                        }
                        if (chLastSpot != null) {
                           GsSpotPair pair = new GsSpotPair(spot, chLastSpot, pFirst, pLast);
                           try {
                              spotPairs.get(ch1).get(chLast).add(pair);
                           } catch (NullPointerException npe) {
                              System.out.println("pos" + pos + ", ch: "
                                      + ch1 + ", chLast: " + chLast + ", frame: " + frame);
                           }
                        } else {
                           // this should never happen!
                           System.out.println("Failed to find spot");
                        }
                     }
                  }
               }
            }
         }
      } // end of for (int pos : positions)

      return spotPairs;
   }

   /**
    * Given an dataset, generate an indexed datastructure of SpotPairs
    * Datastructure consists of position, channel, frame, List of GsSpotPairs
    * 
    * @param dc DataCollectionForm holding the raw data
    * @param row Row number in the DataCollection form
    * @param maxDistanceNm_ distance between two spots needs be smaller than this
    * @param spotsByP spots indexed by position.  Can be null
    * 
    * @return Datastructure with spotpairs.  Indexed by position, channel, frame
    */        
   public static Map<Integer, List<List<List<GsSpotPair>>>> spotPairsByFrame(
           final DataCollectionForm dc, final int row, final Double maxDistanceNm_,
               SpotsByPosition spotsByP) {

      SpotsByPosition spotsByPosition = spotsByP;
      if (spotsByPosition == null) {
         spotsByPosition = PairOrganizer.spotsByPosition(dc.getSpotData(row).spotList_);
      }

      List<Integer> positions = spotsByPosition.positionsUsed_;
      final int maxPos = positions.get(positions.size() - 1);
      final int nrChannels = dc.getSpotData(row).nrChannels_;
      // position, channel, frame, List of GsSpotPairs
      Map<Integer, List<List<List<GsSpotPair>>>> spotPairsByFrame
              = new HashMap<>();
      // Go through all frames to find all pairs, organized by position
      for (int pos : positions) {
         spotPairsByFrame.put(pos, new ArrayList<>());
         for (int ch = 0; ch < nrChannels; ch++) {
            spotPairsByFrame.get(pos).add(new ArrayList<>());
         }

         for (int frame = 1; frame <= dc.getSpotData(row).nrFrames_; frame++) {
            ij.IJ.showProgress(pos * dc.getSpotData(row).nrFrames_ + frame,
                    maxPos * dc.getSpotData(row).nrFrames_);

            // Get points from all channels as ArrayLists 
            Map<Integer, List<SpotData>> spotsByCh
                    = new HashMap<>(nrChannels);
            for (int ch = 0; ch < nrChannels; ch++) {
               spotsByCh.put(ch, new ArrayList<>());
               spotPairsByFrame.get(pos).get(ch).
                       add(new ArrayList<>());
            }
            List<Point2D.Double> xyPointsLastCh
                    = new ArrayList<>();

            for (SpotData gs : spotsByPosition.spotListByPosition_.get(pos)) {
               if (gs.getFrame() == frame) {
                  spotsByCh.get(gs.getChannel() - 1).add(gs);
                  if (gs.getChannel() == nrChannels) {
                     Point2D.Double point = new Point2D.Double(
                             gs.getXCenter(), gs.getYCenter());
                     xyPointsLastCh.add(point);
                  }
               }
            }

            if (xyPointsLastCh.isEmpty()) {
               //MMStudio.getInstance().alerts().postAlert("No points found error", null,
               //        "Pairs function in Localization plugin: no points found in second channel in frame "
               //        + frame);
               continue;
            }

            // Find matching points in the two ArrayLists
            NearestPoint2D np = new NearestPoint2D(xyPointsLastCh, maxDistanceNm_);
            final List<SpotData> spotsLastCh = spotsByCh.get(nrChannels - 1);
            for (int ch = 0; ch < nrChannels - 1; ch++) {
               for (SpotData spot : spotsByCh.get(ch)) {
                  Point2D.Double pFirst = new Point2D.Double(
                          spot.getXCenter(), spot.getYCenter());
                  Point2D.Double pLast = np.findKDWSE(pFirst);
                  if (pLast != null) {
                     // find this point in the ch2 spot list
                     SpotData chLastSpot = null;
                     for (int i = 0; i < spotsLastCh.size() && chLastSpot == null; i++) {
                        if (pLast.x == spotsLastCh.get(i).getXCenter()
                                && pLast.y == spotsLastCh.get(i).getYCenter()) {
                           chLastSpot = spotsLastCh.get(i);
                        }
                     }
                     if (chLastSpot != null) {
                        GsSpotPair pair = new GsSpotPair(spot, chLastSpot, pFirst, pLast);
                        spotPairsByFrame.get(pos).get(ch).get(frame - 1).add(pair);
                     } else {
                        // this should never happen!
                        System.out.println("Failed to find spot");
                     }
                  }
               }
            }
         }
      } // end of for (int pos : positions)
      
      return spotPairsByFrame;

   }
   
   public static Map<Integer, List<List<List<GsSpotPair>>>> spotPairsByFrameAndChannel(
           final DataCollectionForm dc, final int row, final Double maxDistanceNm_,
               SpotsByPosition spotsByP) {

      List<Integer> positions = spotsByP.positionsUsed_;
      final int maxPos = positions.get(positions.size() - 1);
      final int nrChannels = dc.getSpotData(row).nrChannels_;
      // position, channel, frame, List of GsSpotPairs
      Map<Integer, List<List<List<GsSpotPair>>>> spotPairsByFrame
              = new HashMap<>();
      // Go through all frames to find all pairs, organized by position
      for (int pos : positions) {
         spotPairsByFrame.put(pos, new ArrayList<>());
         for (int ch = 0; ch < nrChannels; ch++) {
            spotPairsByFrame.get(pos).add(new ArrayList<>());
         }

         for (int frame = 1; frame <= dc.getSpotData(row).nrFrames_; frame++) {
            ij.IJ.showProgress(pos * dc.getSpotData(row).nrFrames_ + frame,
                    maxPos * dc.getSpotData(row).nrFrames_);

            // Get points from all channels as ArrayLists 
            Map<Integer, List<SpotData>> spotsByCh
                    = new HashMap<>(nrChannels);
            for (int ch = 0; ch < nrChannels; ch++) {
               spotsByCh.put(ch, new ArrayList<>());
               spotPairsByFrame.get(pos).get(ch).
                       add(new ArrayList<>());
            }
            List<Point2D.Double> xyPointsLastCh
                    = new ArrayList<>();

            for (SpotData gs : spotsByP.spotListByPosition_.get(pos)) {
               if (gs.getFrame() == frame) {
                  spotsByCh.get(gs.getChannel() - 1).add(gs);
                  if (gs.getChannel() == nrChannels) {
                     Point2D.Double point = new Point2D.Double(
                             gs.getXCenter(), gs.getYCenter());
                     xyPointsLastCh.add(point);
                  }
               }
            }

            if (xyPointsLastCh.isEmpty()) {
               //MMStudio.getInstance().alerts().postAlert("No points found error", null,
               //        "Pairs function in Localization plugin: no points found in second channel in frame "
               //        + frame);
               continue;
            }

            // Find matching points in the two ArrayLists
            NearestPoint2D np = new NearestPoint2D(xyPointsLastCh, maxDistanceNm_);
            final List<SpotData> spotsLastCh = spotsByCh.get(nrChannels - 1);
            for (int ch = 0; ch < nrChannels - 1; ch++) {
               for (SpotData spot : spotsByCh.get(ch)) {
                  Point2D.Double pFirst = new Point2D.Double(
                          spot.getXCenter(), spot.getYCenter());
                  Point2D.Double pLast = np.findKDWSE(pFirst);
                  if (pLast != null) {
                     // find this point in the ch2 spot list
                     SpotData chLastSpot = null;
                     for (int i = 0; i < spotsLastCh.size() && chLastSpot == null; i++) {
                        if (pLast.x == spotsLastCh.get(i).getXCenter()
                                && pLast.y == spotsLastCh.get(i).getYCenter()) {
                           chLastSpot = spotsLastCh.get(i);
                        }
                     }
                     if (chLastSpot != null) {
                        GsSpotPair pair = new GsSpotPair(spot, chLastSpot, pFirst, pLast);
                        spotPairsByFrame.get(pos).get(ch).get(frame - 1).add(pair);
                     } else {
                        // this should never happen!
                        System.out.println("Failed to find spot");
                     }
                  }
               }
            }
         }
      } // end of for (int pos : positions)
      
      return spotPairsByFrame;

   }

}
