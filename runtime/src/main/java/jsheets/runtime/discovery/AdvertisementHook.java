package jsheets.runtime.discovery;

import java.util.Objects;

import com.google.common.net.HostAndPort;

import javax.inject.Inject;
import jsheets.runtime.ServerSetup;

public final class AdvertisementHook implements ServerSetup.Hook {
  public static AdvertisementHook create(
    HostAndPort advertisedHost,
    ServiceAdvertisementChannel channel
  ) {
    Objects.requireNonNull(advertisedHost, "advertisedHost");
    Objects.requireNonNull(channel, "channel");
    return new AdvertisementHook(advertisedHost, channel);
  }

  private final HostAndPort advertisedHost;
  private final ServiceAdvertisementChannel advertisementChannel;
  private volatile ServiceAdvertisement advertisement;

  @Inject
  AdvertisementHook(
    HostAndPort advertisedHost,
    ServiceAdvertisementChannel advertisementChannel
  ) {
    this.advertisedHost = advertisedHost;
    this.advertisementChannel = advertisementChannel;
  }

  @Override
  public void start() {
    advertisement = advertisementChannel.advertise(advertisedHost);
  }

  @Override
  public void stop() {
    var currentAdvertisement = advertisement;
    if (currentAdvertisement != null) {
      currentAdvertisement.remove();
      advertisement = null;
    }
  }

  @Override
  public String toString() {
    return "AdvertisementHook(host=%s)".formatted(advertisedHost);
  }
}