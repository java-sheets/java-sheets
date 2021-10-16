package jsheets.evaluation.shell.environment.sandbox;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jsheets.evaluation.shell.environment.ClassFileStore;
import jsheets.evaluation.shell.environment.ClassFileStoreLoader;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.evaluation.sandbox.validation.Rule;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class SandboxedEnvironment
  implements ExecutionEnvironment, ExecutionControlProvider {

  public static SandboxedEnvironment create(Collection<Rule> rules) {
    Objects.requireNonNull(rules);
    return new SandboxedEnvironment(() -> SandboxClassFileCheck.of(rules));
  }

  private final Supplier<ClassFileStore> loader;

  private SandboxedEnvironment(Supplier<ClassFileStore> loader) {
    this.loader = loader;
  }

  @Override
  public Installation install() {
    return () -> {};
  }

  @Override
  public ExecutionControlProvider control(String name) {
    return this;
  }

  @Override
  public String name() {
    return "sandbox";
  }

  @Override
  public ExecutionControl generate(
    ExecutionEnv environment,
    Map<String, String> parameters
  ) {
    return new DirectExecutionControl(ClassFileStoreLoader.of(loader.get()));
  }
}
