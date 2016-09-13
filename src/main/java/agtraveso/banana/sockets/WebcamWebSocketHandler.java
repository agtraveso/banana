package agtraveso.banana.sockets;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sarxos.webcam.Webcam;

@WebSocket
public class WebcamWebSocketHandler {

	private static final Logger LOG = LoggerFactory.getLogger(WebcamWebSocketHandler.class);

	private static int EXECUTOR_DELAY_MILLIS = 150;
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private static Set<Session> sessions = new HashSet<Session>();
	private static Webcam webcam;

	static {
		webcam = Webcam.getDefault();
		webcam.setViewSize(new Dimension(640, 480));
	}

	static class BroadCastImageTask implements Runnable {
		private static final BroadCastImageTask INSTANCE = new BroadCastImageTask();

		public static BroadCastImageTask getInstance() {
			return INSTANCE;
		}

		@Override
		public void run() {
			try {
				if (webcam.isOpen()) {
					broadcast(webcam.getImage());
				}
			} catch (Exception e) {
				LOG.error(e.getMessage());
			}
		}
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		LOG.info("webSocket connect from = {}", session.getRemoteAddress().getAddress());

		if (sessions.isEmpty()) {
			// start webcam, first voyeur!
			startLiveStream();
		}

		connect(session);
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		LOG.info("webSocket closed from {}, status = {}, reason = {}", session.getRemoteAddress().getAddress(),
				statusCode, reason);
		disconnect(session);

		if (sessions.isEmpty()) {
			// close webcam, nobody is watching
			stopLiveStream();
		}
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		LOG.error("webSocket error", t);
		teardown();
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		LOG.info("webSocket message, text = {}", message);
	}

	private void startLiveStream() {
		webcam.open(true);
		executor.scheduleAtFixedRate(BroadCastImageTask.getInstance(), 0, EXECUTOR_DELAY_MILLIS, TimeUnit.MILLISECONDS);
	}

	private static void stopLiveStream() {
		webcam.close();
		// executor.shutdown();
	}

	public static void broadcast(BufferedImage image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "JPG", baos);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}

		broadcast(Base64.getEncoder().encodeToString(baos.toByteArray()));
	}

	private static void broadcast(String str) {
		sessions.forEach((session) -> {
			session.getRemote().sendStringByFuture(str);
		});
	}

	private void teardown() {
		stopLiveStream();
		sessions.forEach((session) -> disconnect(session));
	}

	private static void connect(Session session) {
		sessions.add(session);
		LOG.info("active sessions: " + sessions.size());
	}

	private static void disconnect(Session session) {
		// close session first
		session.close();
		// remove safety now
		sessions.remove(session);
		LOG.info("active sessions: " + sessions.size());
	}
}
