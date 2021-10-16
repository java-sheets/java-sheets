package jsheets.runtime.discovery;

import java.util.Objects;

import com.google.common.flogger.FluentLogger;
import com.google.common.net.HostAndPort;

import javax.inject.Inject;
import javax.inject.Named;

import jsheets.runtime.ServerSetup;

public final class AdvertisementHook implements ServerSetup.Hook {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static AdvertisementHook create(
    String serviceId,
    HostAndPort advertisedHost,
    ServiceAdvertisementChannel channel
  ) {
    Objects.requireNonNull(advertisedHost, "advertisedHost");
    Objects.requireNonNull(channel, "channel");
    return new AdvertisementHook(serviceId, advertisedHost, channel);
  }

  private final HostAndPort advertisedHost;
  private final ServiceAdvertisementChannel advertisementChannel;
  private final String serviceId;
  private volatile ServiceAdvertisement advertisement;

  @Inject
  AdvertisementHook(
    @Named("serviceId") String serviceId,
    HostAndPort advertisedHost,
    ServiceAdvertisementChannel advertisementChannel
  ) {
    this.serviceId = serviceId;
    this.advertisedHost = advertisedHost;
    this.advertisementChannel = advertisementChannel;
  }

  @Override
  public void start() {
    try {
      advertisementChannel.open();
      advertisement = advertisementChannel.advertise(serviceId, advertisedHost);
    } catch (Exception failure) {
      log.atSevere().withCause(failure)
        .log("failed to advertise service");
    }
  }

  @Override
  public void stop() {
    var currentAdvertisement = advertisement;
    if (currentAdvertisement != null) {
      currentAdvertisement.remove();
      advertisement = null;
    }
    advertisementChannel.close();
  }

  @Override
  public String toString() {
    return "AdvertisementHook(serviceId=%s, host=%s, channel=%s)"
      .formatted(serviceId, advertisedHost, advertisementChannel);
  }
}