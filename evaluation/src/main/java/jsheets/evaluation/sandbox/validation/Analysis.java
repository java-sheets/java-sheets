package jsheets.evaluation.sandbox.validation;

import jsheets.EvaluationError;
import jsheets.evaluation.failure.FailedEvaluation;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Analysis {
  public static Analysis create() {
    return new Analysis();
  }

  public interface Violation extends FailedEvaluation {}

  private final Collection<Violation> violations =
    new ConcurrentLinkedQueue<>();

  private Analysis() {}

  public void report(Violation violation) {
    violations.add(violation);
  }

  public void reportViolations() {
    if (!violations.isEmpty()) {
      throw new FailedAnalysis(Set.copyOf(violations));
    }
  }

  static final class FailedAnalysis extends RuntimeException implements FailedEvaluation {
    private final Collection<Violation> violations;

    private FailedAnalysis(Collection<Violation> violations) {
      super(
        violations.stream()
          .map(Violation::toString)
          .collect(Collectors.joining(", "))
      );
      this.violations = violations;
    }

    public Stream<Violation> violations() {
      return violations.stream();
    }

    @Override
    public Stream<EvaluationError> describe(Locale locale) {
      return violations.stream()
        .flatMap(violation -> violation.describe(locale));
    }
  }

  public static Stream<Violation> captureViolations(Throwable failure) {
    while (failure != null) {
      if (failure instanceof FailedAnalysis) {
        return ((FailedAnalysis) failure).violations();
      }
      failure = failure.getCause();
    }
    return Stream.empty();
  }
}