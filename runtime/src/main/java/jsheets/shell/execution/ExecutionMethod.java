package jsheets.shell.execution;

import java.util.Collection;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

public interface ExecutionMethod {
  Collection<SnippetEvent> execute(String source);

  interface Factory {
    ExecutionMethod create(JShell shell);
  }
}
