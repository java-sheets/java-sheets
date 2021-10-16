package jsheets.evaluation.failure;

import jsheets.EvaluationError;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public interface FailedEvaluation {
  Stream<EvaluationError> describe(Locale locale);

  static Optional<FailedEvaluation> capture(Throwable failure) {
    while (failure != null) {
      if (failure instanceof FailedEvaluation) {
        return Optional.of((FailedEvaluation) failure);
      }
      failure = failure.getCause();
    }
    return Optional.empty();
  }
}
