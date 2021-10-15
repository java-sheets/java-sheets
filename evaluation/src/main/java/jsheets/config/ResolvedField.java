package jsheets.config;

import java.util.Optional;

record ResolvedField<T>(T value) implements Config.Field<T> {
  @Override
  public T require() {
    return value;
  }

  @Override
  public T or(T fallback) {
    return value;
  }

  @Override
  public Optional<T> orNone() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean exists() {
    return true;
  }
}