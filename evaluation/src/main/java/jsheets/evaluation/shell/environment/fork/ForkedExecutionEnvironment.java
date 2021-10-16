package jsheets.evaluation.shell.environment.fork;

import java.util.Collection;
import java.util.Objects;

import jdk.jshell.spi.ExecutionControlProvider;
import jsheets.evaluation.shell.environment.ClassFileStore;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;

public final class ForkedExecutionEnvironment implements ExecutionEnvironment {
  public static ForkedExecutionEnvironment create(
    ClassFileStore store,
    Collection<String> virtualMachineOptions
  ) {
    Objects.requireNonNull(store, "store");
    Objects.requireNonNull(virtualMachineOptions, "virtualMachineOptions");
    return new ForkedExecutionEnvironment(store, virtualMachineOptions);
  }

  private final ClassFileStore store;
  private final Collection<String> virtualMachineOptions;

  private ForkedExecutionEnvironment(
    ClassFileStore store,
    Collection<String> virtualMachineOptions
  ) {
    this.store = store;
    this.virtualMachineOptions = virtualMachineOptions;
  }

  @Override
  public ExecutionControlProvider control(String name) {
    return ForkingExecutionControlProvider.create(virtualMachineOptions, store);
  }

  @Override
  public Installation install() {
    return () -> {};
  }
}