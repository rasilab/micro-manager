
package edu.ucsf.valelab.gaussianfit.fitting;

import edu.ucsf.valelab.gaussianfit.utils.Gaussian1D;
import org.apache.commons.math3.analysis.MultivariateFunction;

/**
 *
 * @author nico
  */
public class Gaussian1DFunc implements MultivariateFunction {
   private final double[] points_;

   /**
    * 
    * @param points array with measurements
    */
   public Gaussian1DFunc(double[] points) {
      points_ = points;
   }
   
   /**
    * Calculate the sum of the likelihood function
    * @param doubles array with parameters, here doubles[0] == mu, 
    * doubles [1] == sigma
    * @return -sum(logP2D(points, mu, sigma));
    */
   @Override
   public double value(double[] doubles) {
      double sum = 0.0;
      for (double point : points_) {
         double predictedValue = Gaussian1D.gaussian(point, doubles[0], doubles[1]);
         sum += Math.log(predictedValue);
      }
      return sum;
   }
  

}