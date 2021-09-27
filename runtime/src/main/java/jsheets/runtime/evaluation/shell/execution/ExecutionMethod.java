package jsheets.runtime.evaluation.shell.execution;

import java.util.Collection;

import jdk.jshell.Snippet;

public interface ExecutionMethod {
  Collection<Snippet> execute(String source);
}
