package jsheets.config.consul;

import java.util.Objects;

import com.ecwid.consul.v1.ConsulClient;

import jsheets.config.Config;
import jsheets.config.RawConfig;

/**
 * Loads configuration from a <a href="https://consul.io/">Consul</a> backend.
 */
public final class ConsulConfigSource implements Config.Source {
  public static ConsulConfigSource prefixed(String prefix, ConsulClient client) {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(prefix, "prefix");
    return new ConsulConfigSource(prefix + ".", client);
  }

  private final String prefix;
  private final ConsulClient client;

  private ConsulConfigSource(String prefix, ConsulClient client) {
    this.prefix = prefix;
    this.client = client;
  }

  @Override
  public Config load() {
    var pairs = client.getKVValues(prefix).getValue();
    var config = RawConfig.newBuilder();
    for (var pair : pairs) {
      var key = removePrefix(pair.getKey());
      config.withRaw(key, pair.getDecodedValue());
    }
    return config.create();
  }

  private String removePrefix(String key) {
    return key.startsWith(prefix)
      ? key.substring(prefix.length())
      : key;
  }

  @Override
  public String toString() {
    return "ConsulConfigSource(prefix=%s, client=%s)"
      .formatted(prefix, client);
  }
}