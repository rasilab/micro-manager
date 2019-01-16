
package edu.ucsf.valelab.gaussianfit.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data structure to hold statistics derived from lists of tracks
 * Data are indexed by the two channels that the spot pairs belong to
 * @author nico
 */
public class TracksStats {
   
   private final int nrChannels_;
   private final Map<Integer, Map<Integer, List<Double>>> allDistances_;
   private final Map<Integer, Map<Integer, List<Double>>> allSigmas_;
   private final Map<Integer, Map<Integer, List<Double>>> sigmasFirstSpot_;
   private final Map<Integer, Map<Integer, List<Double>>> sigmasSecondSpot_;
   private final Map<Integer, Map<Integer, List<Double>>> vectorDistances_;
   

   public TracksStats(final int nrChannels) {
      nrChannels_ = nrChannels;
      allDistances_ = new HashMap<>();
      allSigmas_ = new HashMap<>(); 
      sigmasFirstSpot_ = new HashMap<>();
      sigmasSecondSpot_ = new HashMap<>();
      vectorDistances_ = new HashMap<>();
      
      for (int ch1 = 1; ch1 < nrChannels; ch1++) {
         allDistances_.put(ch1, new HashMap<>());
         allSigmas_.put(ch1, new HashMap<>());
         sigmasFirstSpot_.put(ch1, new HashMap<>());
         sigmasSecondSpot_.put(ch1, new HashMap<>());
         vectorDistances_.put(ch1, new HashMap<>());
         for (int ch2 = ch1 + 1; ch2 <= nrChannels; ch2++) {
            allDistances_.get(ch1).put(ch2, new ArrayList<>());
            allSigmas_.get(ch1).put(ch2, new ArrayList<>());
            sigmasFirstSpot_.get(ch1).put(ch2, new ArrayList<>());
            sigmasSecondSpot_.get(ch1).put(ch2, new ArrayList<>());
            vectorDistances_.get(ch1).put(ch2, new ArrayList<>());
         }
      }
   }
   
   public List<Double> distances(final int ch1, final int ch2) {
      return allDistances_.get(ch1).get(ch2);
   }
   
   public List<Double> sigmas(final int ch1, final int ch2) {
      return allSigmas_.get(ch1).get(ch2);
   }
   
   public List<Double> sigmasFirstSpot(final int ch1, final int ch2) {
      return sigmasFirstSpot_.get(ch1).get(ch2);
   }
   
   public List<Double> sigmasSecondSpot(final int ch1, final int ch2) {
      return sigmasSecondSpot_.get(ch1).get(ch2);
   }
   
   public List<Double> vectorDistances(final int ch1, final int ch2) {
      return vectorDistances_.get(ch1).get(ch2);
   }
   
   public int nrChannels() { return nrChannels_; }
   
   public boolean validateInput(final int ch1, final int ch2) {
      return ch2 > ch1 && ch2 <= nrChannels_ && ch1 > 1;
   }

}
