package jsheets.server;

import com.google.common.flogger.FluentLogger;
import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.StaticFileConfig;
import jsheets.server.evaluation.EvaluationEndpoint;
import jsheets.server.sheet.SheetEndpoint;

import javax.inject.Inject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Injector;

public final class Server {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public record Options(int port) { }

  private final Injector injector;
  private final Options options;
  private final AtomicReference<Javalin> runningServer =
    new AtomicReference<>(null);

  @Inject
  Server(Injector injector, Options options) {
    this.injector = injector;
    this.options = options;
  }

  public void start() {
    var server = createServer();
    if (!runningServer.compareAndSet(null, server)) {
      throw new IllegalStateException("already running");
    }
    log.atInfo().log("starting...");
    server.start(options.port());
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
    config.ignoreTrailingSlashes = true;
    config.addSinglePageRoot("/", "/static/index.html", Location.CLASSPATH);
    config.addStaticFiles(Server::configureStaticFiles);
  }

  private static void configureStaticFiles(StaticFileConfig config) {
    config.hostedPath = "/";
    config.directory = "/static";
    config.location = Location.CLASSPATH;
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
}
