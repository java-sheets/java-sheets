package jsheets.runtime.evaluation.shell;

import jsheets.EvaluateResponse;
import jsheets.EvaluationResult;
import jsheets.EvaluationResult.Kind;
import jsheets.runtime.evaluation.Evaluation;

public final class MessageSink {
  private final Evaluation.Listener output;

  private final StringBuffer outputBuffer = new StringBuffer();
  private final StringBuffer errorBuffer = new StringBuffer();

  MessageSink(Evaluation.Listener output) {
    this.output = output;
  }

  public void writeError(String message) {
    errorBuffer.append(message);
  }

  public void writeOutput(String message) {
    outputBuffer.append(message);
  }

  public void flush(String componentId) {
    var response = EvaluateResponse.newBuilder();
    flushBuffers(componentId, Kind.INFO, outputBuffer, response);
    flushBuffers(componentId, Kind.ERROR, errorBuffer, response);
    if (response.getResultCount() != 0) {
      output.send(response.build());
    }
  }

  private void flushBuffers(
    String componentId,
    Kind kind,
    StringBuffer buffer,
    EvaluateResponse.Builder response
  ) {
    synchronized (buffer) {
      if (buffer.isEmpty()) {
        return;
      }
      addResult(response, kind, componentId, buffer.toString());
      buffer.setLength(0);
    }
  }

  private void addResult(
    EvaluateResponse.Builder response,
    Kind kind,
    String componentId,
    String output
  ) {
    response.addResult(
      EvaluationResult.newBuilder()
        .setKind(kind)
        .setComponentId(componentId)
        .setOutput(output)
        .build()
    );
  }
}