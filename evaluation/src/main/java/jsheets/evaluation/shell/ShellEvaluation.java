package jsheets.evaluation.shell;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;

import com.google.common.base.MoreObjects;
import com.google.common.flogger.FluentLogger;

import jsheets.CodeSpan;
import jsheets.EvaluateResponse;
import jsheets.EvaluationError;
import jsheets.EvaluationResult;
import jsheets.SnippetSources;
import jsheets.StartEvaluationRequest;
import jsheets.evaluation.Evaluation;
import jsheets.evaluation.EvaluationStartEventBuilder;
import jsheets.evaluation.EvaluationStopEvent;
import jsheets.evaluation.EvaluationStopEventBuilder;
import jsheets.evaluation.failure.FailedEvaluation;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.evaluation.shell.execution.ExecutionMethod;
import jsheets.event.EventSink;

final class ShellEvaluation implements Evaluation {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private volatile JShell shell;
  private volatile ExecutionMethod executionMethod;
  private volatile Instant startTime;
  private final Evaluation.Listener listener;
  private final ExecutionEnvironment environment;
  private final ExecutionMethod.Factory executionMethodFactory;
  private final MessageOutput messageOutput;
  private final EventSink events;
  private final Clock clock;
  /* Is updated whenever errors occur, otherwise stays successful */
  private volatile EvaluationStopEvent.Status stopStatus =
    EvaluationStopEvent.Status.CompletedSuccessfully;

  ShellEvaluation(
    Clock clock,
    ExecutionEnvironment environment,
    ExecutionMethod.Factory executionMethodFactory,
    Evaluation.Listener listener,
    EventSink events,
    MessageOutput messageOutput
  ) {
    this.clock = clock;
    this.listener = listener;
    this.messageOutput = messageOutput;
    this.executionMethodFactory = executionMethodFactory;
    this.environment = environment;
    this.events = events;
  }

  public void start(StartEvaluationRequest request) {
    shell = createShell();
    executionMethod = executionMethodFactory.create(shell);
    startTime = clock.instant();
    messageOutput.open();
    var snippetId = request.getSnippet().getReference().getSnippetId();
    postStartEvent(snippetId);
    try {
      listener.send(evaluateSources(request));
      listener.close();
    } finally {
      cleanUp();
      postStopEvent(snippetId);
    }
  }

  private void postStartEvent(String snippetId) {
    events.postIfEnabled(() -> EvaluationStartEventBuilder.builder()
      .snippetId(snippetId)
      .labels(createEventLabels())
      .build()
    );
  }

  private void postStopEvent(String snippetId) {
    events.postIfEnabled(() -> {
      var duration = startTime == null
        ? Duration.ZERO
        : Duration.between(startTime, clock.instant());
      return EvaluationStopEventBuilder.builder()
        .snippetId(snippetId)
        .labels(createEventLabels())
        .status(stopStatus)
        .duration(duration)
        .build();
    });
  }

  private Map<String, Object> createEventLabels() {
    return Map.of(
      "executionEnvironment", environment.getClass().getName(),
      "startTime", startTime == null ? "-1" : startTime.toString()
    );
  }

  private EvaluateResponse evaluateSources(StartEvaluationRequest request) {
    var response = EvaluateResponse.newBuilder();
    for (var source : request.getSourcesList()) {
      source.getCodeComponentsList().stream()
        .sorted(Comparator.comparing(SnippetSources.CodeComponent::getOrder))
        .forEachOrdered(component -> evaluateComponent(component, response));
    }
    return response.build();
  }

  private void evaluateComponent(
    SnippetSources.CodeComponent component,
    EvaluateResponse.Builder response
  ) {
    messageOutput.updateCurrentComponentId(component.getId());
    try {
      for (var snippet : executionMethod.execute(component.getCode()) ) {
        reportSnippetEvent(component.getId(), snippet, response);
      }
      messageOutput.flush();
    } catch (Throwable failedEvaluation) {
      reportError(component, response, failedEvaluation);
    }
  }

  private void reportError(
    SnippetSources.CodeComponent component,
    EvaluateResponse.Builder response,
    Throwable failure
  ) {
    updateStopStatus(EvaluationStopEvent.Status.Failed);
    FailedEvaluation.capture(failure).ifPresentOrElse(
      value -> reportFailedEvaluation(component, response, value),
      () -> reportInternalFailure(component, response, failure)
    );
  }

  private void reportFailedEvaluation(
    SnippetSources.CodeComponent component,
    EvaluateResponse.Builder response,
    FailedEvaluation failedEvaluation
  ) {
    failedEvaluation.describe(Locale.ENGLISH)
      .distinct()
      .map(error -> error.toBuilder().setComponentId(component.getId()))
      .forEach(response::addError);
  }

  private void reportInternalFailure(
    SnippetSources.CodeComponent component,
    EvaluateResponse.Builder response,
    Throwable failure
  ) {
    log.atWarning()
      .atMostEvery(5, TimeUnit.SECONDS)
      .withCause(failure).log("shell evaluation failed");
    response.addError(
      EvaluationError.newBuilder()
        .setComponentId(component.getId())
        .setKind("internal")
        .build()
    );
  }

  private void reportSnippetEvent(
    String componentId,
    SnippetEvent event,
    EvaluateResponse.Builder response
  ) {
    switch (event.status()) {
      case VALID -> {
        if (event.value() == null) {
          return;
        }
        response.addResult(
          EvaluationResult.newBuilder()
            .setComponentId(componentId)
            .setKind(EvaluationResult.Kind.INFO)
            .setOutput(event.value())
            .build()
        );
      }
      case REJECTED -> reportFailure(componentId, event, response);
    }
  }

  private void reportFailure(
    String componentId,
    SnippetEvent event,
    EvaluateResponse.Builder response
  ) {
    var diagnostics = shell.diagnostics(event.snippet()).toList();
    if (!diagnostics.isEmpty()) {
      updateStopStatus(EvaluationStopEvent.Status.CompletedWithErrors);
    }
    for (var diagnostic : diagnostics) {
      response.addError(EvaluationError.newBuilder()
        .setComponentId(componentId)
        .setKind(diagnostic.getCode())
        .setMessage(diagnostic.getMessage(Locale.ENGLISH))
        .setSpan(CodeSpan.newBuilder()
          .setStart(diagnostic.getStartPosition())
          .setEnd(diagnostic.getEndPosition())
          .build())
        .build());
    }
  }

  /* Once the stop status is *Failed* it may not change to something else */
  private void updateStopStatus(EvaluationStopEvent.Status target) {
    switch (target) {
      case CompletedWithErrors -> {
        if (stopStatus.equals(EvaluationStopEvent.Status.CompletedSuccessfully)) {
          stopStatus = target;
        }
      }
      case Failed -> stopStatus = target;
    }
  }

  private JShell createShell() {
    var engine = environment.control(UUID.randomUUID().toString());
    return JShell.builder()
      .out(messageOutput.createStandardSink())
      .err(messageOutput.createErrorSink())
      .executionEngine(engine, Map.of())
      .build();
  }

  @Override
  public void stop() {
    cleanUp();
  }

  private void cleanUp() {
    messageOutput.close();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("shell", shell)
      .add("environment", environment)
      .add("eventSink", events)
      .toString();
  }
}
