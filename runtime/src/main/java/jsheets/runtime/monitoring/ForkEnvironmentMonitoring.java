package jsheets.runtime.monitoring;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.Subscribe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jsheets.evaluation.shell.environment.fork.BoxLifecycleEvent;

final class ForkEnvironmentMonitoring {
  static ForkEnvironmentMonitoring register(MeterRegistry metrics) {
    var activeBoxCount = metrics.gauge(
      "jsheets.runtime.evaluation.fork.activeBoxCount",
      new AtomicInteger(0)
    );
    var startedBoxes = Counter
      .builder("jsheets.runtime.evaluation.fork.startedBoxes")
      .description("The number of boxes that have been started")
      .register(metrics);
    return new ForkEnvironmentMonitoring(activeBoxCount, startedBoxes);
  }

  private final AtomicInteger activeBoxCount;
  private final Counter startedBoxes;

  private ForkEnvironmentMonitoring(
    AtomicInteger activeBoxCount,
    Counter startedBoxes
  ) {
    this.activeBoxCount = activeBoxCount;
    this.startedBoxes = startedBoxes;
  }

  @Subscribe
  public void receiveLifecycleUpdate(BoxLifecycleEvent event) {
    switch (event.stage()) {
      case Starting -> {
        activeBoxCount.incrementAndGet();
        startedBoxes.increment();
      }
      case Stopping -> activeBoxCount.decrementAndGet();
    }
  }

  @Override
  public String toString() {
    return "ForkEnvironmentMonitoring(activeBoxCount=%s, startedBoxes=%s)"
      .formatted(activeBoxCount, startedBoxes);
  }
}