package jsheets.shell.environment.inprocess;

import jdk.jshell.spi.ExecutionEnv;

interface Tenancy {
  void wrap(ExecutionEnv environment, ThreadGroup group, Runnable task);
}