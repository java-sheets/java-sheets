package jsheets.server.evaluation.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.flogger.FluentLogger;

import io.grpc.ManagedChannelBuilder;
import jsheets.evaluation.EvaluationEngine;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public final class ZookeeperEngineDiscovery implements EnginePool {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static ZookeeperEngineDiscovery create(
    Executor executor,
    ServiceProvider<Void> services
  ) {
    Objects.requireNonNull(executor, "executor");
    Objects.requireNonNull(services, "services");
    return new ZookeeperEngineDiscovery(executor, services);
  }

  private final ServiceProvider<Void> services;
  private final Executor executor;

  private static final Duration idleTimeout = Duration.ofMinutes(1);
  private final Cache<String, EvaluationEngine> connectionPool =
    CacheBuilder.newBuilder()
      .expireAfterAccess(idleTimeout.toMillis(), TimeUnit.MILLISECONDS)
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .build();

  private ZookeeperEngineDiscovery(
    Executor executor,
    ServiceProvider<Void> services
  ) {
    this.executor = executor;
    this.services = services;
  }

  @Override
  public Optional<EvaluationEngine> select() {
    try {
      var service = services.getInstance();
      var connection = connectionPool.get(service.getId(), () -> connect(service));
      return Optional.of(connection);
    } catch (Exception failure) {
      log.atWarning()
        .withCause(failure)
        .atMostEvery(5, TimeUnit.SECONDS)
        .log("failed to find runtime instance");
      return Optional.empty();
    }
  }

  private EvaluationEngine connect(ServiceInstance<?> target) {
    var channel = ManagedChannelBuilder.forAddress(target.getAddress(), target.getPort())
      .usePlaintext()
      .executor(executor)
      .idleTimeout(idleTimeout.toMillis(), TimeUnit.MILLISECONDS)
      .build();
    return SnippetRuntimeEngine.forChannel(channel);
  }
}
