package jsheets.config;

import java.util.Map;
import java.util.Objects;

import com.google.common.annotations.VisibleForTesting;

public final class EnvironmentConfig implements Config {
  public static Config.Source prefixed(String prefix) {
    Objects.requireNonNull(prefix, "prefix");
    return () -> new EnvironmentConfig(prefix + "_", System.getenv());
  }

  private final String prefix;
  private final Map<String, String> environment;

  @VisibleForTesting
  EnvironmentConfig(String prefix, Map<String, String> environment) {
    this.prefix = prefix;
    this.environment = environment;
  }

  @Override
  public <T> Field<T> lookup(Key<T> key) {
    var value = environment.get(formatKey(key));
    if (value == null) {
      return new MissingField<>(key);
    }
    return new ResolvedField<>(key.parse(value));
  }

  private String formatKey(Key<?> key) {
    return prefix + translateKey(key.toString());
  }

  @VisibleForTesting
  static String translateKey(String key) {
    return toEnvironmentCase(key).replace(".", "_");
  }

  private static String toEnvironmentCase(String input) {
    return CamelCase.convert(input, "_", Character::toUpperCase);
  }
}