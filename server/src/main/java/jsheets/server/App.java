package jsheets.server;

import com.google.inject.Guice;
import com.google.inject.Injector;

public final class App {
  private App() {}

  public static void main(String[] arguments) {
		configureLogging();
		var injector = configureInjector();
		var server = injector.getInstance(Server.class);
		server.start();
  }

  private static Injector configureInjector() {
  	return Guice.createInjector(ServerModule.create());
	}

  private static void configureLogging() {
		System.setProperty(
			"flogger.backend_factory",
			"com.google.common.flogger.backend.slf4j.Slf4jBackendFactory#getInstance"
		);
	}
}