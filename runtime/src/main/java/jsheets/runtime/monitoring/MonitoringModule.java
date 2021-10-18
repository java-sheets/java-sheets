package jsheets.runtime.monitoring;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

import com.google.common.eventbus.AsyncEventBus;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.influx.InfluxConfig;
import io.micrometer.influx.InfluxMeterRegistry;
import io.soabase.recordbuilder.core.RecordBuilder;

import jsheets.config.Config;
import jsheets.config.Config.Key;
import jsheets.event.EventSink;
import jsheets.event.GuavaEventSink;

import static jsheets.config.Config.Key.ofInt;
import static jsheets.config.Config.Key.ofString;

public final class MonitoringModule extends AbstractModule {
  public static MonitoringModule create() {
    return new MonitoringModule();
  }

  private MonitoringModule() {}

  private final Key<String> monitoringBackendKey = ofString("monitoring.backend");

  @Provides
  @Singleton
  EventSink eventSink(Executor executor, Config config) {
    return selectRegistry(config).map(registry -> {
      var bus = new AsyncEventBus("monitoring", executor);
      bus.register(EvaluationEngineMonitoring.register(registry));
      bus.register(ForkEnvironmentMonitoring.register(registry));
      return GuavaEventSink.forBus(bus);
    }).orElseGet(EventSink::ignore);
  }

  private Optional<MeterRegistry> selectRegistry(Config config) {
    var backend = monitoringBackendKey.in(config).or("").toLowerCase();
    if (backend.startsWith("influx")) {
      return Optional.of(createInfluxRegistry(config));
    }
    return Optional.empty();
  }

  private MeterRegistry createInfluxRegistry(Config config) {
    return new InfluxMeterRegistry(
      FixedInfluxConfig.fromConfig(config),
      Clock.SYSTEM
    );
  }

  @RecordBuilder
  record FixedInfluxConfig(
    String password,
    String org,
    String userName,
    String bucket,
    String uri,
    String db,
    Duration step
  ) implements InfluxConfig {

    static final Key<String> orgKey = ofString("monitoring.influx.org");
    static final Key<String> bucketKey = ofString("monitoring.influx.bucket");
    static final Key<String> userNameKey = ofString("monitoring.influx.userName");
    static final Key<String> passwordKey = ofString("monitoring.influx.password");
    static final Key<String> databaseKey = ofString("monitoring.influx.db");
    static final Key<String> uriKey = ofString("monitoring.influx.uri");
    static final Key<Integer> stepKey = ofInt("monitoring.influx.step");

    private static final String defaultInfluxBucket = "jsheets";
    private static final String defaultDatabase = "jsheets";
    private static final String defaultUri = "http://localhost:8086";
    private static final int defaultStep = 10;

    static InfluxConfig fromConfig(Config config) {
      return MonitoringModuleFixedInfluxConfigBuilder.builder()
        .password(passwordKey.in(config).require())
        .org(orgKey.in(config).or(""))
        .bucket(bucketKey.in(config).or(defaultInfluxBucket))
        .userName(userNameKey.in(config).require())
        .uri(uriKey.in(config).or(defaultUri))
        .db(databaseKey.in(config).or(defaultDatabase))
        .step(Duration.ofSeconds(stepKey.in(config).or(defaultStep)))
        .build();
    }

    @Override
    public String get(String key) {
      return null;
    }
  }
}