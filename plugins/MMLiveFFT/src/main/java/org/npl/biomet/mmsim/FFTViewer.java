package org.npl.biomet.mmsim;

//To make this plugin work as a menu plugin and not just a cogwheel plugin it needs to be threaded.

//import com.sun.org.apache.xpath.internal.operations.Bool;
//import org.micromanager.events.
import com.google.common.primitives.Doubles;
import ij.process.FHT;
//import org.micromanager.events.NewDisplayEvent
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
//import org.jtransforms.fft.FloatFFT_2D;
import org.micromanager.SnapLiveManager;
import org.micromanager.Studio;
import org.micromanager.data.*;
import org.micromanager.display.DisplayWindow;
import org.micromanager.events.LiveModeEvent;
import org.micromanager.display.DataViewer;
import com.google.common.eventbus.Subscribe;
import org.micromanager.data.ImageJConverter;
import org.micromanager.events.NewDisplayEvent;
import org.micromanager.events.internal.DefaultLiveModeEvent;

import java.io.IOException;

//import org.micromanager.internal.SnapLiveManager;
public class FFTViewer {
	private final Studio studio_;
	private final SnapLiveManager live_;
	private final FFT fft;

	public DisplayWindow fft_display;
	public Datastore fft_store;
//	private ImageJConverter ijconverter;
	private DisplayWindow live_display;
//	private FloatFFT_2D floatFFT_2D;

	private Image current_image;
	private DataProvider live_provider;
	private DataProvider live_display_provider;
	private Thread fftviewer_thread;
//	private float[][] fft;

	public FFTViewer(Studio studio) {
		this(studio, null,null);
	}

//	public FFTViewer(Studio studio, Datastore fft_store) {
//		this(studio, fft_store, null);
//	}

//	public FFTViewer(Studio studio, DisplayWindow live_display) {
//		this(studio, null, live_display);
//	}

	public FFTViewer(Studio studio, Datastore store, DisplayWindow displayWindow){
		studio_ = studio;
//		ijconverter = studio_.data().getImageJConverter();
		live_ = studio_.live();
		fft_store = studio_.data().createRewritableRAMDatastore();
		fft_store.setName("FFT");
//		fft_store.registerForEvents(this);
		fft_display = studio_.displays().createDisplay(fft_store);

//		fft_display.displayStatusString("FFT");
		if(live_.getIsLiveModeOn()){
			if(displayWindow==null){
				live_display= live_.getDisplay();
			}
			else{
				live_display = displayWindow;
			}
			live_display_provider = live_display.getDataProvider();
			live_display_provider.registerForEvents(this);
		}
		fft = new FFT(studio_,live_display_provider,fft_store);
		fftviewer_thread = new Thread(fft);
		synchronized(fftviewer_thread){

		}
		fftviewer_thread.start();

//		studio_.events().post(new DefaultLiveModeEvent(true));
	}


	@Subscribe
	public void onLiveModeEvent(LiveModeEvent ase) {
		System.out.println("Live on");
//		ase.getIsOn();
//		boolean isOnLive = ase.getIsOn();

///  Code for being a MenuPlugin, doesn't work because the Live event comes before the Dataprovider is built.

//		if (live_.getIsLiveModeOn()) {
////			this
////			live_display = live_.getDisplay();
//			System.out.println(live_display);
//			if(live_display==null) {
////				studio_.events().post(new DefaultLiveModeEvent(true));
//			} else {
//				live_display_provider = live_display.getDataProvider();
//				live_display_provider.registerForEvents(this);
//			}
//		} else {
//			System.out.println("Live off");
////			live_provider.unregisterForEvents(this);
//		}
// This code fails to pause the FFT thread for some reason.
		if (live_.getIsLiveModeOn()) {
			fftviewer_thread.notify();
		} else{
			try {
				fftviewer_thread.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}


///// This event posts before the datastore is availiable so we get a null pointer
//	@Subscribe
//	public void onNewLiveImage(DataProviderHasNewImageEvent e){
////		System.out.println("New image");
//		current_image = e.getImage();
//		try {
//			fft_store.putImage(doFFT(current_image));
//		} catch (IOException ex) {
//			ex.printStackTrace();
//		}
//	}
//	@Override
//	public void run() {
//	    System.out.println("Run");
//		boolean interrupt = false;
//		while (!interrupt) {
//			try {
//                current_image = live_display_provider.getAnyImage();
//                fft_store.putImage(fft.doFFT(current_image));
//                interrupt = false;
//                Thread.sleep(0);
//            } catch(NullPointerException e) {
//                e.printStackTrace();
//                interrupt=false;
//			} catch (IOException e) {
//				e.printStackTrace();
//				interrupt=true;
//			} catch(InterruptedException e){
//				System.out.println("End FFT");
//				interrupt=true;
//			}
//
//		}
//	}
}
