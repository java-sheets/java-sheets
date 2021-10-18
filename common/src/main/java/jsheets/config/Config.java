package jsheets.config;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Storage of {@link Config.Field configuration fields}, that can be accessed
 * in a typesafe way (using the {@link Config.Key keys}.
 */
public interface Config {
  <T> Field<T> lookup(Key<T> key);

  interface Source {
    Config load();
  }

  interface Field<T> {
    T require();

    Optional<T> orNone();

    default T or(T value) {
      return orNone().orElse(value);
    }

    boolean exists();
  }

  final class Key<T> {
    public static Key<Boolean> ofFlag(String name) {
      Objects.requireNonNull(name, "name");
      return new Key<>(name, Boolean::parseBoolean);
    }

    public static Key<String> ofString(String name) {
      Objects.requireNonNull(name, "name");
      return new Key<>(name, String::valueOf);
    }

    public static Key<Integer> ofInt(String name) {
      Objects.requireNonNull(name, "name");
      return new Key<>(name, Integer::parseInt);
    }

    public static Key<Double> ofDouble(String name) {
      Objects.requireNonNull(name, "name");
      return new Key<>(name, Double::parseDouble);
    }

    public static <T> Key<T> of(String name, Function<String, T> parse) {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(parse, "parse");
      return new Key<>(name, parse);
    }

    private final String name;
    private final Function<String, T> parse;

    private Key(String name, Function<String, T> parse) {
      this.name = name;
      this.parse = parse;
    }

    public Field<T> in(Config config) {
      return config.lookup(this);
    }

    public T parse(String rawInput) {
      return parse.apply(rawInput);
    }

    @Override
    public boolean equals(Object target) {
      if (target == this) {
        return true;
      }
      return target instanceof Key key && (
        key.name.equals(name)
      );
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
