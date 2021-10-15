package jsheets.runtime;

import java.util.Optional;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import com.google.inject.Singleton;
import jsheets.config.Config;
import org.apache.curator.framework.CuratorFramework;

import jsheets.runtime.discovery.ServiceAdvertisementChannel;
import jsheets.runtime.discovery.ZookeeperServiceAdvertisementChannel;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ZookeeperModule extends AbstractModule {
  public static ZookeeperModule create() {
    return new ZookeeperModule();
  }

  private ZookeeperModule() {}

  private static final Config.Key<String> connectionStringKey
    = Config.Key.ofString("zookeeper.connectionString");

  private static final Config.Key<Integer> connectionBackoffKey
    = Config.Key.ofInt("zookeeper.connectBackoff");

  private static final int defaultBackoff = 1000;
  private static final int retryLimit = 3;

  @Provides
  @Singleton
  Optional<CuratorFramework> curatorFramework(Config config) {
    return connectionStringKey.in(config).orNone()
      .map(connectionString -> {
        int backoff = connectionBackoffKey.in(config).or(defaultBackoff);
        var client = CuratorFrameworkFactory.newClient(
          connectionString,
          new ExponentialBackoffRetry(backoff, retryLimit)
        );
        client.start();
        Runtime.getRuntime().addShutdownHook(new Thread(client::close));
        return client;
      });
  }

  @Provides
  @Singleton
  Optional<ServiceAdvertisementChannel> serviceAdvertisementChannel(
    Optional<CuratorFramework> curator
  ) {
    return curator.map(ZookeeperServiceAdvertisementChannel::create);
  }
}
