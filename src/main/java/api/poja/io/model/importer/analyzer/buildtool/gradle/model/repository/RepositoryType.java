package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public enum RepositoryType {
  MAVEN("maven"),
  MAVEN_LOCAL("mavenLocal"),
  FLAT_DIR("flatDir"),
  IVY("ivy");

  private final String value;

  @Override
  public String toString() {
    return value;
  }

  public static RepositoryType fromValue(String value) {
    for (RepositoryType b : values()) {
      if (b.value().equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
