package jsheets.server.evaluation.client;

import jsheets.StartEvaluationRequest;
import jsheets.evaluation.Evaluation;
import jsheets.evaluation.EvaluationEngine;

import javax.inject.Inject;
import java.util.Objects;

public final class PooledEvaluationEngine implements EvaluationEngine {
  public static PooledEvaluationEngine of(EnginePool pool) {
    Objects.requireNonNull(pool, "pool");
    return new PooledEvaluationEngine(pool);
  }
  
  private final EnginePool pool;

  @Inject
  PooledEvaluationEngine(EnginePool pool) {
    this.pool = pool;
  }

  @Override
  public Evaluation start(StartEvaluationRequest request, Evaluation.Listener listener) {
    return pool.select()
      .map(engine -> engine.start(request, listener))
      .orElseGet(() -> reportNoEngineFound(listener));
  }

  private Evaluation reportNoEngineFound(Evaluation.Listener listener) {
    listener.close();
    return () -> {};
  }
}
