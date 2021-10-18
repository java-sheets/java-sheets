package jsheets.config;

import java.util.Arrays;
import java.util.Objects;

public final class CombinedConfig implements Config {
  /**
   * Combines the {@code configs}, prioritized based on descending order.
   * <p>
   * If config {@code Ci} contains a value for any given key, then it is
   * returned, otherwise config {@code Ci+1} is queried until no configs remain
   * and a <italic>missing field</italic> is returned.
   *
   * @param configs Ordered set of sources.
   * @return Config that merges the values of all {@code configs}.
   */
  public static Config of(Config... configs) {
    Objects.requireNonNull(configs);
    return new CombinedConfig(configs.clone());
  }

  private final Config[] configs;

  private CombinedConfig(Config[] configs) {
    this.configs = configs;
  }

  @Override
  public <T> Field<T> lookup(Key<T> key) {
    for (var config : configs) {
      var field = config.lookup(key);
      if (field.exists()) {
        return field;
      }
    }
    return new MissingField<>(key);
  }

  @Override
  public String toString() {
    return "CombinedSource(%s)".formatted(Arrays.toString(configs));
  }
}