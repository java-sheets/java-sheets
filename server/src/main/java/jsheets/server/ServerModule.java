package jsheets.server;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import jsheets.server.evaluation.EvaluationModule;
import jsheets.server.sheet.SheetModule;

import java.util.OptionalInt;

public final class ServerModule extends AbstractModule {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static ServerModule create() {
    return new ServerModule();
  }

  private ServerModule() {}

  @Override
  protected void configure() {
    install(SheetModule.create());
    install(EvaluationModule.create());
  }

  private static final int defaultPort = 8080;

  @Provides
  Server.Options createConfig() {
    return new Server.Options(
      readIntFromEnvironment("JSHEETS_SERVER_PORT").orElse(defaultPort)
    );
  }

  private static OptionalInt readIntFromEnvironment(String key) {
    var value = System.getenv(key);
    if (value == null) {
      return OptionalInt.empty();
    }
    try {
      return OptionalInt.of(Integer.parseInt(value));
    } catch (NumberFormatException malformedNumber) {
      log.atWarning()
        .withCause(malformedNumber)
        .with(MetadataKey.single("key", String.class), key)
        .log("ignoring variable because it is not an int");
      return OptionalInt.empty();
    }
  }
}
