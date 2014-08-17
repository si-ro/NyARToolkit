package jp.qualitas.nyartoolkit.java3d.utils.webcam;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.j3d.Background;
import javax.media.j3d.Behavior;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnElapsedTime;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

import jp.nyatla.nyartoolkit.core.NyARCode;
import jp.nyatla.nyartoolkit.core.NyARException;
import jp.nyatla.nyartoolkit.core.param.NyARParam;
import jp.nyatla.nyartoolkit.core.types.NyARIntSize;
import jp.nyatla.nyartoolkit.core.types.matrix.NyARDoubleMatrix44;
import jp.nyatla.nyartoolkit.detector.NyARSingleDetectMarker;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamExceptionHandler;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * NyARToolkitと連動したBehaviorを返却するクラスです。
 * 提供できるBehaviorは、BackgroundとTransformgroupです。
 *
 */
public class NyARSingleMarkerBehaviorHolder implements WebcamListener
{
	private NyARParam _cparam;

	private Webcam _capture;
	/**
	 * Scheduled executor acting as timer.
	 */
	private ScheduledExecutorService executor = null;
	/**
	 * Repainter is used to fetch images from camera and force panel repaint
	 * when image is ready.
	 */
	private final ImageUpdater updater;
	/**
	 * Is frames requesting frequency limited? If true, images will be fetched
	 * in configured time intervals. If false, images will be fetched as fast as
	 * camera can serve them.
	 */
	private boolean frequencyLimit = false;
	/**
	 * Frames requesting frequency.
	 */
	private double frequency = 5; // FPS
	/**
	 * Image currently being displayed.
	 */
	private BufferedImage image = null;
	/**
	 * Webcam is currently starting.
	 */
	private volatile boolean starting = false;
	/**
	 * Is there any problem with webcam?
	 */
	private volatile boolean errored = false;
	/**
	 * Painting is paused.
	 */
	private volatile boolean paused = false;
	
	/**
	 * Webcam has been started.
	 */
	private final AtomicBoolean started = new AtomicBoolean(false);
	
	private J3dNyARRaster_RGB _nya_raster;//最大3スレッドで共有されるので、排他制御かけること。

	private NyARSingleDetectMarker _nya;

	//Behaviorホルダ
	private NyARBehavior _nya_behavior;
	private float i_rate;
	private NyARCode i_ar_code;
	private double i_marker_width;
	private NyARSingleMarkerBehaviorListener listener;
	
	public NyARSingleMarkerBehaviorHolder(NyARParam i_cparam, float i_rate, NyARCode i_ar_code, double i_marker_width) throws NyARException
	{
		this.i_rate = i_rate;
		this.i_ar_code = i_ar_code;
		this.i_marker_width = i_marker_width;
		
		this._nya_behavior = null;
		final NyARIntSize scr_size = i_cparam.getScreenSize();
		this._cparam = i_cparam;
		//キャプチャの準備
		Dimension size = WebcamResolution.QVGA.getSize();

		//this._capture = Webcam.getWebcams().get(1);
		this._capture = Webcam.getDefault();
		this._capture.setViewSize(size);
		this._capture.addWebcamListener(this);
		this.updater = new ImageUpdater();
		
		
//		this._nya_raster = new J3dNyARRaster_RGB(scr_size.w, scr_size.h);
//		this._nya =NyARSingleDetectMarker.createInstance(this._cparam, i_ar_code, i_marker_width);
//		this._nya_behavior = new NyARBehavior(this._nya, this._nya_raster, i_rate);
	}

	public Behavior getBehavior()
	{
		return this._nya_behavior;
	}

	/**
	 * i_back_groundにキャプチャ画像を転送するようにBehaviorを設定します。
	 * i_back_groungはALLOW_IMAGE_WRITE属性を持つものである必要があります。
	 * @param i_back_groung
	 * @return
	 */
	public void setBackGround(Background i_back_ground)
	{
		//コール先で排他制御
		this._nya_behavior.setRelatedBackGround(i_back_ground);
	}

	/**
	 * i_trgroupの座標系をマーカーにあわせるようにBehaviorを設定します。
	 *
	 */
	public void setTransformGroup(TransformGroup i_trgroup)
	{
		//コール先で排他制御
		this._nya_behavior.setRelatedTransformGroup(i_trgroup);
	}
	public void setUpdateListener(NyARSingleMarkerBehaviorListener i_listener)
	{
		//コール先で排他制御
		this._nya_behavior.setUpdateListener(i_listener);
	}
	
