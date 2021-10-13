package jsheets.sandbox.validation;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public final class Analysis {
  public static Analysis create() {
    return new Analysis();
  }

  public interface Violation {}

  private final Collection<Violation> violations =
    new ConcurrentLinkedQueue<>();

  private Analysis() {}

  public void report(Violation violation) {
    violations.add(violation);
  }

  public void reportViolations() {
    switch (violations.size()) {
      case 0 -> { }
      case 1 -> throw new RuntimeException(
        violations.iterator().next().toString()
      );
      default -> throw new RuntimeException(
        violations.stream()
          .map(Violation::toString)
          .collect(Collectors.joining(", "))
      );
    }
  }
}