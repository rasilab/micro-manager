package edu.ucsf.valelab.gaussianfit.data;

import java.util.List;
import java.util.Map;

/**
 *
 * @author nico
 */
public class SpotsByPosition {

   public final Map<Integer, List<SpotData>> spotListByPosition_;
   public final List<Integer> positionsUsed_;

   public SpotsByPosition(Map<Integer, List<SpotData>> spotListByPosition,
           List<Integer> positionsUsed) {
      spotListByPosition_ = spotListByPosition;
      positionsUsed_ = positionsUsed;
   }
   
}