	/**
	 * 座標系再計算後に呼び出されるリスナです。
	 * @param i_listener
	 */
	public void setWebcapOpenListener(NyARSingleMarkerBehaviorListener i_listener)
	{
		this.listener = i_listener;
	}

//	/**
//	 * ラスタを更新 コールバック関数だから呼んじゃらめえ
//	 */
//	public void onUpdateBuffer(Buffer i_buffer)
//	{
//		try {
//			synchronized (this._nya_raster) {
//				this._nya_raster.setBuffer(i_buffer);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	/**
	 * Open webcam.
	 */
	public void open() {
		System.out.println("trying to open attached webcam");
		this._capture.open();
	}
	/**
	 * start rendering.
	 */
	public void start() throws NyARException {

		if (!started.compareAndSet(false, true)) {
			return;
		}

		this._nya_raster = new J3dNyARRaster_RGB(this._capture.getImage());
		this._nya =NyARSingleDetectMarker.createInstance(this._cparam, i_ar_code, i_marker_width);
		this._nya_behavior = new NyARBehavior(this._nya, this._nya_raster, i_rate);
		
		this._capture.addWebcamListener(this);

		System.out.println("Starting image rendering");

		updater.start();

		this.starting = true;

		try {
			if (!this._capture.isOpen()) {
				errored = !this._capture.open();
			}
		} catch (WebcamException e) {
			errored = true;
			throw new NyARException(e);
		} finally {
			starting = false;
		}
	}

	/**
	 * Stop rendering and close webcam.
	 */
	public void stop() throws NyARException {

		if (!started.compareAndSet(true, false)) {
			return;
		}

		this._capture.removeWebcamListener(this);
		System.out.println("Stopping panel rendering and closing attached webcam");

		try {
			updater.stop();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		image = null;

		try {
			if (this._capture.isOpen()) {
				errored = !this._capture.close();
			}
		} catch (WebcamException e) {
			errored = true;
			throw new NyARException(e);
		}
	}

	public void webcamClosed(WebcamEvent arg0) {
		
	}

	public void webcamDisposed(WebcamEvent arg0) {
		
	}

	public void webcamOpen(WebcamEvent webcamEvent) {
		System.out.println("success opening Webcam.");
		if (listener != null) {
			listener.onWebcamOpen();
		}
	}

	public void webcamImageObtained(WebcamEvent webcamEvent) {

	}
	/**
	 * Is frequency limit enabled?
	 * 
	 * @return True or false
	 */
	public boolean isFPSLimited() {
		return frequencyLimit;
	}
	/**
	 * Image updater reads images from camera and force panel to be repainted.
	 * 
	 * @author Bartosz Firyn (SarXos)
	 */
	private class ImageUpdater implements Runnable {
		/**
		 * Repaint scheduler schedule panel updates.
		 * 
		 * @author Bartosz Firyn (sarxos)
		 */
		private class RepaintScheduler extends Thread {

			/**
			 * Repaint scheduler schedule panel updates.
			 */
			public RepaintScheduler() {
				setUncaughtExceptionHandler(WebcamExceptionHandler.getInstance());
				setName(String.format("repaint-scheduler-%s", _capture.getName()));
				setDaemon(true);
			}

			@Override
			public void run() {

				// do nothing when not running
				if (!running.get()) {
					return;
				}

				// loop when starting, to wait for images
				while (starting) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}

				// schedule update when webcam is open, otherwise schedule
				// second scheduler execution

				try {

					// FPS limit means that panel rendering frequency is
					// limited to the specific value and panel will not be
					// rendered more often then specific value

					if (_capture.isOpen()) {

						// TODO: rename FPS value in panel to rendering
						// frequency

						if (isFPSLimited()) {
							executor.scheduleAtFixedRate(updater, 0, (long) (1000 / frequency), TimeUnit.MILLISECONDS);
						} else {
							executor.scheduleWithFixedDelay(updater, 100, 1, TimeUnit.MILLISECONDS);
						}
					} else {
						executor.schedule(this, 500, TimeUnit.MILLISECONDS);
					}
				} catch (RejectedExecutionException e) {

					// executor has been shut down, which means that someone
					// stopped panel / webcam device before it was actually
					// completely started (it was in "starting" timeframe)

//					LOG.warn("Executor rejected paint update");
//					LOG.trace("Executor rejected paint update because of", e);

					return;
				}
			}
		}
		/**
		 * Update scheduler thread.
		 */
		private Thread scheduler = null;

		/**
		 * Is repainter running?
		 */
		private AtomicBoolean running = new AtomicBoolean(false);

		/**
		 * Start repainter. Can be invoked many times, but only first call will
		 * take effect.
		 */
		public void start() {
			if (running.compareAndSet(false, true)) {
				executor = Executors.newScheduledThreadPool(1, THREAD_FACTORY);
				scheduler = new RepaintScheduler();
				scheduler.start();
			}
		}

		/**
		 * Stop repainter. Can be invoked many times, but only first call will
		 * take effect.
		 * 
		 * @throws InterruptedException
		 */
		public void stop() throws InterruptedException {
			if (running.compareAndSet(true, false)) {
//				executor.shutdown();
//				executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
				scheduler.join();
			}
		}

		public void run() {
			try {
				update();
			} catch (Throwable t) {
				errored = true;
				WebcamExceptionHandler.handle(t);
			}
		}
		private int index = 0;
		/**
		 * Perform single panel area update (repaint newly obtained image).
		 */
		private void update() {

			// do nothing when updater not running, when webcam is closed, or
			// panel repainting is paused

			if (!running.get() || !_capture.isOpen() || paused) {
				return;
			}

			// get new image from webcam

			BufferedImage tmp = _capture.getImage();
			boolean repaint = true;

			if (tmp != null) {

				// ignore repaint if image is the same as before
				if (image == tmp) {
					repaint = false;
				}
				if (repaint) {
					try {
						synchronized (_nya_raster) {
							_nya_raster.setBuffer(tmp);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				errored = false;
				image = tmp;
			}
		}
	}
	/**
	 * Thread factory used by execution service.
	 */
	private static final ThreadFactory THREAD_FACTORY = new PanelThreadFactory();
	private static final class PanelThreadFactory implements ThreadFactory {

		private static final AtomicInteger number = new AtomicInteger(0);

		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, String.format("webcam-panel-scheduled-executor-%d", number.incrementAndGet()));
			t.setUncaughtExceptionHandler(WebcamExceptionHandler.getInstance());
			t.setDaemon(true);
			return t;
		}
	}
}

class NyARBehavior extends Behavior
{
	private NyARDoubleMatrix44 trans_mat_result = new NyARDoubleMatrix44();

