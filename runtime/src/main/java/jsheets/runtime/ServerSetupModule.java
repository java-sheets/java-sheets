package jsheets.runtime;

import java.util.*;

import com.google.common.net.HostAndPort;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import io.grpc.ServerServiceDefinition;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import jsheets.config.Config;
import jsheets.runtime.discovery.AdvertisementHook;
import jsheets.runtime.discovery.ServiceAdvertisementChannel;

/* Configures the ServerSetup. */
final class ServerSetupModule extends AbstractModule {
  static ServerSetupModule create() {
    return new ServerSetupModule();
  }

  private ServerSetupModule() {}

  @Provides
  @Singleton
  @Named("runtimeService")
  ServerServiceDefinition snippetRuntimeService(SnippetRuntimeService service) {
    return service.bindService();
  }

  private static final Config.Key<Integer> servicePortKey =
    Config.Key.ofInt("server.port");

  private static final int defaultPort = 8080;

  @Provides
  @Singleton
  ServerSetup.Options serverSetupOptions(Config config) {
    return ServerSetupOptionsBuilder.builder()
      .port(servicePortKey.in(config).orNone().orElse(defaultPort))
      .features(listFeatures(config))
      .build();
  }

  private static final Config.Key<Boolean> healthCheckKey =
    Config.Key.ofFlag("server.features.enableHealthCheck");

  private static final Config.Key<Boolean> grpcReflectionKey =
    Config.Key.ofFlag("server.features.enableGrpcReflection");

  private Set<ServerSetup.Feature> listFeatures(Config config) {
    var features = EnumSet.noneOf(ServerSetup.Feature.class);
    if (healthCheckKey.in(config).orNone().orElse(true)) {
      features.add(ServerSetup.Feature.Health);
    }
    if (grpcReflectionKey.in(config).orNone().orElse(false)) {
      features.add(ServerSetup.Feature.Reflection);
    }
    return features;
  }

  private static final Config.Key<String> serviceIdKey =
    Config.Key.ofString("service.id");

  @Provides
  @Singleton
  @Named("serviceId")
  String serviceId(Config config) {
    return serviceIdKey.in(config).orNone()
      .orElse(UUID.randomUUID().toString());
  }

  private static final Config.Key<HostAndPort> advertisedHostKey =
    Config.Key.of("service.advertisedHost", HostAndPort::fromString);

  @Provides
  @Singleton
  Collection<ServerSetup.Hook> setupHooks(
    Config config,
    Provider<Optional<ServiceAdvertisementChannel>> advertisementChannelFactory
  ) {
    return advertisedHostKey.in(config).orNone().map(host -> {
      var hook = AdvertisementHook.create(
        host,
        advertisementChannelFactory.get().orElseThrow()
      );
      return List.<ServerSetup.Hook>of(hook);
    }).orElse(List.of());
  }
}