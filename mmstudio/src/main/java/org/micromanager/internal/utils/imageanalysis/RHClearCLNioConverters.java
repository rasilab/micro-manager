package org.micromanager.internal.utils.imageanalysis;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.ClearCLImage;
import net.haesleinhuepf.clij.clearcl.ClearCLContext;
import net.haesleinhuepf.clij.clearcl.enums.HostAccessType;
import net.haesleinhuepf.clij.clearcl.enums.ImageChannelDataType;
import net.haesleinhuepf.clij.clearcl.enums.ImageChannelOrder;
import net.haesleinhuepf.clij.clearcl.enums.KernelAccessType;
import net.haesleinhuepf.clij.clearcl.enums.MemAllocMode;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;



/**
 * Since Robert forked ClearCL we need to duplicate the converter code
 * only changing the imports
 * 
 * @author nico
 */
public class RHClearCLNioConverters
{

  /**
   * convertNioTiClearCL
   *
   * Author: @nicost 6 2019
   *
   * @param context
   *          ClearCLContext instance - the thing that knows about the GPU
   * @param source
   *          Java Direct Buffer containing intensity (pixel) data
   * @param dimensions
   *          Dimensions of the image. Should be 2D or 3D (higher?) Mismatch
   *          with the input data may be disastrous
   * @return ClearCLBuffer containing a copy of the input data
   */
  public static ClearCLBuffer convertNioTiClearCLBuffer(ClearCLContext context,
                                                        Buffer source,
                                                        long[] dimensions)
  {
    ClearCLBuffer target;
    NativeTypeEnum type = null;
    if (source instanceof ByteBuffer)
    {
      type = NativeTypeEnum.UnsignedByte;
    }
    else if (source instanceof ShortBuffer)
    {
      type = NativeTypeEnum.UnsignedShort;
    }
    else if (source instanceof FloatBuffer)
    {
      type = NativeTypeEnum.Float;
    } // Todo: other types, exception when type not found
    target = context.createBuffer(MemAllocMode.Best,
                                  HostAccessType.ReadWrite,
                                  KernelAccessType.ReadWrite,
                                  1L,
                                  type,
                                  dimensions);
    target.readFrom(source, true);

    return target;
  }

  public static ClearCLImage convertNioTiClearCLImage(ClearCLContext context,
                                                      Buffer source,
                                                      long[] dimensions)
  {
    ClearCLImage target;
    ImageChannelDataType type = null;
    if (source instanceof ByteBuffer)
    {
      type = ImageChannelDataType.UnsignedInt8;
    }
    else if (source instanceof ShortBuffer)
    {
      type = ImageChannelDataType.UnsignedInt16;
    }
    else if (source instanceof FloatBuffer)
    {
      type = ImageChannelDataType.Float;
    } // Todo: other types, exception when type not found
    target =
           context.createImage(HostAccessType.ReadWrite,
                               KernelAccessType.ReadWrite,
                               ImageChannelOrder.R,
                               type,
                               dimensions);
    target.readFrom(source, true);

    return target;
  }

  /**
   * Copies Intensity data from ClearCLBuffer to Java Direct Buffer Input is
   * either ClearCLBuffer or ClearCLImage Caller will also need dimensions:
   * source.getDimensions()
   * 
   * @param source
   *          ClearCLBuffer to be convert to Java direct Buffer
   * @return Java Direct Buffer
   */
  public static Buffer convertClearCLBufferToNio(ClearCLImageInterface source)
  {
    Buffer buffer = null;
    if (null != source.getNativeType())
      switch (source.getNativeType())
      {
      case UnsignedByte:
        buffer =
               ByteBuffer.allocate((int) (source.getSizeInBytes()
                                          / source.getNativeType()
                                                  .getSizeInBytes()));
        source.writeTo(buffer, true);
        break;
      case UnsignedShort:
        buffer =
               ShortBuffer.allocate((int) (source.getSizeInBytes()
                                           / source.getNativeType()
                                                   .getSizeInBytes()));
        source.writeTo(buffer, true);
        break;
      case Float:
        buffer =
               FloatBuffer.allocate((int) (source.getSizeInBytes()
                                           / source.getNativeType()
                                                   .getSizeInBytes()));
        source.writeTo(buffer, true);
        break;
      default:
        // Todo: other types, exception when type not found
        break;
      }

    return buffer;
  }

}