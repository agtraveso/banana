package agtraveso.banana.sockets;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(SessionHandler.class);

	private static SessionHandler INSTANCE = new SessionHandler();

	public static SessionHandler getInstance() {
		return INSTANCE;
	}

	private Set<Session> sessions = Collections.synchronizedSet(new HashSet<Session>());

	public void suscribe(Session session) {
		sessions.add(session);
		LOG.info("active sessions: " + sessions.size());
	}

	public void unsuscribe(Session session) {
		sessions.remove(session);
		LOG.info("active sessions: " + sessions.size());
	}

	public boolean isSomeoneConnected() {
		return !sessions.isEmpty();
	}

	public void broadcast(BufferedImage image) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "JPG", baos);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}

		broadcast(Base64.getEncoder().encodeToString(baos.toByteArray()));
	}

	public void broadcast(String str) {
		synchronized (sessions) {
			sessions.forEach((session) -> {
				if (session != null && session.isOpen()) {
					session.getRemote().sendStringByFuture(str);
				}
			});
		}
	}

	public void shutdown() {
		sessions.forEach((session) -> unsuscribe(session));
	}

}
