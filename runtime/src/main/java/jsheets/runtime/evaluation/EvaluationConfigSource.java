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
public final class EvaluationConfigSource implements Config.Source {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private EvaluationConfigSource() {}

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

  private static final Config.Key<String> defaultImportsKey =
    Config.Key.ofString("evaluation.defaultImports");

  public static Config.Key<String> accessGraphKey() {
    return accessGraphKey;
  }

  public static EvaluationConfigSource fromClassPath() {
    return new EvaluationConfigSource();
  }

  private static final Config.Key<String> virtualMachineOptionsKey =
    Config.Key.ofString("evaluation.fork.virtualMachineOptions");

  static Config.Key<String> virtualMachineOptionsKey() {
    return virtualMachineOptionsKey;
  }

  private static final String accessGraphFilePath =
    "runtime/evaluation/sandbox/accessGraph.txt";

  private static final String virtualMachineOptionsFilePath =
    "runtime/evaluation/fork/virtualMachineOptions.txt";

  private static final String defaultImportsFilePath =
    "runtime/evaluation/defaultImportsKey.txt";

  @Override
  public Config load() {
    var config = RawConfig.newBuilder();
    readFullFile(accessGraphFilePath).ifPresent(value ->
      config.with(accessGraphKey, value)
    );
    readFullFile(virtualMachineOptionsFilePath).ifPresent(value ->
      config.with(virtualMachineOptionsKey, value)
    );
    readFullFile(defaultImportsFilePath).ifPresent(value ->
      config.with(defaultImportsKey, value)
    );
    return config.create();
  }

  private static Optional<String> readFullFile(String path) {
    var resources = Thread.currentThread().getContextClassLoader();
    var file = resources.getResourceAsStream(path);
    if (file == null) {
      log.atConfig().log("could not find %s in classpath", path);
      return Optional.empty();
    }
    try (var input = new BufferedInputStream(file)) {
      return Optional.of(new String(input.readAllBytes()));
    } catch (IOException failedRead) {
      log.atWarning()
        .withCause(failedRead)
        .log("failed to read %s", path);
    }
    return Optional.empty();
  }
}