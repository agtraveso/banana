package agtraveso.banana.sockets;

import java.awt.Dimension;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;

public class WebcamHandler {

	private static final Logger LOG = LoggerFactory.getLogger(WebcamHandler.class);
	private static WebcamHandler INSTANCE = new WebcamHandler();

	private static int EXECUTOR_DELAY_MILLIS = 150;
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private static Webcam webcam;

	private BroadCastImageTask broadCastImageTask = new BroadCastImageTask();

	class BroadCastImageTask implements Runnable {
		@Override
		public void run() {
			try {
				if (webcam.isOpen()) {
					SessionHandler.getInstance().broadcast(webcam.getImage());
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		}
	}

	static {
		webcam = Webcam.getDefault();
		// TODO remove hardcoded
		Dimension dimension = webcam.getViewSizes()[2];
		webcam.setViewSize(new Dimension(dimension.width, dimension.height));
	}

	public static WebcamHandler getInstance() {
		return INSTANCE;
	}

	public void startLiveStream() {
		webcam.open(true);
		if (executor.isShutdown()) {
			executor = Executors.newScheduledThreadPool(1);
		}
		executor.scheduleAtFixedRate(this.broadCastImageTask, 0, EXECUTOR_DELAY_MILLIS, TimeUnit.MILLISECONDS);
	}

	public void stopLiveStream() {
		if (webcam.isOpen()) {
			webcam.close();
		}
		if (!executor.isShutdown()) {
			executor.shutdown();
		}
	}

}
