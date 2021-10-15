package jsheets.server.evaluation.client;

import java.util.Optional;

import com.ecwid.consul.v1.ConsulClient;
import jsheets.evaluation.EvaluationEngine;

public final class ConsulServiceDiscovery implements EnginePool {
  private final ConsulClient client;

  private ConsulServiceDiscovery(ConsulClient client) {
    this.client = client;
  }

  @Override
  public Optional<EvaluationEngine> select() {
    return Optional.empty();
  }
}