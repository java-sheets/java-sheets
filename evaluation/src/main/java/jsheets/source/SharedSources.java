package jsheets.source;

import com.google.common.base.MoreObjects;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class SharedSources {
  public static SharedSources createEmpty() {
    return createEmpty(Clock.systemUTC());
  }

  public static SharedSources createEmpty(Clock clock) {
    Objects.requireNonNull(clock, "a clock is required to track update time");
    return new SharedSources(clock, new ConcurrentHashMap<>());
  }

  public record Entry(String snippet, String hash, Instant lastUpdate) { }

  private final Clock clock;
  private final Map<String, Entry>  hashes;

  private SharedSources(Clock clock, Map<String, Entry> hashes) {
    this.hashes = hashes;
    this.clock = clock;
  }

  public boolean has(String snippet, Entry hash) {
    var knownHash = hashes.get(snippet);
    return knownHash != null && knownHash.equals(hash);
  }

  public void save(String snippet, String hash) {
    hashes.put(snippet, new Entry(snippet, hash, clock.instant()));
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("hashes", hashes)
      .toString();
  }
}
