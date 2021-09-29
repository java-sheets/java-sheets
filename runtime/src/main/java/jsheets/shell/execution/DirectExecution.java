package jsheets.shell.execution;

import java.util.Collection;
import java.util.Objects;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

public final class DirectExecution implements ExecutionMethod {
  public static DirectExecution create(JShell shell) {
    Objects.requireNonNull(shell, "shell");
    return new DirectExecution(shell);
  }

  private final JShell shell;

  private DirectExecution(JShell shell) {
    this.shell = shell;
  }

  @Override
  public Collection<SnippetEvent> execute(String source) {
    return shell.eval(source);
  }

  @Override
  public String toString() {
    return "DirectExecution(shell=%s)".formatted(shell);
  }
}