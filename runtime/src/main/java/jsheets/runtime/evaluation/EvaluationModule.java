package jsheets.runtime.evaluation;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.inject.Named;
import jsheets.evaluation.EvaluationEngine;
import jsheets.evaluation.sandbox.access.AccessGraph;
import jsheets.evaluation.sandbox.validation.ForbiddenMemberFilter;
import jsheets.evaluation.shell.ShellEvaluationEngine;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.evaluation.shell.environment.SandboxedEnvironment;
import jsheets.evaluation.shell.environment.StandardEnvironment;
import jsheets.evaluation.shell.environment.inprocess.EmbeddedEnvironment;
import jsheets.evaluation.shell.environment.inprocess.InProcessExecutionControl;
import jsheets.evaluation.shell.execution.ExecutionMethod;
import jsheets.evaluation.shell.execution.ExhaustiveExecution;
import jsheets.config.Config;
import jsheets.evaluation.shell.execution.SystemBasedExecutionMethodFactory;

import java.util.List;

import static jsheets.runtime.evaluation.SandboxConfigSource.accessGraphKey;
import static jsheets.runtime.evaluation.SandboxConfigSource.disableSandboxKey;

public final class EvaluationModule extends AbstractModule {
  public static EvaluationModule create() {
    return new EvaluationModule();
  }

  private EvaluationModule() {}

  @Provides
  @Singleton
  EvaluationEngine evaluationEngine(ExecutionEnvironment environment) {
    return ShellEvaluationEngine.newBuilder()
      .useEnvironment(environment)
      .useExecutionMethodFactory(SystemBasedExecutionMethodFactory.create())
      .create();
  }

  @Provides
  @Singleton
  ExecutionEnvironment executionEnvironment(Config config) {
    boolean disableSandbox =
      disableSandboxKey().in(config).orNone().orElse(false);
    if (disableSandbox) {
      return StandardEnvironment.create();
    }
    var accessGraphConfig = accessGraphKey().in(config).require();
    var accessGraph = AccessGraph.of(accessGraphConfig.split("\n"));
    return SandboxedEnvironment.create(
      List.of(ForbiddenMemberFilter.create(accessGraph))
    );
  }
}