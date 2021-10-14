package jsheets.runtime.discovery;

import com.google.common.net.HostAndPort;

public interface ServiceAdvertisementChannel {
  ServiceAdvertisement advertise(HostAndPort address);
}
