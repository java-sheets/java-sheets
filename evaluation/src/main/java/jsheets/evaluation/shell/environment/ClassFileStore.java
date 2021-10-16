package jsheets.evaluation.shell.environment;

import jdk.jshell.spi.ExecutionControl;

public interface ClassFileStore {
  void redefine(ExecutionControl.ClassBytecodes[] bytecodes);
  void load(ExecutionControl.ClassBytecodes[] bytecodes);
}
