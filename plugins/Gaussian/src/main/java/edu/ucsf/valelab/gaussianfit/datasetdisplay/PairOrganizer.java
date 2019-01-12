package edu.ucsf.valelab.gaussianfit.datasetdisplay;

import edu.ucsf.valelab.gaussianfit.DataCollectionForm;
import edu.ucsf.valelab.gaussianfit.data.GsSpotPair;
import edu.ucsf.valelab.gaussianfit.data.SpotData;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPoint2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author nico
 */
public class PairOrganizer {

   public static Map<Integer, Map<Integer, Map<Integer, Map<Integer, List<GsSpotPair>>>>>
           allPossiblePairs(final DataCollectionForm dc, final int row, final Double maxDistanceNm_) {

      // position, channel1, channel2, frame, List of GsSpotPairs
      Map<Integer, Map<Integer, Map<Integer, Map<Integer, List<GsSpotPair>>>>> spotPairsByFrame
              = new HashMap<>();

      // index spots by position
      Map<Integer, ArrayList<SpotData>> spotListsByPosition
              = new HashMap<>();
      // and keep track of the positions that are actually used
      List<Integer> positions = new ArrayList<>();
      for (SpotData spot : dc.getSpotData(row).spotList_) {
         if (positions.indexOf(spot.getPosition()) == -1) {
            positions.add(spot.getPosition());
         }
         if (spotListsByPosition.get(spot.getPosition()) == null) {
            spotListsByPosition.put(spot.getPosition(), new ArrayList<>());
         }
         spotListsByPosition.get(spot.getPosition()).add(spot);
      }
      Collections.sort(positions);
      final int maxPos = positions.get(positions.size() - 1);
      final int nrChannels = dc.getSpotData(row).nrChannels_;

      // First go through all frames to find all pairs, organize by position
      for (int pos : positions) {
         spotPairsByFrame.put(pos, new HashMap<>());
         for (int ch1 = 1; ch1 <= nrChannels; ch1++) {
            spotPairsByFrame.get(pos).put(ch1, new HashMap<>());
            for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
               spotPairsByFrame.get(pos).get(ch1).put(ch2, new HashMap<>());
            }
         }

         for (int frame = 1; frame <= dc.getSpotData(row).nrFrames_; frame++) {
            ij.IJ.showProgress(pos * dc.getSpotData(row).nrFrames_ + frame,
                    maxPos * dc.getSpotData(row).nrFrames_);

            // Get points from all channels as ArrayLists
            Map<Integer, List<SpotData>> spotsByCh = new HashMap<>(nrChannels);
            Map<Integer, List<Point2D.Double>> pointsByCh = new HashMap<>(nrChannels);
            for (int ch1 = 1; ch1 <= nrChannels; ch1++) {
               spotsByCh.put(ch1, new ArrayList<>());
               pointsByCh.put(ch1, new ArrayList<>());
               for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
                  spotPairsByFrame.get(pos).get(ch1).get(ch2).put(frame, new ArrayList<>());
               }
            }

            for (SpotData gs : spotListsByPosition.get(pos)) {
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
               for (int ch = 1; ch < chLast; ch++) {
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
                           try {
                              spotPairsByFrame.get(pos).get(ch).get(chLast).get(frame).add(pair);
                           } catch (NullPointerException npe) {
                              System.out.println("pos" + pos + ", ch: "
                                      + ch + ", chLast: " + chLast + ", frame: " + frame);
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

      return spotPairsByFrame;
   }
}
