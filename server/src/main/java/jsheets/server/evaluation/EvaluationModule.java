package jsheets.server.evaluation;

import java.time.Clock;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import jsheets.evaluation.EvaluationEngine;
import jsheets.evaluation.shell.ShellEvaluationEngine;


public final class EvaluationModule extends AbstractModule {
  public static EvaluationModule create() {
    return new EvaluationModule();
  }

  private EvaluationModule() {}

  @Provides
  @Singleton
  EvaluationEngine evaluationEngine(Clock clock) {
    return ShellEvaluationEngine.newBuilder()
      .useWorkerPool(Executors.newCachedThreadPool())
      .useClock(clock)
      .create();
  }
}