package jsheets.server;

import com.google.common.flogger.FluentLogger;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;

public final class App {
  private App() {}

  public static void main(String[] arguments) {
		configureLogging();
    FluentLogger.forEnclosingClass()
			.atInfo()
			.log("Hello, World!!!");
		Javalin.create(App::configureServer).start().stop();
  }

  private static void configureLogging() {
		System.setProperty(
			"flogger.backend_factory",
			"com.google.common.flogger.backend.slf4j.Slf4jBackendFactory#getInstance"
		);
	}

  private static void configureServer(JavalinConfig config) {
		config.showJavalinBanner = false;
	}
}