	private NyARSingleDetectMarker related_nya;

	private TransformGroup trgroup;

	private Background back_ground;

	private J3dNyARRaster_RGB raster;

	private WakeupCondition wakeup;

	private NyARSingleMarkerBehaviorListener listener;

	public void initialize()
	{
		wakeupOn(wakeup);
	}

	/**
	 * i_related_ic2dの内容で定期的にi_back_groundを更新するBehavior
	 * @param i_back_ground
	 * @param i_related_ic2d
	 */
	public NyARBehavior(NyARSingleDetectMarker i_related_nya, J3dNyARRaster_RGB i_related_raster, float i_rate)
	{
		super();
		wakeup = new WakeupOnElapsedTime((int) (1000 / i_rate));
		related_nya = i_related_nya;
		trgroup = null;
		raster = i_related_raster;
		back_ground = null;
		listener = null;
		this.setSchedulingBounds(new BoundingSphere(new Point3d(), 100.0));
	}

	public void setRelatedBackGround(Background i_back_ground)
	{
		synchronized (raster) {
			back_ground = i_back_ground;
		}
	}

	public void setRelatedTransformGroup(TransformGroup i_trgroup)
	{
		synchronized (raster) {
			trgroup = i_trgroup;
		}
	}
	public void setUpdateListener(NyARSingleMarkerBehaviorListener i_listener)
	{
		if (raster != null) {
			synchronized (raster) {
				listener = i_listener;
			}			
		} else {
			listener = i_listener;
		}
	}

	/**
	 * いわゆるイベントハンドラ
	 */
	public void processStimulus(Enumeration criteria)
	{
		try {
			synchronized (raster) {
				Transform3D t3d = null;
				boolean is_marker_exist = false;
				if (back_ground != null) {
					raster.renewImageComponent2D();/*DirectXモードのときの対策*/
					back_ground.setImage(raster.getImageComponent2D());
				}
				if (raster.hasBuffer()) {
					is_marker_exist = related_nya.detectMarkerLite(raster, 100);
					if (is_marker_exist)
					{
						final NyARDoubleMatrix44 src = this.trans_mat_result;
						related_nya.getTransmationMatrix(src);
//						Matrix4d matrix = new Matrix4d(src.m00, -src.m10, -src.m20, 0, -src.m01, src.m11, src.m21, 0, -src.m02, src.m12, src.m22, 0, -src.m03, src.m13, src.m23, 1);
/*						Matrix4d matrix = new Matrix4d(
								 src.m00, src.m01, src.m02, src.m03,
								-src.m10, -src.m11, -src.m12, -src.m13,
								-src.m20, -src.m21, -src.m22, -src.m23,
								0,0,0, 1.0);*/
/*						Matrix4d matrix2 = new Matrix4d(
						-src.m00, -src.m01, -src.m02, -src.m03,
						-src.m10, -src.m11, -src.m12, -src.m13,
						 src.m20,  src.m21,  src.m22,  src.m23,
					           0,        0,        0,        1);
						*/
						Matrix4d matrix = new Matrix4d(
								-src.m00, -src.m10, src.m20, 0,
								-src.m01, -src.m11, src.m21, 0,
								-src.m02, -src.m12, src.m22, 0,
							    -src.m03,-src.m13, src.m23, 1);
						matrix.transpose();
						t3d = new Transform3D(matrix);
						if (trgroup != null) {
							trgroup.setTransform(t3d);
						}
					}
				}
				if (listener != null) {
					listener.onUpdate(is_marker_exist, t3d);
				}
			}
			wakeupOn(wakeup);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
