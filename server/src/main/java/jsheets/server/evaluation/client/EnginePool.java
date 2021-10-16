package jsheets.server.evaluation.client;

import java.util.Optional;

import jsheets.evaluation.EvaluationEngine;

public interface EnginePool {
  Optional<EvaluationEngine> select();
}
