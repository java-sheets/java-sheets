package jsheets.runtime.monitoring;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jsheets.evaluation.EvaluationStartEvent;
import jsheets.evaluation.EvaluationStopEvent;

final class EvaluationEngineMonitoring {
  static EvaluationEngineMonitoring register(MeterRegistry registry) {
    return new EvaluationEngineMonitoring(
      registry.gauge(
        "jsheets.runtime.evaluation.activeEvaluations",
        new AtomicInteger(0)
      ),
      Counter.builder("jsheets.runtime.evaluation.successfulEvaluations")
        .description("Count of evaluations that completed without errors")
        .register(registry),
      Counter.builder("jsheets.runtime.evaluation.erroneousEvaluations")
        .description("Count of evaluations that completed with errors")
        .register(registry),
      Counter.builder("jsheets.runtime.evaluation.failedEvaluations")
        .description("Count of evaluations that failed internally")
        .register(registry),
      Timer.builder("jsheets.runtime.evaluation.duration")
        .description("Average duration of evaluations")
        .register(registry)
    );
  }

  private final AtomicInteger activeEvaluations;
  private final Counter successfulEvaluations;
  private final Counter failedEvaluations;
  private final Counter erroneousEvaluations;
  private final Timer evaluationDuration;

  private EvaluationEngineMonitoring(
    AtomicInteger activeEvaluations,
    Counter successfulEvaluations,
    Counter erroneousEvaluations,
    Counter failedEvaluations,
    Timer evaluationDuration
  ) {
    this.activeEvaluations = activeEvaluations;
    this.successfulEvaluations = successfulEvaluations;
    this.failedEvaluations = failedEvaluations;
    this.erroneousEvaluations = erroneousEvaluations;
    this.evaluationDuration = evaluationDuration;
  }

  @Subscribe
  public void recordStart(EvaluationStartEvent start) {
    activeEvaluations.incrementAndGet();
  }

  @Subscribe
  public void recordStop(EvaluationStopEvent stop) {
    activeEvaluations.decrementAndGet();
    if (!stop.hasFailed()) {
      // We should not record the time of failed evaluations, failure indicates
      // that it never started and will thus only contain the time it took to
      // perform preprocessing and startup till a failure occurred.
      evaluationDuration.record(stop.duration());
    }
    switch (stop.status()) {
      case Failed -> failedEvaluations.increment();
      case CompletedSuccessfully -> successfulEvaluations.increment();
      case CompletedWithErrors -> erroneousEvaluations.increment();
    }
  }
}