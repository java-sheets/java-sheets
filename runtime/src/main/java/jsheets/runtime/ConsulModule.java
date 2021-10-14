package jsheets.runtime;

import java.util.Optional;

import com.google.common.net.HostAndPort;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.ecwid.consul.v1.ConsulClient;
import javax.inject.Named;
import jsheets.config.Config;
import jsheets.config.consul.ConsulConfigSource;
import jsheets.runtime.discovery.ConsulServiceAdvertisementChannel;
import jsheets.runtime.discovery.ServiceAdvertisementChannel;

final class ConsulModule extends AbstractModule {
  static ConsulModule create() {
    return new ConsulModule();
  }

  private ConsulModule() {}

  private static final Config.Key<HostAndPort> consulEndpointKey =
    Config.Key.of("consul.endpoint", HostAndPort::fromString);

  @Provides
  @Singleton
  Optional<ConsulClient> consulClient(@Named("environment") Config config) {
    return consulEndpointKey.in(config).orNone().map(endpoint ->
      new ConsulClient(endpoint.getHost(), endpoint.getPort())
    );
  }

  private static final String consulKeyPrefix = "jsheets.runtime";

  @Provides
  @Singleton
  Optional<ConsulConfigSource> consulConfigSource(
    Optional<ConsulClient> clientBinding
  ) {
    return clientBinding.map(client ->
      ConsulConfigSource.prefixed(consulKeyPrefix, client)
    );
  }

  @Provides
  @Singleton
  Optional<ServiceAdvertisementChannel> consulAdvertisementChannel(
    @Named("serviceId") String serviceId,
    Optional<ConsulClient> clientBinding
  ) {
    return clientBinding.map(client ->
      ConsulServiceAdvertisementChannel.forServiceId(serviceId, client)
    );
  }
}