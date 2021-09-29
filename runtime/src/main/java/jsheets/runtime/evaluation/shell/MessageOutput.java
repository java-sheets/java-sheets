package jsheets.runtime.evaluation.shell;

import java.io.PrintStream;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.flogger.FluentLogger;

import jsheets.output.CapturingOutput;
import jsheets.runtime.evaluation.Evaluation;

final class MessageOutput implements AutoCloseable {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final MessageLog messages;
  private final Duration flushInterval;
  private final ScheduledExecutorService scheduler;
  private final AtomicReference<String> currentComponentId = new AtomicReference<>(null);
  private final AtomicReference<ScheduledFuture<?>> flushTask = new AtomicReference<>(null);

  MessageOutput(
    Duration flushInterval,
    ScheduledExecutorService scheduler,
    Evaluation.Listener listener
  ) {
    this.flushInterval = flushInterval;
    this.scheduler = scheduler;
    messages = new MessageLog(listener);
  }

  public void open() {
    flushTask.set(
      scheduler.scheduleWithFixedDelay(
        this::flush,
        flushInterval.toMillis(),
        flushInterval.toMillis(),
        TimeUnit.MILLISECONDS
      )
    );
  }

  @Override
  public void close() {
    var task = flushTask.getAndSet(null);
    if (task != null) {
      task.cancel(/* mayInterrupt */ false);
    }
  }

  public void updateCurrentComponentId(String id) {
    currentComponentId.set(id);
  }

  public void flush() {
    var id = currentComponentId.get();
    if (id == null) {
      return;
    }
    try {
      messages.flush(id);
    } catch (Exception failure) {
      log.atWarning()
        .withCause(failure)
        .log("failed to flush messages of component %s", id);
    }
  }

  public PrintStream createStandardSink() {
    return CapturingOutput.to(messages::writeOutput);
  }

  public PrintStream createErrorSink() {
    return CapturingOutput.to(messages::writeError);
  }
}