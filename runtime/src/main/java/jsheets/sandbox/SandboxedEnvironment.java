package jsheets.sandbox;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jsheets.sandbox.validation.Rule;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class SandboxedEnvironment implements ExecutionControlProvider {
  public static SandboxedEnvironment create(Collection<Rule> rules) {
    Objects.requireNonNull(rules);
    return new SandboxedEnvironment(rules);
  }

  private final Collection<Rule> rules;

  private SandboxedEnvironment(Collection<Rule> rules) {
    this.rules = rules;
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
    return new DirectExecutionControl(SandboxLoader.create(rules));
  }
}
