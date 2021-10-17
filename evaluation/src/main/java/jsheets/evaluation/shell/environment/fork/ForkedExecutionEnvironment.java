package jsheets.evaluation.shell.environment.fork;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
    return new ForkedExecutionEnvironment(
      store,
      virtualMachineOptions,
      createDaemonScheduler()
    );
  }

  private static ScheduledExecutorService createDaemonScheduler() {
    var factory = new ThreadFactoryBuilder().setDaemon(true).build();
    return Executors.newScheduledThreadPool(1, factory);
  }

  private final ClassFileStore store;
  private final Collection<String> virtualMachineOptions;
  private final ScheduledExecutorService scheduler;

  private ForkedExecutionEnvironment(
    ClassFileStore store,
    Collection<String> virtualMachineOptions,
    ScheduledExecutorService scheduler
  ) {
    this.store = store;
    this.virtualMachineOptions = virtualMachineOptions;
    this.scheduler = scheduler;
  }

  @Override
  public ExecutionControlProvider control(String name) {
    return ForkingExecutionControlProvider.create(
      virtualMachineOptions,
      store,
      scheduler
    );
  }

  @Override
  public Installation install() {
    return () -> {};
  }
}