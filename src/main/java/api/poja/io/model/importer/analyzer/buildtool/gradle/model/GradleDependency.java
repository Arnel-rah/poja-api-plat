package api.poja.io.model.importer.analyzer.buildtool.gradle.model;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@code GradleDependency} represents a dependency declaration that is composed
 *
 * <p>Configuration Name + Dependency Notation - GroupID : ArtifactID (Name) : Version?
 *
 * @param group groupId
 * @param name ArtifactID
 * @param version
 */
@Slf4j
@Builder(toBuilder = true)
public record GradleDependency(String configuration, String group, String name, String version) {
  public GradleDependency {
    if (configuration == null || configuration.isBlank()) {
      throw new IllegalArgumentException("configuration cannot be null or blank");
    }

    if (group == null || group.isBlank()) {
      throw new IllegalArgumentException("group cannot be null or blank");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name cannot be null or blank");
    }

    if (version == null) {
      log.warn("version is null");
    }
  }

  /**
   * Returns the string representation of this gradle dependency whose format is "Configuration
   * 'GROUP:NAME:VERSION'" if the version is specified otherwise "Configuration 'GROUP:NAME'"
   */
  @Override
  public String toString() {
    return String.format(
        "%s '%s:%s%s'", configuration, group, name, version == null ? "" : ":" + version);
  }

  public String key() {
    return String.join(":", configuration, group, name);
  }
}
