package jsheets.server;

import com.google.common.flogger.FluentLogger;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import jsheets.server.evaluation.EvaluationEndpoint;
import jsheets.server.sheet.SheetEndpoint;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Injector;

public final class Server {
	private static final FluentLogger log = FluentLogger.forEnclosingClass();

	public record Config(int port) { }

	private final Injector injector;
	private final Config config;
	private final AtomicReference<Javalin> runningServer =
		new AtomicReference<>(null);

	@Inject
	Server(Injector injector, Config config) {
		this.injector = injector;
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
    injector.getInstance(EvaluationEndpoint.class).configure(server);
    injector.getInstance(SheetEndpoint.class).configure(server);
		return server;
	}

	private static void configure(JavalinConfig config) {
		config.showJavalinBanner = false;
	}
}
