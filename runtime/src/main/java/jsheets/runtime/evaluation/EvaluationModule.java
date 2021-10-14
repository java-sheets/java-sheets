package jsheets.runtime.evaluation;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.inject.Named;
import jsheets.evaluation.EvaluationEngine;
import jsheets.evaluation.shell.ShellEvaluationEngine;
import jsheets.evaluation.shell.execution.ExecutionMethod;
import jsheets.evaluation.shell.execution.ExhaustiveExecution;
import jsheets.config.Config;

import static jsheets.runtime.evaluation.SandboxConfigSource.accessGraphKey;
import static jsheets.runtime.evaluation.SandboxConfigSource.disableSandboxKey;

public final class EvaluationModule extends AbstractModule {
  public static EvaluationModule create() {
    return new EvaluationModule();
  }

  private EvaluationModule() {}

  @Provides
  @Singleton
  EvaluationEngine evaluationEngine(ExecutionMethod.Factory executionMethodFactory) {
    return ShellEvaluationEngine.newBuilder()
      .useExecutionMethodFactory(executionMethodFactory)
      .create();
  }

  @Provides
  @Singleton
  ExecutionMethod.Factory executionMethodFactory(
    Config config,
    @Named("underlyingExecutionMethod")
    ExecutionMethod.Factory underlyingExecutionMethodFactory
  ) {
    boolean disableSandbox =
      disableSandboxKey().in(config).orNone().orElse(false);
    var accessGraphConfig = accessGraphKey().in(config).require();
    if (disableSandbox) {
      return underlyingExecutionMethodFactory;
    }
    return underlyingExecutionMethodFactory;
  }


  @Named("underlyingExecutionMethod")
  ExecutionMethod.Factory underlyingExecutionMethodFactory(Config config) {
    return ExhaustiveExecution::create;
  }
}