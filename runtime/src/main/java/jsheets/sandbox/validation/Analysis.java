package jsheets.sandbox.validation;

public final class Analysis {
  public static Analysis create() {
    return new Analysis();
  }

  public record Violation() {}

  private Analysis() {}
}