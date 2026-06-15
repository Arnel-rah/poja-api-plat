package api.poja.io.model.importer.analyzer.buildtool;

import java.util.Arrays;
import java.util.stream.Stream;

public class BuildToolFile {

  public static final String BUILD_GRADLE = "build.gradle";
  public static final String SETTINGS_GRADLE = "settings.gradle";
  public static final String[] GRADLE_FILES = {
    "gradlew", "gradlew.cmd", "gradlew.bat", "gradle", BUILD_GRADLE, SETTINGS_GRADLE
  };

  public static final String POM_XML = "pom.xml";
  public static final String[] MAVEN_FILES = {"mvnw", "mvnw.cmd", "mvnw.bat", ".mvn", POM_XML};

  public static final String[] ALL =
      Stream.of(GRADLE_FILES, MAVEN_FILES).flatMap(Arrays::stream).toArray(String[]::new);

  private BuildToolFile() {}
}
