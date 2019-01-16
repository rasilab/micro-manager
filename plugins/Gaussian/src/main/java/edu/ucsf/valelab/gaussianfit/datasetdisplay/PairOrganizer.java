package edu.ucsf.valelab.gaussianfit.datasetdisplay;

import edu.ucsf.valelab.gaussianfit.DataCollectionForm;
import edu.ucsf.valelab.gaussianfit.data.GsSpotPair;
import edu.ucsf.valelab.gaussianfit.data.SpotData;
import edu.ucsf.valelab.gaussianfit.data.SpotsByPosition;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPoint2D;
import edu.ucsf.valelab.gaussianfit.spotoperations.NearestPointByData;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
    * for 3 channel data).  Data structure is indexed by first and second channel
    * of the pair
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
    * Given a dataset of Spots in multiple channels,
    *    generates an indexed datastructure of SpotPairs
    * Datastructure consists of position, channel, frame, List of GsSpotPairs
    * 
    * @param dc DataCollectionForm holding the raw data
    * @param row Row number in the DataCollection form
    * @param maxDistanceNm_ distance between two spots needs be smaller than this
    * @param spotsByP spots indexed by position.  Can be null
    * 
    * @return Datastructure with spotpairs.  Indexed by position, channel1, channel2, frame
    */ 
   public static Map<Integer, Map<Integer, Map<Integer, List <List<GsSpotPair>>>>> 
        spotPairsByFrameAndChannel(
           final DataCollectionForm dc, final int row, final Double maxDistanceNm_,
               SpotsByPosition spotsByP) {
           
      SpotsByPosition spotsByPosition = spotsByP;
      if (spotsByPosition == null) {
         spotsByPosition = PairOrganizer.spotsByPosition(dc.getSpotData(row).spotList_);
      }
      // position, channel1, channel2, frame, List of GsSpotPairs
      Map<Integer, Map<Integer, Map<Integer, List <List<GsSpotPair>>>>> allPairsIndexed
              = new HashMap<>();
      List<Integer> positions = spotsByPosition.positionsUsed_;
      final int maxPos = positions.get(positions.size() - 1);
      final int nrChannels = dc.getSpotData(row).nrChannels_;

      // Go through all frames to find all pairs, organized by position
      for (int pos : positions) {
         allPairsIndexed.put(pos, new HashMap<>());
         for (int ch1 = 1; ch1 < nrChannels; ch1++) {
            allPairsIndexed.get(pos).put(ch1, new HashMap<>());
            for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
               allPairsIndexed.get(pos).get(ch1).put(ch2, new ArrayList<>());
            }
         }
         
         for (int frame = 1; frame <= dc.getSpotData(row).nrFrames_; frame++) {
            ij.IJ.showProgress(pos * dc.getSpotData(row).nrFrames_ + frame,
                    maxPos * dc.getSpotData(row).nrFrames_);

            // Get points from all channels as ArrayLists 
            Map<Integer, List<SpotData>> spotsByCh = new HashMap<>(nrChannels);
            Map<Integer, List<Point2D.Double>> xyPointsByCh = new HashMap<>(nrChannels);
            for (int ch = 1; ch <= nrChannels; ch++) {
               spotsByCh.put(ch, new ArrayList<>());
               xyPointsByCh.put(ch, new ArrayList<>());
            }
            for (SpotData gs : spotsByPosition.spotListByPosition_.get(pos)) {
               if (gs.getFrame() == frame) {
                  spotsByCh.get(gs.getChannel()).add(gs);
                  xyPointsByCh.get(gs.getChannel()).add(new Point2D.Double(
                          gs.getXCenter(), gs.getYCenter()));
               }
            }

            // Find matching points in the two ArrayLists
            for (int ch1 = 1; ch1 < nrChannels; ch1++) {
               for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
                  allPairsIndexed.get(pos).get(ch1).get(ch2).add(new ArrayList<>());
                  NearestPoint2D np = new NearestPoint2D(xyPointsByCh.get(ch2), maxDistanceNm_);
                  final List<SpotData> spotsSecondCh = spotsByCh.get(ch2);
                  for (SpotData spot : spotsByCh.get(ch1)) {
                     Point2D.Double pFirst = new Point2D.Double(
                             spot.getXCenter(), spot.getYCenter());
                     Point2D.Double pLast = np.findKDWSE(pFirst);
                     if (pLast != null) {
                        // found this point in the ch2 spot list
                        SpotData secondChSpot = null;
                        for (int i = 0; i < spotsSecondCh.size() && secondChSpot == null; i++) {
                           if (pLast.x == spotsSecondCh.get(i).getXCenter()
                                   && pLast.y == spotsSecondCh.get(i).getYCenter()) {
                              secondChSpot = spotsSecondCh.get(i);
                           }
                        }
                        if (secondChSpot != null) {
                           GsSpotPair pair = new GsSpotPair(spot, secondChSpot, pFirst, pLast);
                           allPairsIndexed.get(pos).get(ch1).get(ch2).get(frame - 1).add(pair);
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

      return allPairsIndexed;

   }

        
   /**
    * Assembles tracks from lists of spotPairs
    * 
    * @param spotPairs Data structure of spot pairs indexed by position, ch1, ch2, and frame
    *                   Can be generated with spotPairsByFrameAndChannel function
    * @param nrFrames   nrFrames in this dataset
    * @param nrChannels nrChannels in this dataset
    * @param maxDistanceNm_   distance between frames should be less than this
    * @param spotsByPosition  Datastructure containing all positions used in this dataset
    * @return List with tracks at each position
    */
   public static List<List<GsSpotPair>> pairTracks(
           Map<Integer, Map<Integer, Map<Integer, List<List<GsSpotPair>>>>> spotPairs,
           final int nrChannels, final int nrFrames, final Double maxDistanceNm_,
           SpotsByPosition spotsByPosition) {

      List<List<GsSpotPair>> tracks = new ArrayList<>();

      for (int pos : spotsByPosition.positionsUsed_) {
         // prepare NearestPoint objects to speed up finding closest pair 
         List<NearestPointByData> npsp = new ArrayList<>();
         for (int ch1 = 1; ch1 < nrChannels; ch1++) {
            for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
               for (int frame = 1; frame <= nrFrames; frame++) {
                  npsp.add(new NearestPointByData(
                          spotPairs.get(pos).get(ch1).get(ch2).get(frame - 1), maxDistanceNm_));
               }

               for (int firstFrame = 1; firstFrame <= nrFrames; firstFrame++) {
                  Iterator<GsSpotPair> iSpotPairs
                          = spotPairs.get(pos).get(ch1).get(ch2).get(firstFrame - 1).iterator();
                  while (iSpotPairs.hasNext()) {
                     GsSpotPair spotPair = iSpotPairs.next();

                     if (!spotPair.partOfTrack()) {
                        for (int frame = firstFrame; frame <= nrFrames; frame++) {
                           if (!spotPair.partOfTrack() && spotPair.getFirstSpot().getFrame() == frame) {
                              ArrayList<GsSpotPair> track = new ArrayList<>();
                              track.add(spotPair);
                              spotPair.useInTrack(true);
                              int searchInFrame = frame + 1;
                              while (searchInFrame <= nrFrames) {
                                 GsSpotPair newSpotPair = (GsSpotPair) npsp.get(searchInFrame - 1).findKDWSE(
                                         new Point2D.Double(spotPair.getFirstPoint().getX(),
                                                 spotPair.getFirstPoint().getY()));
                                 if (newSpotPair != null && !newSpotPair.partOfTrack()) {
                                    newSpotPair.useInTrack(true);
                                    spotPair = newSpotPair;
                                    track.add(spotPair);
                                 }
                                 searchInFrame++;
                              }
                              tracks.add(track);
                           }
                        }
                     }
                  }
               }
            }  // end of assembling tracks.  
         }
      
      }
      return tracks;
   }
  

}
