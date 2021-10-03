package jsheets.runtime.evaluation.shell;


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
import jsheets.runtime.evaluation.Evaluation;
import jsheets.shell.environment.ExecutionEnvironment;
import jsheets.shell.execution.ExecutionMethod;
import jsheets.source.SharedSources;

final class ShellEvaluation implements Evaluation {
  private enum Stage { Initial, Starting, Evaluating, Terminated }

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private volatile JShell shell;
  private volatile ExecutionMethod executionMethod;
  private final Evaluation.Listener listener;
  private final SharedSources sources;
  private final ExecutionEnvironment environment;
  private final ExecutionMethod.Factory executionMethodFactory;
  private final MessageOutput messageOutput;
  private final AtomicReference<Stage> stage = new AtomicReference<>(Stage.Initial);

  ShellEvaluation(
    Clock clock,
    ExecutionEnvironment environment,
    ExecutionMethod.Factory executionMethodFactory,
    Evaluation.Listener listener,
    MessageOutput messageOutput
  ) {
    this.listener = listener;
    this.messageOutput = messageOutput;
    this.executionMethodFactory = executionMethodFactory;
    this.environment = environment;
    sources = SharedSources.createEmpty(clock);
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
    } catch (Exception failedEvaluation) {
      log.atWarning()
        .atMostEvery(5, TimeUnit.SECONDS)
        .withCause(failedEvaluation).log("shell evaluation failed");
      response.addError(
        EvaluationError.newBuilder()
          .setComponentId(component.getId())
          .setKind("internal")
          .build()
      );
    }
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
      .add("sources", sources)
      .add("environment", environment)
      .add("stage", stage)
      .toString();
  }
}
