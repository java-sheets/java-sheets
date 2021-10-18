package jsheets.runtime.evaluation;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import jsheets.evaluation.EvaluationEngine;
import jsheets.evaluation.sandbox.access.AccessGraph;
import jsheets.evaluation.sandbox.validation.ForbiddenMemberFilter;
import jsheets.evaluation.shell.ShellEvaluationEngine;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.evaluation.shell.environment.fork.ForkedExecutionEnvironment;
import jsheets.evaluation.shell.environment.sandbox.SandboxClassFileCheck;
import jsheets.evaluation.shell.environment.StandardEnvironment;
import jsheets.config.Config;
import jsheets.evaluation.shell.execution.SystemBasedExecutionMethodFactory;

import java.util.Collection;
import java.util.List;

import static jsheets.runtime.evaluation.EvaluationConfigSource.accessGraphKey;
import static jsheets.runtime.evaluation.EvaluationConfigSource.disableSandboxKey;
import static jsheets.runtime.evaluation.EvaluationConfigSource.virtualMachineOptionsKey;

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
    return ForkedExecutionEnvironment.create(
      SandboxClassFileCheck.of(
        List.of(ForbiddenMemberFilter.create(accessGraph))
      ),
     listVirtualMachineOptions(config)
    );
  }

  Collection<String> listVirtualMachineOptions(Config config) {
    return List.of(
      virtualMachineOptionsKey().in(config).or("").trim().split("\n")
    );
  }
}