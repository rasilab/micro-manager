///////////////////////////////////////////////////////////////////////////////
//FILE:          ShadingProcessor.java
//PROJECT:       Micro-Manager  
//SUBSYSTEM:     MultiChannelShading plugin
//-----------------------------------------------------------------------------
//
// AUTHOR:       Kurt Thorn, Nico Stuurman
//
// COPYRIGHT:    University of California, San Francisco 2014
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

package org.micromanager.multichannelshading;


import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import mmcorej.Configuration;
import mmcorej.PropertySetting;
import net.haesleinhuepf.clij.clearcl.ClearCL;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.ClearCLContext;
import net.haesleinhuepf.clij.clearcl.ClearCLDevice;
import net.haesleinhuepf.clij.clearcl.backend.ClearCLBackendInterface;
import net.haesleinhuepf.clij.clearcl.backend.ClearCLBackends;
import net.haesleinhuepf.clij.clearcl.ocllib.OCLlib;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;

import org.micromanager.data.Image;
import org.micromanager.data.Metadata;
import org.micromanager.data.Processor;
import org.micromanager.data.ProcessorContext;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.clops.CLKernelException;
import org.micromanager.clops.CLKernelExecutor;
import org.micromanager.clops.Kernels;
import org.micromanager.data.internal.DefaultImage;

/**
 *
 * @author nico, modified for MM2.0 by Chris Weisiger
 */
public class ShadingProcessor extends Processor {

   private final Studio studio_;
   private final String channelGroup_;
   private Boolean useOpenCL_;
   private final List<String> presets_;
   private final ImageCollection imageCollection_;
   private CLKernelExecutor gCLKE_;
   private ClearCLContext gCLContext_;
   private ClearCLBuffer clImg_;
   private ClearCLBuffer clDestImg_;

   public ShadingProcessor(Studio studio, String channelGroup,
           Boolean useOpenCL, String backgroundFile, List<String> presets,
           List<String> files) {
      studio_ = studio;
      channelGroup_ = channelGroup;
      useOpenCL_ = useOpenCL;
      if (useOpenCL_) {
         ClearCLBackendInterface lClearCLBackend
              = ClearCLBackends.getBestBackend();

          ClearCL lClearCL = new ClearCL(lClearCLBackend);

   
         ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

         if (lBestGPUDevice == null) { // assume that is what is returned if there is no GPU
            useOpenCL_ = false;
         } else {
            try {
               gCLContext_ = lBestGPUDevice.createContext();
               gCLKE_ = new CLKernelExecutor(gCLContext_, OCLlib.class);
            } catch (IOException ioe) {
               studio_.alerts().postAlert(MultiChannelShading.MENUNAME, this.getClass(), 
                       "Failed to initialize OpenCL, falling back");
               useOpenCL_ = false;
            }
         }
      }
      presets_ = presets;
      imageCollection_ = new ImageCollection(studio_);
      if (backgroundFile != null && !backgroundFile.equals("")) {
         try {
            imageCollection_.setBackground(backgroundFile);
         } catch (ShadingException e) {
            studio_.logs().logError(e, "Unable to set background file to " + backgroundFile);
         }
      }
      try {
         for (int i = 0; i < presets.size(); ++i) {
            imageCollection_.addFlatField(presets.get(i), files.get(i));
         }
      } catch (ShadingException e) {
         studio_.logs().logError(e, "Error recreating ImageCollection");
      }

   }

   // Classes used to classify alerts in the processImage function below
   private class Not8or16BitClass {   }

   private class NoBinningInfoClass {   }

   private class NoRoiClass {   }

   private class NoBackgroundForThisBinModeClass {   }

   private class ErrorSubtractingClass {   }
   
   private class ErrorInOpenCLClass {}

