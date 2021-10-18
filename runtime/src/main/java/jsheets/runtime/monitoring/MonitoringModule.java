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
import jsheets.config.Config;
import jsheets.event.EventSink;
import jsheets.event.GuavaEventSink;

public final class MonitoringModule extends AbstractModule {
  public static MonitoringModule create() {
    return new MonitoringModule();
  }

  private MonitoringModule() {}

  private final Config.Key<String> monitoringBackendKey =
    Config.Key.ofString("monitoring.backend");

  @Provides
  @Singleton
  private EventSink eventSink(Executor executor, Config config) {
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

  record FixedInfluxConfig(
    String authToken,
    String org,
    String bucket,
    Duration step
  ) implements InfluxConfig {

    private static final Config.Key<String> influxAuthTokenKey =
      Config.Key.ofString("monitoring.influx.authToken");

    private static final Config.Key<Integer> stepKey =
      Config.Key.ofInt("monitoring.influx.step");

    private static final Config.Key<String> influxOrgKey =
      Config.Key.ofString("monitoring.influx.org");

    private static final Config.Key<String> influxBucketKey =
      Config.Key.ofString("monitoring.influx.bucket");

    private static final String defaultInfluxBucket = "jsheets";

    private static final int defaultStep = 10;

    static InfluxConfig fromConfig(Config config) {
      var authToken = influxAuthTokenKey.in(config).require();
      var org = influxOrgKey.in(config).require();
      var bucket = influxBucketKey.in(config).or(defaultInfluxBucket);
      var step = Duration.ofSeconds(stepKey.in(config).or(defaultStep));
      return new FixedInfluxConfig(authToken, org, bucket, step);
    }

    @Override
    public Duration step() {
      return InfluxConfig.super.step();
    }

    @Override
    public String get(String key) {
      return null;
    }
  }
}