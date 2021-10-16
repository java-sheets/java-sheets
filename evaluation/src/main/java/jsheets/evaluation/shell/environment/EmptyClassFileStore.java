package jsheets.evaluation.shell.environment;

import jdk.jshell.spi.ExecutionControl;

public final class EmptyClassFileStore implements ClassFileStore {
  public static EmptyClassFileStore create() {
    return new EmptyClassFileStore();
  }

  private EmptyClassFileStore() {}

  @Override
  public void load(ExecutionControl.ClassBytecodes[] bytecodes) {}

  @Override
  public void redefine(ExecutionControl.ClassBytecodes[] bytecodes) {}
}