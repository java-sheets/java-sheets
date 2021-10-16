package jsheets.server.evaluation.client;

import com.google.common.flogger.FluentLogger;
import io.grpc.ManagedChannelBuilder;
import jsheets.evaluation.EvaluationEngine;
import org.apache.curator.x.discovery.ServiceProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// TODO: Pool Connections
public final class ZookeeperEngineDiscovery implements EnginePool {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static ZookeeperEngineDiscovery create(ServiceProvider<Void> services) {
    Objects.requireNonNull(services, "services");
    return new ZookeeperEngineDiscovery(services);
  }

  private final ServiceProvider<Void> services;

  private ZookeeperEngineDiscovery(ServiceProvider<Void> services) {
    this.services = services;
  }

  @Override
  public Optional<EvaluationEngine> select() {
    try {
      var service = services.getInstance();
      var channel = ManagedChannelBuilder.forAddress(service.getAddress(), service.getPort())
        .usePlaintext()
        .build();
      return Optional.of(SnippetRuntimeEngine.forChannel(channel));
    } catch (Exception failure) {
      log.atWarning()
        .withCause(failure)
        .atMostEvery(5, TimeUnit.SECONDS)
        .log("failed to find runtime instance");
      return Optional.empty();
    }
  }
}
