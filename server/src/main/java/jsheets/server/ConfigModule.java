package jsheets.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import jsheets.config.Config;
import jsheets.config.EnvironmentConfig;

public final class ConfigModule extends AbstractModule {
  public static ConfigModule create() {
    return new ConfigModule();
  }

  private ConfigModule() {}

  private static final String environmentPrefix = "JSHEETS";

  @Provides
  Config config() {
    return EnvironmentConfig.prefixed(environmentPrefix).load();
  }
}
