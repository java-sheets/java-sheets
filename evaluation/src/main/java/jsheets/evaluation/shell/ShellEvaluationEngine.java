package jsheets.evaluation.shell;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jsheets.StartEvaluationRequest;
import jsheets.evaluation.Evaluation;
import jsheets.evaluation.EvaluationEngine;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.evaluation.shell.environment.StandardEnvironment;
import jsheets.evaluation.shell.execution.ExecutionMethod;
import jsheets.evaluation.shell.execution.SystemBasedExecutionMethodFactory;
import jsheets.event.EventSink;

public final class ShellEvaluationEngine implements EvaluationEngine {
  private final Executor workerPool;
  private final ScheduledExecutorService scheduler;
  private final ExecutionEnvironment executionEnvironment;
  private final ExecutionMethod.Factory executionMethodFactory;
  private final Duration messageFlushInterval;
  private final EventSink events;
  private final Clock clock;
  private final Collection<String> builtinImports;

  private ShellEvaluationEngine(
    Clock clock,
    Executor workerPool,
    ScheduledExecutorService scheduler,
    ExecutionEnvironment executionEnvironment,
    ExecutionMethod.Factory executionMethodFactory,
    Duration messageFlushInterval,
    EventSink events,
    Collection<String> builtinImports
  ) {
    this.clock = clock;
    this.workerPool = workerPool;
    this.scheduler = scheduler;
    this.executionEnvironment = executionEnvironment;
    this.messageFlushInterval = messageFlushInterval;
    this.executionMethodFactory = executionMethodFactory;
    this.events = events;
    this.builtinImports = builtinImports;
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
      events,
      new MessageOutput(
        messageFlushInterval,
        scheduler,
        listener
      ),
      builtinImports
    );
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private Executor workerPool;
    private ExecutionEnvironment environment;
    private Duration messageFlushInterval;
    private ScheduledExecutorService scheduler;
    private ExecutionMethod.Factory executionMethodFactory;
    private EventSink events;
    private Clock clock;
    private Collection<String> builtinImports;

    public Builder useWorkerPool(Executor pool) {
      Objects.requireNonNull(pool, "workerPool");
      workerPool = pool;
      return this;
    }

    public Builder withClock(Clock clock) {
      Objects.requireNonNull(clock, "clock");
      this.clock = clock;
      return this;
    }

    public Builder withEventSink(EventSink sink) {
      Objects.requireNonNull(sink, "sink");
      events = sink;
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

    public Builder useBuiltinImports(Collection<String> builtinImports) {
      Objects.requireNonNull(builtinImports, "builtinImports");
      this.builtinImports = builtinImports;
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
        selectClock(),
        selectWorkerPool(),
        selectScheduler(),
        selectEnvironment(),
        selectExecutionMethodFactory(),
        selectMessageFlushInterval(),
        selectEventSink(),
        selectBuiltinImports()
      );
    }

    private Clock selectClock() {
      return clock == null ? Clock.systemUTC() : clock;
    }

    private EventSink selectEventSink() {
      return events == null ? EventSink.ignore() : events;
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

    private Collection<String> selectBuiltinImports() {
      return builtinImports == null ? List.of() : builtinImports;
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
