package jsheets.runtime.evaluation.shell.environment;

import com.google.common.flogger.FluentLogger;
import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControlProvider;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executing multiple shells in a shared process can have many negative
 * side effects because the java library is generally not designed in a
 * way to support multi tenancy.
 */
public final class InProcessExecution implements ExecutionEnvironment {
	@Override
	public ExecutionControlProvider control() {
		return null;
	}

	interface Tenancy {
		PrintStream createErrorOutput(ThreadGroup group);
		PrintStream createStandardOutput(ThreadGroup group);
	}

	public static final class MultiTenancy implements Tenancy {
		public static MultiTenancy create() {
			return new MultiTenancy();
		}

		private MultiTenancy() {}

		@Override
		public PrintStream createErrorOutput(ThreadGroup group) {
			return TenantBasedOutput.currentError()
				.map(output -> {
					output.registerGroup(group.getName(), message -> {});
					return (PrintStream) output;
				}).orElse(System.err);
		}

		@Override
		public PrintStream createStandardOutput(ThreadGroup group) {
			return TenantBasedOutput.currentOutput()
				.map(output -> {
					output.registerGroup(group.getName(), message -> {});
					return (PrintStream) output;
				}).orElse(System.out);
		}
	}

	/**
	 * Thrown when the execution is preempted using {@link Control#stop()}.
	 */
	public static final class Preemption extends RuntimeException {
		Preemption() {}
	}

	/**
	 * Controls execution of the JShell in the current JVM.
	 */
	public static final class Control extends DirectExecutionControl {
		private static final FluentLogger log = FluentLogger.forEnclosingClass();

		private final AtomicBoolean running = new AtomicBoolean(false);
		private final AtomicReference<ThreadGroup> workerGroupReference =
			new AtomicReference<>(null);

		private final String workerGroupName;

		private Control(String workerGroupName) {
			this.workerGroupName = workerGroupName;
		}

		private Control(String workerGroupName, LoaderDelegate loaderDelegate) {
			super(loaderDelegate);
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
			var parent = new Thread(workerGroupReference.get(), () -> {
				try {
					@SuppressWarnings("unchecked")
					var result = (Output) method.invoke(staticReceiver, noParameters);
					future.complete(result);
				} catch (Throwable failure) {
					future.completeExceptionally(failure);
				}
			});
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

		private static final FluentLogger.Api logPreemption =
			log.atWarning().atMostEvery(5, TimeUnit.SECONDS);

		private void joinChildThreads() {
			boolean wasPreempted = false;
			Throwable failedJoin = null;
			for (var child : listWorkers()) {
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
		@SuppressWarnings("deprecated")
		public void stop() {
			if (!running.compareAndSet(true, false)) {
				throw new IllegalStateException("execution is not running");
			}
			var worker = workerGroupReference.getAndSet(null);
			Objects.requireNonNull(workerGroupReference, "worker can not be null while running");
			worker.stop();
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
			return exception.getCause() != null
				&& isCausedByThreadDeath(exception.getCause());
		}

		private Thread.UncaughtExceptionHandler captureException(
			Capture<Throwable> target
		) {
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
}
