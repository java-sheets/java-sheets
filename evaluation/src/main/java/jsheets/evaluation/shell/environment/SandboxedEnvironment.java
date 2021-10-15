package jsheets.evaluation.shell.environment;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jsheets.evaluation.sandbox.SandboxLoader;
import jsheets.evaluation.sandbox.validation.Rule;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class SandboxedEnvironment
  implements ExecutionEnvironment, ExecutionControlProvider {

  public static SandboxedEnvironment create(Collection<Rule> rules) {
    Objects.requireNonNull(rules);
    return new SandboxedEnvironment(() -> SandboxLoader.create(rules));
  }

  private final Supplier<SandboxLoader> loader;

  private SandboxedEnvironment(Supplier<SandboxLoader> loader) {
    this.loader = loader;
  }

  @Override
  public Installation install() {
    return loader.get().install()::run;
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
    return new DirectExecutionControl(loader.get());
  }
}
