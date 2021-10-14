package jsheets.evaluation.shell.environment.inprocess;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.flogger.FluentLogger;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionEnv;

/**
 * Controls execution of the JShell in the current JVM.
 */
public final class InProcessExecutionControl extends DirectExecutionControl {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final AtomicReference<ThreadGroup> workerGroupReference =
    new AtomicReference<>(null);

  private final ExecutionEnv environment;
  private final Tenancy tenancy;
  private final String workerGroupName;

  InProcessExecutionControl(
    ExecutionEnv environment,
    Tenancy tenancy,
    String workerGroupName
  ) {
    this.tenancy = tenancy;
    this.environment = environment;
    this.workerGroupName = workerGroupName;
  }

  private static final Object staticReceiver = null;
  private static final Object[] noParameters = new Object[0];

  @Override
  protected String invoke(Method method) {
    var exception = new Capture<Throwable>(null);
    Thread.setDefaultUncaughtExceptionHandler(captureException(exception));
    this.workerGroupReference.set(createWorkerGroup());
    var result = invokeInGroup(method, exception);
    throwCapture(exception);
    return valueString(result);
  }

  private Object invokeInGroup(Method method, Capture<Throwable> exception) {
    return runInParentThread(method)
      .thenRun(this::joinChildThreads)
      .exceptionally(failure -> {
        exception.value = failure;
        return null;
      }).join();
  }

  private ThreadGroup createWorkerGroup() {
    return new ThreadGroup(workerGroupName);
  }

  /*
   * Invokes the method in the parent thread and thus ensures, that all
   * child threads created by the method belong to the workerGroup.
   */
  private <Output> CompletableFuture<Output> runInParentThread(Method method) {
    var future = new CompletableFuture<Output>();
    Runnable task = () -> {
      try {
        @SuppressWarnings("unchecked")
        var result = (Output) method.invoke(staticReceiver, noParameters);
        future.complete(result);
      } catch (Throwable failure) {
        future.completeExceptionally(failure);
      }
    };
    var group = workerGroupReference.get();
    var parent = new Thread(group, () -> tenancy.wrap(environment, group, task));
    parent.start();
    return future;
  }

  private Thread[] listWorkers() {
    // Inherent race condition due to the ancient design of the ThreadGroup.
    var workers = workerGroupReference.get();
    var threads = new Thread[workers.activeCount()];
    workers.enumerate(threads);
    return threads;
  }

  private static final FluentLogger.Api logPreemption = log.atWarning()
    .atMostEvery(5, TimeUnit.SECONDS);

  private void joinChildThreads() {
    boolean wasPreempted = false;
    Throwable failedJoin = null;
    for (var child : listWorkers()) {
      if (child == Thread.currentThread()) {
        continue;
      }
      try {
        child.join();
      } catch (Throwable failure) {
        failedJoin = failure;
        if (isCausedByThreadDeath(failure)) {
          logPreemption.withCause(failure).log("jshell was preempted");
          wasPreempted = true;
        }
      }
    }
    if (wasPreempted) {
      throw new Preemption();
    }
    if (failedJoin != null) {
      throw new RuntimeException(failedJoin);
    }
  }

  @Override
  public void stop() {
    if (!running.compareAndSet(true, false)) {
      throw new IllegalStateException("execution is not running");
    }
    var worker = workerGroupReference.getAndSet(null);
    Objects.requireNonNull(
      workerGroupReference,
      "worker can not be null while running"
    );
    worker.interrupt();
  }

  @Override
  protected void clientCodeEnter() {
    running.set(true);
  }

  @Override
  protected void clientCodeLeave() {
    running.set(false);
  }

  private static final class Capture<Value> {
    volatile Value value;

    Capture(Value initial) {
      this.value = initial;
    }
  }

  private boolean isCausedByThreadDeath(Throwable exception) {
    if (exception instanceof ThreadDeath) {
      return true;
    }
    return exception.getCause() != null && isCausedByThreadDeath(exception.getCause());
  }

  private Thread.UncaughtExceptionHandler captureException(Capture<Throwable> target) {
    return (thread, exception) -> target.value = exception;
  }

  private void throwCapture(Capture<Throwable> exception) {
    if (isCausedByThreadDeath(exception.value)) {
      throw new Preemption();
    }
    if (exception.value != null) {
      throw new RuntimeException(exception.value);
    }
  }
}