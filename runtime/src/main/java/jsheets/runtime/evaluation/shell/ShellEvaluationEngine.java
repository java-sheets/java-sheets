package jsheets.runtime.evaluation.shell;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jsheets.StartEvaluationRequest;
import jsheets.runtime.evaluation.Evaluation;
import jsheets.runtime.evaluation.EvaluationEngine;
import jsheets.runtime.evaluation.shell.environment.ExecutionEnvironment;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ShellEvaluationEngine implements EvaluationEngine {
	private final Clock clock;
	private final Executor workerPool;
	private final ExecutionEnvironment executionEnvironment;

	private ShellEvaluationEngine(
		Clock clock,
		Executor workerPool,
		ExecutionEnvironment executionEnvironment
	) {
		this.clock = clock;
		this.workerPool = workerPool;
		this.executionEnvironment = executionEnvironment;
	}

	@Override
	public Evaluation start(StartEvaluationRequest request, Evaluation.Listener listener) {
		var evaluation = new ShellEvaluation(clock, executionEnvironment);
		workerPool.execute(() -> evaluation.start(request));
		return evaluation;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static final class Builder {
		private Clock clock = Clock.systemUTC();
		private Executor workerPool;
		private ExecutionEnvironment environment;

		public Builder useClock(Clock clock) {
			Objects.requireNonNull(clock, "clock");
			this.clock = clock;
			return this;
		}

		public Builder useWorkerPool(Executor pool) {
			Objects.requireNonNull(pool, "workerPool");
			workerPool = pool;
			return this;
		}

		public Builder useEnvironment(ExecutionEnvironment environment) {
			Objects.requireNonNull(environment, "environment");
			this.environment = environment;
			return this;
		}

		public EvaluationEngine create() {
			return new ShellEvaluationEngine(
				clock,
				selectWorkerPool(),
				selectEnvironment()
			);
		}

		private ExecutionEnvironment selectEnvironment() {
			return Objects.requireNonNull(environment, "environment");
		}

		private Executor selectWorkerPool() {
			return workerPool == null ? createDefaultWorkerPool() : workerPool;
		}

		private Executor createDefaultWorkerPool() {
			return Executors.newCachedThreadPool(
				new ThreadFactoryBuilder()
					.setDaemon(true)
					.setNameFormat("shell-evaluation-executor-%d")
					.build()
			);
		}
	}
}
