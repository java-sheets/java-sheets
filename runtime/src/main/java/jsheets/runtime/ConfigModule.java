package jsheets.runtime;

import java.util.ArrayList;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.inject.Named;
import jsheets.config.CombinedConfig;
import jsheets.config.Config;
import jsheets.config.EnvironmentConfig;
import jsheets.runtime.evaluation.EvaluationConfigSource;

final class ConfigModule extends AbstractModule {
  static ConfigModule create() {
    return new ConfigModule();
  }

  private ConfigModule() {}

  private static final String environmentPrefix = "JSHEETS_RUNTIME";

  @Provides
  @Singleton
  Config createConfig(@Named("environment") Config environment) {
    var configs = new ArrayList<Config>();
    configs.add(environment);
    configs.add(EvaluationConfigSource.fromClassPath().load());
    return CombinedConfig.of(configs.toArray(Config[]::new));
  }

  @Provides
  @Singleton
  @Named("environment")
  Config environmentConfig() {
    return EnvironmentConfig.prefixed(environmentPrefix).load();
  }
}