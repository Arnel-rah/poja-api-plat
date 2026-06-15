package api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer;

public enum BuildTool {
  MAVEN,
  GRADLE,
  NONE,
  MULTIPLE,
  INCOMPLETE;

  public static BuildTool fromValue(String value) {
    for (BuildTool b : values()) {
      if (b.name().equals(value)) {
        return b;
      }
    }

    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
