package jsheets.config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record FileConfigSource(
  Config.Key<?> key,
  Path relativePath,
  Path workingDirectory
) implements Config.Source {

  public static FileConfigSource inCurrentWorkingDirectory(
    Config.Key<?> key,
    Path relativePath
  ) {
    var workingDirectory = Path.of(System.getProperty("user.dir"));
    return new FileConfigSource(key, relativePath, workingDirectory);
  }

  @Override
  public Config load() {
    return loadContent()
      .map(content -> RawConfig.of(Map.of(key.toString(), content)))
      .orElseGet(() -> RawConfig.of(Map.of()));
  }

  private Optional<String> loadContent() {
    return loadFromSystem().or(this::loadFromClassPath);
  }

  private Optional<String> loadFromSystem() {
    var fullPath = workingDirectory.resolve(relativePath);
    try {
      return Optional.of(
        Files.readString(fullPath, StandardCharsets.UTF_8)
      );
    } catch (IOException failedRead) {
      return Optional.empty();
    }
  }

  private Optional<String> loadFromClassPath() {
    var resources = Thread.currentThread().getContextClassLoader();
    var file = resources.getResourceAsStream(relativePath.toString());
    if (file == null) {
      return Optional.empty();
    }
    try (var input = new BufferedInputStream(file)) {
      return Optional.of(new String(input.readAllBytes()));
    } catch (IOException failedRead) {
      return Optional.empty();
    }
  }
}