   @Override
   public void processImage(Image image, ProcessorContext context) {
      int width = image.getWidth();
      int height = image.getHeight();

      // For now, this plugin only works with 8 or 16 bit grayscale images
      if (image.getNumComponents() > 1 || image.getBytesPerPixel() > 2) {
         String msg = "Cannot flatfield correct images other than 8 or 16 bit grayscale";
         studio_.alerts().postAlert(MultiChannelShading.MENUNAME, Not8or16BitClass.class, msg);
         context.outputImage(image);
         return;
      }

      Metadata metadata = image.getMetadata();
      PropertyMap userData = metadata.getUserData();

      Image bgSubtracted = image;
      Image result;

      // subtract background
      Integer binning = metadata.getBinning();
      if (binning == null) {
         String msg = "MultiShadingPlugin: Image metadata did not contain Binning information.";
         studio_.alerts().postAlert(MultiChannelShading.MENUNAME, NoBinningInfoClass.class, msg);
         // Assume binning is 1
         binning = 1;
      }
      Rectangle rect = metadata.getROI();
      if (rect == null) {
         String msg = "MultiShadingPlugin: Image metadata did not list ROI.";
         studio_.alerts().postAlert(MultiChannelShading.MENUNAME, NoRoiClass.class, msg);
      }
      ImagePlusInfo background = null;
      try {
         background = imageCollection_.getBackground(binning, rect);
      } catch (ShadingException e) {
         String msg = "Error getting background for bin mode " + binning + " and rect " + rect;
         studio_.alerts().postAlert(MultiChannelShading.MENUNAME,
                 NoBackgroundForThisBinModeClass.class, msg);
      }

      ImagePlusInfo flatFieldImage = getMatchingFlatFieldImage(
              metadata, binning, rect);

      if (useOpenCL_) {
         try {
            ClearCLBuffer clBackground, clFlatField;
            // buffering of clImg_ and clDestImg_.  TODO: do this more elegantly
            if (image.getBytesPerPixel() == 2) {
               if (clImg_ == null || clImg_.getDimensions()[0] != width || 
                       clImg_.getDimensions()[1] != height || 
                       clImg_.getNativeType() != NativeTypeEnum.UnsignedShort) {
                  if (clImg_ != null) {
                     clImg_.close();
                  }
                  clImg_ = gCLContext_.createBuffer(NativeTypeEnum.UnsignedShort,
                       image.getWidth() * image.getHeight());
               }
               if (clDestImg_ == null || clDestImg_.getDimensions()[0] != width || 
                       clDestImg_.getDimensions()[1] != height || 
                       clDestImg_.getNativeType() != NativeTypeEnum.UnsignedShort) {
                  if (clDestImg_ != null) {
                     clDestImg_.close();
                  }
                  clDestImg_ = gCLContext_.createBuffer(NativeTypeEnum.UnsignedShort,
                       image.getWidth() * image.getHeight());
               }
            } else { //(image.getBytesPerPixel() == 1) 
               if (clImg_ == null || clImg_.getDimensions()[0] != width || 
                       clImg_.getDimensions()[1] != height || 
                       clImg_.getNativeType() != NativeTypeEnum.UnsignedByte) {
                  if (clImg_ != null) {
                     clImg_.close();
                  }
                  clImg_ = gCLContext_.createBuffer(NativeTypeEnum.UnsignedByte,
                       image.getWidth() * image.getHeight());
               }
               if (clDestImg_ == null || clDestImg_.getDimensions()[0] != width || 
                       clDestImg_.getDimensions()[1] != height || 
                       clDestImg_.getNativeType() != NativeTypeEnum.UnsignedByte) {
                  if (clDestImg_ != null) {
                     clDestImg_.close();
                  }
                  clDestImg_ = gCLContext_.createBuffer(NativeTypeEnum.UnsignedByte,
                       image.getWidth() * image.getHeight());
               }
            }


            // copy image to the GPU
            clImg_.readFrom(((DefaultImage) image).getPixelBuffer(), false);
            // process with different kernels depending on availability of flatfield
            // and background:
            if (background != null && flatFieldImage == null) {
               clBackground = background.getCLBuffer(gCLContext_);
               // need to use different kernels for differe types
               Kernels.subtractImages(gCLKE_, clImg_, clBackground, clDestImg_);
            } else if (background == null && flatFieldImage != null) {
               clFlatField = flatFieldImage.getCLBuffer(gCLContext_);
               Kernels.multiplyImages(gCLKE_, clImg_, clFlatField, clDestImg_);
            } else if (background != null && flatFieldImage != null) {
               clBackground = background.getCLBuffer(gCLContext_);
               clFlatField = flatFieldImage.getCLBuffer(gCLContext_);               
               Kernels.subtractImages(gCLKE_, clImg_, clBackground, clDestImg_);               
               Kernels.multiplyImages(gCLKE_, clDestImg_, clFlatField, clDestImg_);
            }
            // copy processed image back from the GPU
            clDestImg_.writeTo(((DefaultImage) image).getPixelBuffer(), true);
            // release resources.  If more GPU processing is desired, this should change
            context.outputImage(image);
            return;
         } catch (CLKernelException clke) {
            studio_.alerts().postAlert(MultiChannelShading.MENUNAME,
                    ErrorInOpenCLClass.class,
                    "Error using GPU: " + clke.getMessage());
            useOpenCL_ = false;
         }
      }


      if (background != null) {
         ImageProcessor ip = studio_.data().ij().createProcessor(image);
         ImageProcessor ipBackground = background.getProcessor();
         try {
            ip = ImageUtils.subtractImageProcessors(ip, ipBackground);
            if (userData != null) {
               userData = userData.copy().putBoolean("Background-corrected", true).build();
            }
         } catch (ShadingException e) {
            String msg = "Unable to subtract background: " + e.getMessage();
            studio_.alerts().postAlert(MultiChannelShading.MENUNAME, 
                 ErrorSubtractingClass.class, msg);
         }
         bgSubtracted = studio_.data().ij().createImage(ip, image.getCoords(),
                 metadata.copy().userData(userData).build());
      }


      // do not calculate flat field if we don't have a matching channel;
      // just return the background-subtracted image (which is the unmodified
      // image if we also don't have a background subtraction file).
      if (flatFieldImage == null) {
         context.outputImage(bgSubtracted);
         return;
      }

      if (userData != null) {
         userData = userData.copy().putBoolean("Flatfield-corrected", true).build();
         metadata = metadata.copy().userData(userData).build();
      }
      
      
      
      if (image.getBytesPerPixel() == 1) {
         byte[] newPixels = new byte[width * height];
         byte[] oldPixels = (byte[]) image.getRawPixels();
         int length = oldPixels.length;
         float[] flatFieldPixels = (float[]) flatFieldImage.getProcessor().getPixels();
         for (int index = 0; index < length; index++) {
            float oldPixel = (float) ((int) (oldPixels[index]) & 0x000000ff);
            float newValue = oldPixel * flatFieldPixels[index];
            if (newValue > 2 * Byte.MAX_VALUE) {
               newValue = 2 * Byte.MAX_VALUE;
            }
            newPixels[index] = (byte) (newValue);
         }
         result = studio_.data().createImage(newPixels, width, height,
                 1, 1, image.getCoords(), metadata);
         context.outputImage(result);
      } else if (image.getBytesPerPixel() == 2) {
         short[] newPixels = new short[width * height];
         short[] oldPixels = (short[]) bgSubtracted.getRawPixels();
         int length = oldPixels.length;
         for (int index = 0; index < length; index++) {
            // shorts are signed in java so have to do this conversion to get 
            // the right value
            float oldPixel = (float) ((int) (oldPixels[index]) & 0x0000ffff);
            float newValue = (oldPixel
                    * flatFieldImage.getProcessor().getf(index)) + 0.5f;
            if (newValue > 2 * Short.MAX_VALUE) {
               newValue = 2 * Short.MAX_VALUE;
            }
            newPixels[index] = (short) (((int) newValue) & 0x0000ffff);
         }
         result = studio_.data().createImage(newPixels, width, height,
                 2, 1, image.getCoords(), metadata);
         context.outputImage(result);
      }
   }


