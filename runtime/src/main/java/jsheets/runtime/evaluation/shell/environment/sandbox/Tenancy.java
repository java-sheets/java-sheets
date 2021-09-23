package jsheets.runtime.evaluation.shell.environment.sandbox;

import jdk.jshell.spi.ExecutionEnv;

interface Tenancy {
  void wrap(ExecutionEnv environment, ThreadGroup group, Runnable task);
}