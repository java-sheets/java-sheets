package jsheets.runtime.evaluation.shell.environment.inprocess;

import java.io.PrintStream;
import java.util.function.Consumer;

import jdk.jshell.spi.ExecutionEnv;

public final class MultiTenancy implements Tenancy {
  public static MultiTenancy create() {
    return new MultiTenancy();
  }

  private MultiTenancy() {}

  private void registerErrorOutput(ThreadGroup group, ExecutionEnv environment) {
    TenantBasedOutput.currentError().ifPresent(output ->
      forward(output, group, environment.userErr())
    );
  }

  private void registerStandardOutput(ThreadGroup group, ExecutionEnv environment) {
    TenantBasedOutput.currentOutput().ifPresent(output ->
      forward(output, group, environment.userOut())
    );
  }

  private void forward(
    TenantBasedOutput output,
    ThreadGroup group,
    PrintStream target
  ) {
    output.registerGroup(group.getName(), target::print);
  }

  @Override
  public void wrap(ExecutionEnv environment, ThreadGroup group, Runnable task) {
    registerErrorOutput(group, environment);
    registerStandardOutput(group, environment);
    runAndEnsureThatOutputIsUnmodified(task);
    removeGroups(group);
  }

  private void runAndEnsureThatOutputIsUnmodified(Runnable task) {
    var standard = System.out;
    var error = System.err;
    task.run();
    if (System.out != standard) {
      System.setOut(standard);
    }
    if (System.err != error) {
      System.setErr(error);
    }
  }

  private void removeGroups(ThreadGroup group) {
    Consumer<TenantBasedOutput> remove =
      output -> output.removeGroup(group.getName());
    TenantBasedOutput.currentError().ifPresent(remove);
    TenantBasedOutput.currentOutput().ifPresent(remove);
  }
}