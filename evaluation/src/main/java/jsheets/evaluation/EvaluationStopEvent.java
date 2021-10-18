package jsheets.evaluation;

import java.time.Duration;
import java.util.Map;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record EvaluationStopEvent(
  String snippetId,
  Duration duration,
  Status status,
  Map<String, Object> labels
) implements EvaluationEvent {

  public boolean hasFailed() {
    return status.equals(Status.Failed);
  }

  public enum Status {
    /**
     * The evaluation was successful, there were no errors in the user code.
     */
    CompletedSuccessfully,
    /**
     * The evaluation could be run but there were errors in the user code.
     */
    CompletedWithErrors,
    /**
     * The evaluation failed, this must not indicate that user code is invalid.
     */
    Failed
  }
}