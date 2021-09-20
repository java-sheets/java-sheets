package jsheets.runtime.evaluation.shell.environment;

import jdk.jshell.spi.ExecutionControlProvider;

public interface ExecutionEnvironment {
  ExecutionControlProvider control(String name);
  Installation install();

  interface Installation extends AutoCloseable {
    void close();
  }
}
