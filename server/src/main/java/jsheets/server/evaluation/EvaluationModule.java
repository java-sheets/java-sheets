package jsheets.server.evaluation;

import com.google.common.flogger.FluentLogger;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import jsheets.config.Config;
import jsheets.evaluation.EvaluationEngine;
import jsheets.evaluation.shell.ShellEvaluationEngine;
import jsheets.server.evaluation.client.PooledEvaluationEngine;
import jsheets.server.evaluation.client.ZookeeperEngineDiscovery;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceProvider;

import java.util.Optional;
import java.util.concurrent.Executors;

public final class EvaluationModule extends AbstractModule {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  public static EvaluationModule create() {
    return new EvaluationModule();
  }

  private EvaluationModule() {}

  @Provides
  @Singleton
  EvaluationEngine evaluationEngine(
    Optional<CuratorFramework> curatorBinding
  ) {
    return curatorBinding.map(this::createRemoteEvaluationEngine)
      .orElseGet(this::createEmbeddedEvaluationEngine);
  }

  private EvaluationEngine createRemoteEvaluationEngine(CuratorFramework client) {
    var provider = createServiceProvider(client);
    try {
      provider.start();
    } catch (Exception failure) {
      log.atSevere().withCause(failure).log("failed to start service discovery");
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        provider.close();
      } catch (Exception failure) {
        log.atWarning().withCause(failure).log("failed to close service discovery");
      }
    }));
    return PooledEvaluationEngine.of(ZookeeperEngineDiscovery.create(provider));
  }

  private EvaluationEngine createEmbeddedEvaluationEngine() {
    return ShellEvaluationEngine.newBuilder()
      .useWorkerPool(Executors.newCachedThreadPool())
      .create();
  }

  private ServiceProvider<Void> createServiceProvider(CuratorFramework curator) {
    return ServiceDiscoveryBuilder.builder(Void.class)
      .client(curator)
      .basePath("/jsheets/services")
      .build()
      .serviceProviderBuilder()
      .serviceName("runtime")
      .build();
  }

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
}