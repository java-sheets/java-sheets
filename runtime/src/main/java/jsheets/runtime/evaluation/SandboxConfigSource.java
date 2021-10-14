package jsheets.runtime.evaluation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Optional;

import com.google.common.flogger.FluentLogger;

import jsheets.config.Config;
import jsheets.config.RawConfig;

/**
 * Reads the {@code AccessGraph} configuration from the classpath.
 */
public final class SandboxConfigSource implements Config.Source {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private static final Config.Key<Boolean> disableSandboxKey =
    Config.Key.ofFlag("evaluation.sandbox.disable");

  /**
   * Since the sandbox is an important security measure, it has to be
   * disabled explicitly.
   */
  public static Config.Key<Boolean> disableSandboxKey() {
    return disableSandboxKey;
  }

  private static final Config.Key<String> accessGraphKey =
    Config.Key.ofString("evaluation.sandbox.accessGraph");

  public static Config.Key<String> accessGraphKey() {
    return accessGraphKey;
  }

  public static SandboxConfigSource fromClassPath() {
    return new SandboxConfigSource();
  }

  private SandboxConfigSource() {}

  @Override
  public Config load() {
    var config = RawConfig.newBuilder();
    readAccessGraphFile()
      .ifPresent(accessGraph -> config.with(accessGraphKey, accessGraph));
    return config.create();
  }

  private static final String accessGraphFilePath =
    "runtime/evaluation/sandbox/accessGraph.txt";

  private Optional<String> readAccessGraphFile() {
    var resources = Thread.currentThread().getContextClassLoader();
    var file = resources.getResourceAsStream(accessGraphFilePath);
    if (file == null) {
      log.atConfig().log("could not find %s in classpath", accessGraphFilePath);
      return Optional.empty();
    }
    try (var input = new BufferedInputStream(file)) {
      return Optional.of(new String(input.readAllBytes()));
    } catch (IOException failedRead) {
      log.atWarning()
        .withCause(failedRead)
        .log("failed to read accessGraph.txt");
    }
    return Optional.empty();
  }
}