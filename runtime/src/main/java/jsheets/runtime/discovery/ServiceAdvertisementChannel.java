package jsheets.runtime.discovery;

import com.google.common.net.HostAndPort;

public interface ServiceAdvertisementChannel {
  ServiceAdvertisement advertise(String serviceId, HostAndPort address);
  void open();
  void close();
}
