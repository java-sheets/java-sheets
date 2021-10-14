package jsheets.runtime;

import java.util.ArrayList;
import java.util.Optional;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.inject.Named;
import jsheets.config.CombinedConfig;
import jsheets.config.Config;
import jsheets.config.EnvironmentConfig;
import jsheets.config.consul.ConsulConfigSource;
import jsheets.runtime.evaluation.SandboxConfigSource;

final class ConfigModule extends AbstractModule {
  static ConfigModule create() {
    return new ConfigModule();
  }

  private ConfigModule() {}

  private static final String environmentPrefix = "JSHELL_RUNTIME";

  @Provides
  @Singleton
  Config createConfig(
    Optional<ConsulConfigSource> consulSource,
    @Named("environment") Config environment
  ) {
    var configs = new ArrayList<Config>();
    configs.add(environment);
    configs.add(SandboxConfigSource.fromClassPath().load());
    consulSource.ifPresent(source -> configs.add(source.load()));
    return CombinedConfig.of(configs.toArray(Config[]::new));
  }

  @Provides
  @Singleton
  @Named("environment")
  Config environmentConfig() {
    return EnvironmentConfig.prefixed(environmentPrefix).load();
  }
}