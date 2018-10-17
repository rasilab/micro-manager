/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.ucsf.valelab.gaussianfit.fitting;

import edu.ucsf.valelab.gaussianfit.utils.EmpiricalCumulativeDistribution;
import edu.ucsf.valelab.gaussianfit.utils.Gaussian1D;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;

/**
 *
 * @author nico
 */
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

/**
 * Class that uses the apache commons math3 library to fit a Gaussian distribution
 * by optimizing the maximum likelihood function
 * distances.
 * @author nico
 */

public class Gaussian1DecdFitter {
   private final Vector2D[] points_;
   private double muGuess_ = 0.0;
   private double sigmaGuess_ = 10.0;
   
   
    /**
    * Helper class to calculate the least square error between the numerically
    * calculated Gaussian integral for a given mu and sigma, and the experimentally determined
    * cumulative distribution function
   */
   private class Gauss1DIntegralFunc implements MultivariateFunction {
      private final Vector2D[] data_;
      
      public Gauss1DIntegralFunc(Vector2D[] data) {
         data_ = data;
      }
       
      /**
       * 
       * @param input = double[] {mu, sigma}
       * @return least square error with the data
       */
      @Override
      public double value(double[] input) {
         UnivariateFunction function = v ->  Gaussian1D.gaussian(v, input[0], input[1]);
         UnivariateIntegrator in = new SimpsonIntegrator();
         //double maxIntegral = in.integrate(100000000, function, -100.0, 
         //        100.0);
         double maxIntegral = 1.0; // integral of our Gaussian over infinity is always 1
         //  if the range does not capture everything, then we should penalize
         double lsqErrorSum = 0.0d;
         Vector2D previousIntegral = new Vector2D(data_[0].getX() - 1.0, 0.0);
         for (Vector2D d : data_) {
            if (d.getX() <= previousIntegral.getX()) {
               // will happen when same value occurs twice as in bootstrapping
               lsqErrorSum += (previousIntegral.getY() - d.getY()) * 
               (previousIntegral.getY() - d.getY());
            } else {
               double incrementalIntegral = in.integrate(100000000, function,
                       previousIntegral.getX(), d.getX());
               double currentIntegral = previousIntegral.getY() + incrementalIntegral;
               previousIntegral = new Vector2D(d.getX(), currentIntegral);
               double fractionalIntegral = currentIntegral / maxIntegral;
               lsqErrorSum += ( (fractionalIntegral - d.getY()) * (fractionalIntegral - d.getY()) );
            }
         }
         return lsqErrorSum;
      }
      
   }

   
   
   /**
    * Constructor
    * @param points array with data points to be fitted
    */
   public Gaussian1DecdFitter(double[] points) {
      points_ = EmpiricalCumulativeDistribution.calculate(points);
   }
   
   /**
    * Lets caller provide start parameters for fit of mu and sigma
    * @param mu
    * @param sigma 
    */
   public void setStartParams(double mu, double sigma) {
      muGuess_ = mu;
      sigmaGuess_ = sigma;
   }
   
   
   public double[] solve() throws FittingException {
      SimplexOptimizer optimizer = new SimplexOptimizer(1e-9, 1e-12);
      Gauss1DIntegralFunc integralFunction = new Gauss1DIntegralFunc(points_);
      
      // bounds should not be needed. but just in case:
      /*    
      double[] lowerBounds = {-1000.0, 0.0};
      double[] upperBounds = {1000.0, 1000.0};
      MultivariateFunctionMappingAdapter mfma = new MultivariateFunctionMappingAdapter(
              integralFunction, lowerBounds, upperBounds);

      
      // approach: calculate cdf.  Use a Simplexoptimizer to optimize the 
      // least square error function of the numerical calculation of the PDF
      // against the experimental cdf

         PointValuePair solution = optimizer.optimize(
                 new ObjectiveFunction(mfma),
                 new MaxEval(10000),
                 GoalType.MINIMIZE,
                 new InitialGuess(mfma.boundedToUnbounded(new double[]{muGuess_, sigmaGuess_})),
                 new NelderMeadSimplex(new double[]{0.2, 0.2})//,
         );

         return mfma.unboundedToBounded(solution.getPoint());
   }
      */
      

      PointValuePair result = optimizer.optimize(new MaxEval(5000),
              new ObjectiveFunction(integralFunction),
              GoalType.MINIMIZE,
              new InitialGuess(new double[] {muGuess_, sigmaGuess_} ),
              new NelderMeadSimplex(new double[] {0.2, 0.2})
              //, this may work in math4:
             // new SimpleBounds(new double[] {0.0, 0.0}, new double[] {100.0, 50.0}
      );
      
      return result.getPoint();

   }

            
}  
