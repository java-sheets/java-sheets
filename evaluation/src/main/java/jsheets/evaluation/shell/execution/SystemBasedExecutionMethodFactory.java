package jsheets.evaluation.shell.execution;

import jdk.jshell.JShell;

public final class SystemBasedExecutionMethodFactory
  implements ExecutionMethod.Factory {

  public static SystemBasedExecutionMethodFactory create() {
    return new SystemBasedExecutionMethodFactory();
  }

  private SystemBasedExecutionMethodFactory() {}

  private static final boolean isExhaustiveExecutionSupported =
    ExhaustiveExecution.isSupported();

  @Override
  public ExecutionMethod create(JShell shell) {
    return isExhaustiveExecutionSupported
      ? ExhaustiveExecution.create(shell)
      : DirectExecution.create(shell);
  }
}