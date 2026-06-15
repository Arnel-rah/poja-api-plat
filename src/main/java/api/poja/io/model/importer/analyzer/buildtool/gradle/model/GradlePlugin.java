package api.poja.io.model.importer.analyzer.buildtool.gradle.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record GradlePlugin(String id, String version) {
  public GradlePlugin {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id cannot be null or blank");
    }

    if (version == null) {
      log.warn("version is null");
    }
  }

  /**
   * Returns the string representation of this gradle plugin whose format is "id 'VERSION' version
   * 'version'" if the version is specified otherwise "id 'ID'"
   */
  @Override
  public String toString() {
    return String.format(
        "id '%s'%s", id, version != null ? String.format(" version '%s'", version) : "");
  }

  public static GradlePlugin JAVA_PLUGIN = new GradlePlugin("java", null);
}
