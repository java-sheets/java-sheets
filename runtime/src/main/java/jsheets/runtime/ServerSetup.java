package jsheets.runtime;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.soabase.recordbuilder.core.RecordBuilder;
import javax.inject.Inject;
import javax.inject.Named;

public final class ServerSetup {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public enum Feature {
    Reflection,
    Health
  }

  @RecordBuilder
  public record Options(int port, Set<Feature> features) {}

  public interface Hook {
    void start();
    void stop();
  }

  private final Options options;
  private final ServerServiceDefinition runtimeService;
  private final HealthStatusManager health = new HealthStatusManager();
  private final AtomicReference<Server> activeServer = new AtomicReference<>(null);
  private final Collection<Hook> hooks;

  @Inject
  ServerSetup(
    Options options,
    Collection<Hook> hooks,
    @Named("runtimeService") ServerServiceDefinition runtimeService
  ) {
    this.hooks = hooks;
    this.options = options;
    this.runtimeService = runtimeService;
  }

  public void start() throws IOException, InterruptedException {
    log.atConfig().log("configuring server");
    var server = createServer();
    if (!activeServer.compareAndSet(null, server)) {
      throw new IllegalStateException("already running");
    }
    boot(server);
    try {
      server.awaitTermination();
    } finally {
      callStopHooks();
    }
  }

  private static final MetadataKey<Integer> portKey =
    MetadataKey.single("port", Integer.class);

  private void boot(Server server) throws IOException {
    server.start();
    log.atInfo().with(portKey, options.port).log("listening for requests");
    callStartHooks();
    updateHealth(HealthCheckResponse.ServingStatus.SERVING);
    log.atConfig().log("finished boot");
  }

  private void callStartHooks() {
    for (var hook : hooks) {
      try {
        log.atConfig().log("calling start() in hook %s", hook);
        hook.start();
      } catch (Throwable failure) {
        log.atWarning().withCause(failure).log(
          "error while calling start() in hook %s",
          hook
        );
      }
    }
  }

  private void callStopHooks() {
    for (var hook : hooks) {
      try {
        log.atConfig().log("calling stop() in hook %s", hook);
        hook.stop();
      } catch (Throwable failure) {
        log.atWarning().withCause(failure).log(
          "error while calling stop() in hook %s",
          hook
        );
      }
    }
  }

  private void updateHealth(HealthCheckResponse.ServingStatus status) {
    health.setStatus(runtimeService.getServiceDescriptor().getName(), status);
  }

  private Server createServer() {
    var server = ServerBuilder.forPort(options.port);
    server.addService(runtimeService);
    addOptionalServices(server);
    return server.build();
  }

  private void addOptionalServices(ServerBuilder<?> server) {
    if (options.features().contains(Feature.Reflection)) {
      log.atConfig().log("the grpc reflection-service has been enabled");
      server.addService(ProtoReflectionService.newInstance());
    }
    if (options.features().contains(Feature.Health)) {
      log.atConfig().log("the grpc health-service has been enabled");
      updateHealth(HealthCheckResponse.ServingStatus.NOT_SERVING);
      server.addService(health.getHealthService());
    }
  }
}