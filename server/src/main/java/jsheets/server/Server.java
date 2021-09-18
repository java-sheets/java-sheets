package jsheets.server;

import com.google.common.flogger.FluentLogger;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.StaticFileConfig;
import jsheets.server.sheet.SheetEndpoint;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class Server {
	private static final FluentLogger log = FluentLogger.forEnclosingClass();

	public record Config(int port) { }

	private final SheetEndpoint sheetEndpoint;
	private final Config config;
	private final AtomicReference<Javalin> runningServer =
		new AtomicReference<>(null);

	@Inject
	Server(SheetEndpoint sheetEndpoint, Config config) {
		this.sheetEndpoint = sheetEndpoint;
		this.config = config;
	}

	public void start() {
		var server = createServer();
		if (!runningServer.compareAndSet(null, server)) {
			throw new IllegalStateException("already running");
		}
		log.atInfo().log("starting...");
		server.start(config.port());
	}

	public void stop() {
		var server = runningServer.getAndSet(null);
		if (server != null) {
			log.atInfo().log("stopping...");
			server.stop();
		}
	}

	private Javalin createServer() {
		var server = Javalin.create(Server::configure);
		sheetEndpoint.configure(server);
		return server;
	}

	private static void configure(JavalinConfig config) {
		config.showJavalinBanner = false;
		config.addStaticFiles(Server::configureStaticFiles);
	}

	private static void configureStaticFiles(StaticFileConfig config) {
		config.hostedPath = "/";
		config.directory = pathToStaticFiles();
		config.location = Location.EXTERNAL;
		config.precompress = shouldCacheStaticFiles();
		config.headers = Map.of();
	}

	private static boolean shouldCacheStaticFiles() {
		return Boolean.parseBoolean(
			Objects.requireNonNullElse(
				System.getenv("JSHEETS_SERVER_CACHE_STATIC_FILES"),
				"false"
			)
		);
	}

	private static String pathToStaticFiles() {
		return Objects.requireNonNullElse(
			System.getenv("JSHEETS_SERVER_STATIC_FILES_PATH"),
			"/static"
		);
	}
}
