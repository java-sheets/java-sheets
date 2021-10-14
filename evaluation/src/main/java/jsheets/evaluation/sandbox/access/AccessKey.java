package jsheets.evaluation.sandbox.access;

import java.util.Objects;
import java.util.regex.Pattern;

public record AccessKey(Pattern separator, String value) {
  public static AccessKey infer(String value) {
    return value.contains("/") ? slashSeparated(value) : dotSeparated(value);
  }

  private static final Pattern dotOrMethodSeparator =
    Pattern.compile("[.#]");

  public static AccessKey dotSeparated(String value) {
    Objects.requireNonNull(value, "value");
    return new AccessKey(dotOrMethodSeparator, value);
  }

  private static final Pattern slashOrMethodSeparator =
    Pattern.compile("[/#]");

  public static AccessKey slashSeparated(String value) {
    Objects.requireNonNull(value, "value");
    return new AccessKey(slashOrMethodSeparator, value);
  }

  public String[] split() {
    return separator.split(value);
  }

  public String lastPart() {
    var parts = split();
    if (parts.length == 0) {
      return value;
    }
    return parts[parts.length - 1];
  }
}