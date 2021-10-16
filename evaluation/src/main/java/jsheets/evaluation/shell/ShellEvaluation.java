package jsheets.evaluation.shell;

import java.time.Clock;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import jsheets.evaluation.failure.FailedEvaluation;
import jsheets.evaluation.shell.environment.ExecutionEnvironment;
import jsheets.evaluation.shell.execution.ExecutionMethod;

final class ShellEvaluation implements Evaluation {
  private enum Stage { Initial, Starting, Evaluating, Terminated }

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private volatile JShell shell;
  private volatile ExecutionMethod executionMethod;
  private final Evaluation.Listener listener;
  private final ExecutionEnvironment environment;
  private final ExecutionMethod.Factory executionMethodFactory;
  private final MessageOutput messageOutput;
  private final AtomicReference<Stage> stage = new AtomicReference<>(Stage.Initial);

  ShellEvaluation(
    ExecutionEnvironment environment,
    ExecutionMethod.Factory executionMethodFactory,
    Evaluation.Listener listener,
    MessageOutput messageOutput
  ) {
    this.listener = listener;
    this.messageOutput = messageOutput;
    this.executionMethodFactory = executionMethodFactory;
    this.environment = environment;
  }

  public void start(StartEvaluationRequest request) {
    shell = createShell();
    executionMethod = executionMethodFactory.create(shell);
    messageOutput.open();
    listener.send(evaluateSources(request));
    listener.close();
    cleanUp();
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
        reportEvent(component.getId(), snippet, response);
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

  private void reportEvent(
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
      case REJECTED -> reportFailedEvent(componentId, event, response);
    }
  }

  private void reportFailedEvent(
    String componentId,
    SnippetEvent event,
    EvaluateResponse.Builder response
  ) {
    shell.diagnostics(event.snippet()).forEach(diagnostic ->
      response.addError(
        EvaluationError.newBuilder()
          .setComponentId(componentId)
          .setKind(diagnostic.getCode())
          .setMessage(diagnostic.getMessage(Locale.ENGLISH))
          .setSpan(
            CodeSpan.newBuilder()
              .setStart(diagnostic.getStartPosition())
              .setEnd(diagnostic.getEndPosition())
              .build()
          ).build()
      )
    );
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
      .add("stage", stage)
      .toString();
  }
}
