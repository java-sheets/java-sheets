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
import jsheets.event.EventSink;

import java.util.Collection;
import java.util.List;

import static jsheets.runtime.evaluation.EvaluationConfigSource.*;

public final class EvaluationModule extends AbstractModule {
  public static EvaluationModule create() {
    return new EvaluationModule();
  }

  private EvaluationModule() {
  }

  private static final String fallbackDefaultImports = """
    java.lang.*
    java.math.*
    java.time.*
    java.text.*
    java.util.*
    java.util.function.*
    java.util.stream.*
    """;

  @Provides
  @Singleton
  EvaluationEngine evaluationEngine(Config config, ExecutionEnvironment environment) {
    var builtinImports = defaultImportsKey().in(config)
      .or(fallbackDefaultImports)
      .lines()
      .toList();
    return ShellEvaluationEngine.newBuilder()
      .useEnvironment(environment)
      .useExecutionMethodFactory(SystemBasedExecutionMethodFactory.create())
      .useBuiltinImports(builtinImports)
      .create();
  }

  @Provides
  @Singleton
  ExecutionEnvironment executionEnvironment(Config config, EventSink events) {
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
      listVirtualMachineOptions(config),
      events
    );
  }

  Collection<String> listVirtualMachineOptions(Config config) {
    return List.of(
      virtualMachineOptionsKey().in(config).or("").trim().split("\n")
    );
  }
}