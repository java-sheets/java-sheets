package jsheets.runtime.evaluation;

import java.nio.file.Path;

import com.google.api.client.util.Strings;
import jsheets.config.Config;
import jsheets.config.Configs;
import jsheets.config.FileConfigSource;

/**
 * Reads the {@code AccessGraph} configuration from the classpath.
 */
public final class EvaluationConfigSource implements Config.Source {
  public static EvaluationConfigSource create() {
    return new EvaluationConfigSource();
  }

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

  private static final Config.Key<String> virtualMachineOptionsKey =
    Config.Key.ofString("evaluation.fork.virtualMachineOptions");

  static Config.Key<String> virtualMachineOptionsKey() {
    return virtualMachineOptionsKey;
  }

  @Override
  public Config load() {
    var directory = determineConfigPath();
    return Configs.loadAll(
      new FileConfigSource(
        accessGraphKey,
        Path.of("runtime/evaluation/sandbox/accessGraph.txt"),
        directory
      ),
      new FileConfigSource(
        virtualMachineOptionsKey,
        Path.of("runtime/evaluation/fork/virtualMachineOptions.txt"),
        directory
      ),
      new FileConfigSource(
        defaultImportsKey,
        Path.of("runtime/evaluation/defaultImports.txt"),
        directory
      )
    );
  }

  private static final String configPathOverrideField =
    "JSHEETS_RUNTIME_CONFIG_PATH";

  private static Path determineConfigPath() {
    var specialPath = System.getenv(configPathOverrideField);
    return Strings.isNullOrEmpty(specialPath)
      ? Path.of(System.getProperty("user.dir"))
      : Path.of(specialPath);
  }
}