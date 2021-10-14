package jsheets.runtime.discovery;

import java.util.Objects;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.MetadataKey;
import com.google.common.net.HostAndPort;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

/**
 * Implements a <a href="https://consul.io/">Consul</a>
 * backend for <stron>Service Discovery</stron>.
 * <p>
 * Consul discovery uses additional <italic>gRpc</italic> health checks
 * that can only be used if the {@code Health} Feature is activated.
 */
public final class ConsulServiceAdvertisementChannel implements ServiceAdvertisementChannel {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static ConsulServiceAdvertisementChannel forServiceId(
    String serviceId,
    ConsulClient client
  ) {
    Objects.requireNonNull(serviceId, "serviceId");
    Objects.requireNonNull(client, "client");
    return new ConsulServiceAdvertisementChannel(serviceId, client);
  }

  private final String serviceId;
  private final ConsulClient client;

  private ConsulServiceAdvertisementChannel(String serviceId, ConsulClient client) {
    this.client = client;
    this.serviceId = serviceId;
  }

  private static final MetadataKey<String> idKey =
    MetadataKey.single("serviceId", String.class);

  @Override
  public ServiceAdvertisement advertise(HostAndPort address) {
    var service = createService(address);
    log.atInfo()
      .with(idKey, serviceId)
      .log("advertising service in service discovery");
    client.agentServiceRegister(service);
    return this::remove;
  }

  private void remove() {
    client.agentServiceDeregister(serviceId);
    log.atInfo()
      .with(idKey, serviceId)
      .log("removing service discovery advertisement");
  }

  private NewService createService(HostAndPort address) {
    var service = new NewService();
    service.setId(serviceId);
    service.setPort(address.getPort());
    service.setAddress(address.getHost());
    service.setCheck(createCheck(address));
    return service;
  }

  private static final String updateInterval = "10s";

  private NewService.Check createCheck(HostAndPort address) {
    var check = new NewService.Check();
    check.setGrpc(address.toString());
    check.setInterval(updateInterval);
    return check;
  }

  @Override
  public String toString() {
    return "ConsulServiceAdvertisement(serviceId=%s,client=%s)"
      .formatted(serviceId, client);
  }
}