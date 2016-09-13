package agtraveso.banana;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFiles;
import static spark.Spark.webSocket;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agtraveso.banana.sockets.WebcamWebSocketHandler;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

public class Application {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	private static void init() {
		// handle all exceptions
		exception(Exception.class, (e, req, res) -> e.printStackTrace());
		port(3000);
		staticFiles.location("/public");
	}

	private static String render(String view, Map<String, Object> model) {
		return new VelocityTemplateEngine().render(new ModelAndView(model, view));
	}

	public static void main(String[] args) {
		LOG.info("launchin banana!");
		init();
		webSocket("/webcam", WebcamWebSocketHandler.class);

		get("/", (req, res) -> render("velocity/index.vm", new HashMap<String, Object>()));
	}
}
