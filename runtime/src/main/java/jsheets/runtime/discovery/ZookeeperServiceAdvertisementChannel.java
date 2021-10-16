package jsheets.runtime.discovery;

import com.google.common.flogger.FluentLogger;
import com.google.common.net.HostAndPort;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;

public final class ZookeeperServiceAdvertisementChannel
  implements ServiceAdvertisementChannel {

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static ServiceAdvertisementChannel create(CuratorFramework curator) {
    var discovery = ServiceDiscoveryBuilder.builder(Void.class)
      .basePath("jsheets/services")
      .client(curator)
      .build();
    return new ZookeeperServiceAdvertisementChannel(discovery);
  }

  private final ServiceDiscovery<Void> discovery;

  private ZookeeperServiceAdvertisementChannel(ServiceDiscovery<Void> discovery) {
    this.discovery = discovery;
  }

  @Override
  public void open() {
    try {
      discovery.start();
    } catch (Exception failure) {
      throw new RuntimeException();
    }
  }

  @Override
  public void close() {
    try {
      discovery.close();
    } catch (Exception failure) {
      log.atWarning().withCause(failure).log("failed to close discovery");
    }
  }

  @Override
  public ServiceAdvertisement advertise(String serviceId, HostAndPort address) {
    var service = createServiceInstance(serviceId, address);
    try {
      discovery.registerService(service);
    } catch (Exception failedRegistration) {
      throw new RuntimeException(failedRegistration);
    }
    return () -> {
      try {
        discovery.unregisterService(service);
      } catch (Exception failure) {
        log.atWarning().withCause(failure).log("failed to unregister service");
      }
    };
  }

  private ServiceInstance<Void> createServiceInstance(
    String serviceId,
    HostAndPort address
  ) {
    return new ServiceInstance<>(
      /* name */ "runtime",
      /* id */ serviceId,
      /* address */ address.getHost(),
      /* port */ address.getPort(),
      /* ssl port */ address.getPort(),
      /* payload */ null,
      /* registrationTimeUTC */ System.currentTimeMillis(),
      /* serviceType */ ServiceType.DYNAMIC,
      /* uri */ null
    );
  }
}
