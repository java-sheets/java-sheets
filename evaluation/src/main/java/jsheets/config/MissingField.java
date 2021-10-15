package jsheets.config;

import java.util.NoSuchElementException;
import java.util.Optional;

record MissingField<T>(Config.Key<T> key) implements Config.Field<T> {
  @Override
  public T require() {
    throw new NoSuchElementException(key + " is missing");
  }

  @Override
  public T or(T value) {
    return value;
  }

  @Override
  public Optional<T> orNone() {
    return Optional.empty();
  }

  @Override
  public boolean exists() {
    return false;
  }
}