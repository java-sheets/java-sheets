package jsheets.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

public final class RawConfig implements Config {
  public static RawConfig of(Map<String, String> entries) {
    var fields = new HashMap<String, String>(entries.size());
    for (var entry : entries.entrySet()) {
      Objects.requireNonNull(entry.getKey(), "keys may not be null");
      Objects.requireNonNull(entry.getValue(), "values may not be null");
      fields.put(entry.getKey(), entry.getValue());
    }
    return new RawConfig(fields);
  }

  private final Map<String, String> values;

  private RawConfig(Map<String, String> values) {
    this.values = values;
  }

  @Override
  public <T> Field<T> lookup(Key<T> key) {
    Objects.requireNonNull(key, "key may not be null");
    var value = values.get(key.toString());
    if (value == null) {
      return new MissingField<>(key);
    }
    return new ResolvedField<>(key.parse(value));
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private final Map<String, String> values = new HashMap<>();

    @CanIgnoreReturnValue
    public <T> Builder with(Key<T> key, T value) {
      Objects.requireNonNull(key, "keys may not be null");
      Objects.requireNonNull(value, "values may not be null");
      values.put(key.toString(), value.toString());
      return this;
    }

    @CanIgnoreReturnValue
    public Builder withRaw(String key, String value) {
      Objects.requireNonNull(key, "keys may not be null");
      Objects.requireNonNull(value, "values may not be null");
      values.put(key, value);
      return this;
    }

    public RawConfig create() {
      return new RawConfig(Map.copyOf(values));
    }
  }
}