   /**
    * Given the metadata of the image currently being processed, find a matching
    * preset from the channelgroup used by the tablemodel
    *
    * @param metadata Metadata of image being processed
    * @return matching flat field image
    */
   ImagePlusInfo getMatchingFlatFieldImage(Metadata metadata, int binning,
           Rectangle rect) {
      PropertyMap scopeData = metadata.getScopeData();
      for (String preset : presets_) {
         try {
            Configuration config = studio_.getCMMCore().getConfigData(
                    channelGroup_, preset);
            boolean presetMatch = true;
            for (int i = 0; i < config.size(); i++) {
               PropertySetting ps = config.getSetting(i);
               String key = ps.getKey();
               String value = ps.getPropertyValue();
               if (scopeData.containsKey(key)
                       && scopeData.getPropertyType(key) == String.class) {
                  String scopeSetting = scopeData.getString(key);
                  if (!value.equals(scopeSetting)) {
                     presetMatch = false;
                     break;
                  }
               }
            }
            if (presetMatch) {
               return imageCollection_.getFlatField(preset, binning, rect);
            }
         } catch (Exception ex) {
            studio_.logs().logError(ex, "Exception in tag matching");
         }
      }
      return null;
   }

   public ImageCollection getImageCollection() {
      return imageCollection_;
   }
   
}
