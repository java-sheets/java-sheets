package jsheets.sandbox;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

import java.util.Map;

public final class SandboxedEnvironment implements ExecutionControlProvider {
  public static SandboxedEnvironment create() {
    return new SandboxedEnvironment();
  }

  private SandboxedEnvironment() {}

  @Override
  public String name() {
    return "sandbox";
  }

  @Override
  public ExecutionControl generate(
    ExecutionEnv environment,
    Map<String, String> parameters
  ) {
    return new DirectExecutionControl(SandboxLoader.create());
  }
}
