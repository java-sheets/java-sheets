package jsheets.runtime.evaluation.shell;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jsheets.StartEvaluationRequest;
import jsheets.runtime.evaluation.Evaluation;
import jsheets.runtime.evaluation.EvaluationEngine;
import jsheets.shell.environment.ExecutionEnvironment;
import jsheets.shell.environment.StandardEnvironment;
import jsheets.shell.execution.ExecutionMethod;
import jsheets.shell.execution.SystemBasedExecutionMethodFactory;

public final class ShellEvaluationEngine implements EvaluationEngine {
  private final Clock clock;
  private final Executor workerPool;
  private final ScheduledExecutorService scheduler;
  private final ExecutionEnvironment executionEnvironment;
  private final ExecutionMethod.Factory executionMethodFactory;
  private final Duration messageFlushInterval;

  private ShellEvaluationEngine(
    Clock clock,
    Executor workerPool,
    ScheduledExecutorService scheduler,
    ExecutionEnvironment executionEnvironment,
    ExecutionMethod.Factory executionMethodFactory,
    Duration messageFlushInterval
  ) {
    this.clock = clock;
    this.workerPool = workerPool;
    this.scheduler = scheduler;
    this.executionEnvironment = executionEnvironment;
    this.messageFlushInterval = messageFlushInterval;
    this.executionMethodFactory = executionMethodFactory;
  }

  @Override
  public Evaluation start(
    StartEvaluationRequest request,
    Evaluation.Listener listener
  ) {
    var evaluation = createEvaluation(listener);
    workerPool.execute(() -> evaluation.start(request));
    return evaluation;
  }

  private ShellEvaluation createEvaluation(Evaluation.Listener listener) {
    return new ShellEvaluation(
      clock,
      executionEnvironment,
      executionMethodFactory,
      listener,
      new MessageOutput(
        messageFlushInterval,
        scheduler,
        listener
      )
    );
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private Clock clock = Clock.systemUTC();
    private Executor workerPool;
    private ExecutionEnvironment environment;
    private Duration messageFlushInterval;
    private ScheduledExecutorService scheduler;
    private ExecutionMethod.Factory executionMethodFactory;

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

    public Builder useExecutionMethodFactory(ExecutionMethod.Factory factory) {
      Objects.requireNonNull(factory, "executionMethodFactory");
      this.executionMethodFactory = factory;
      return this;
    }

    public Builder useScheduler(ScheduledExecutorService scheduler) {
      Objects.requireNonNull(scheduler, "scheduler");
      this.scheduler = scheduler;
      return this;
    }

    public Builder withMessageFlushInterval(Duration messageFlushInterval) {
      Objects.requireNonNull(messageFlushInterval, "messageFlushInterval");
      this.messageFlushInterval = messageFlushInterval;
      return this;
    }

    public EvaluationEngine create() {
      return new ShellEvaluationEngine(
        clock,
        selectWorkerPool(),
        selectScheduler(),
        selectEnvironment(),
        selectExecutionMethodFactory(),
        selectMessageFlushInterval()
      );
    }

    private ExecutionMethod.Factory selectExecutionMethodFactory() {
      return executionMethodFactory == null
        ? SystemBasedExecutionMethodFactory.create()
        : executionMethodFactory;
    }

    private ScheduledExecutorService selectScheduler() {
      return scheduler == null
        ? Executors.newSingleThreadScheduledExecutor()
        : scheduler;
    }

    private Duration selectMessageFlushInterval() {
      return messageFlushInterval == null
        ? Duration.ofMillis(500)
        : messageFlushInterval;
    }

    private ExecutionEnvironment selectEnvironment() {
      return environment == null ? StandardEnvironment.create() : environment;
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
