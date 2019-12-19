package org.npl.biomet.mmsim;

import ij.process.FHT;
import ij.process.ShortProcessor;
import ij.process.ImageProcessor;
import org.micromanager.Studio;
import org.micromanager.data.DataProvider;
import org.micromanager.data.Datastore;
import org.micromanager.data.Image;
import org.micromanager.data.ImageJConverter;
import java.lang.Math;
import java.io.IOException;

public class FFT implements Runnable {

    private ImageJConverter ijconverter;
    private DataProvider imageProvider_;
    private Studio studio_;
    private Datastore outStore_;

    public FFT(Studio studio_){
        this(studio_,null,null);
    }

    public FFT(Studio studio, DataProvider imageProvider,Datastore outStore){
        studio_ = studio;
        ijconverter = studio_.data().getImageJConverter();
        imageProvider_ = imageProvider;
        outStore_ = outStore;
    }


    public Image doFFT(Image image){
        
//		System.out.println("Starting fft");
        Image raw_image = image;
        Image out;

        ImageProcessor raw_image_ip = ijconverter.createProcessor(raw_image);

        ImageProcessor padedraw_image_ip = padPower2(raw_image_ip);

        FHT fht = new FHT(raw_image_ip);
        fht.setShowProgress(false);
        if(fht.powerOf2Size()){

            fht.transform();
            ImageProcessor fft_processor = fht.getPowerSpectrum();
            ImageProcessor out_ip = fft_processor.convertToShort(false);
            out = ijconverter.createImage(out_ip, image.getCoords(), image.getMetadata());
            return out;
        }
        return null;
    }

    private ImageProcessor padPower2(ImageProcessor raw_image_ip) {
        int height = raw_image_ip.getHeight();
        int width = raw_image_ip.getWidth();

        double log2_width = Math.log(width) / Math.log(2);
        double log2_height = Math.log(height) / Math.log(2);

        int pow2_width = (int) Math.pow(2,Math.ceil(log2_width));
        int pow2_height = (int) Math.pow(2,Math.ceil(log2_height));

        if(pow2_width==width &&  pow2_height==height) {
            return raw_image_ip;
        }

        ShortProcessor emptyProcessor = new ShortProcessor(pow2_width, pow2_height);
        ImageProcessor paddedProcessor = emptyProcessor.duplicate();

        int xloc = Math.round(pow2_width / 2 - width / 2);
        int yloc = Math.round(pow2_height / 2 - height / 2);

        paddedProcessor.insert(raw_image_ip,xloc,yloc);

        return paddedProcessor;
    }

    @Override
    public void run() {
        System.out.println("Run");
        boolean interrupt = false;
//        synchronized (this) {
        while (!interrupt) {
                try {
                    Image current_image = imageProvider_.getAnyImage();
                    Image fft_current_image = doFFT(current_image);
                    outStore_.putImage(fft_current_image);
                    interrupt = false;
                    Thread.sleep(0);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    interrupt = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    interrupt = true;
                } catch (InterruptedException e) {
                    System.out.println("End FFT");
                    Thread.currentThread().interrupt();
                    interrupt = true;
                }

            }
//        }
    }
}
