package jsheets.config;

public final class Configs {
  private Configs() {}

  public static Config loadAll(Config.Source... sources) {
    var configs = new Config[sources.length];
    for (int source = 0; source < sources.length; source++) {
      configs[source] = sources[source].load();
    }
    return CombinedConfig.of(configs);
  }
}