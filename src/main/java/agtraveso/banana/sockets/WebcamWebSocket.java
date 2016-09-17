package agtraveso.banana.sockets;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class WebcamWebSocket {

	private static final Logger LOG = LoggerFactory.getLogger(WebcamWebSocket.class);

	@OnWebSocketConnect
	public void onConnect(Session session) {
		LOG.info("webSocket connect from = {}", session.getRemoteAddress().getAddress());
		if (!SessionHandler.getInstance().isSomeoneConnected()) {
			// start webcam, first voyeur!
			WebcamHandler.getInstance().startLiveStream();
		}
		SessionHandler.getInstance().suscribe(session);
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason) {
		LOG.info("webSocket closed from {}, status = {}, reason = {}", session.getRemoteAddress().getAddress(),
				statusCode, reason);
		SessionHandler.getInstance().unsuscribe(session);
		if (!SessionHandler.getInstance().isSomeoneConnected()) {
			// close live streaming, nobody is watching
			WebcamHandler.getInstance().stopLiveStream();
		}
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		LOG.error("webSocket error", t);
		WebcamHandler.getInstance().stopLiveStream();
		SessionHandler.getInstance().shutdown();
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		LOG.info("webSocket message, text = {}", message);

	}

}
