package api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer;

import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.BUILD_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.POM_XML;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.SETTINGS_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.INCOMPLETE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.MAVEN;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.MULTIPLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.NONE;
import static java.util.stream.Collectors.joining;

import api.poja.io.model.importer.analyzer.Analyzer;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.UnknownApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class BuildToolAnalyzer implements Analyzer {

  /**
   * The `mvnw` script retrieves the mvn distribution defined in `distributionUrl` within
   * maven-wrapper.properties. The file .mvn/wrapper/maven-wrapper.jar is not required to be present
   * beforehand, as it is downloaded automatically if missing.
   */
  public static final String[] MVN_FILENAMES = {
    POM_XML, ".mvn/wrapper/maven-wrapper.properties", "mvnw"
  };

  public static final String[] GRADLE_FILENAMES = {
    BUILD_GRADLE,
    SETTINGS_GRADLE,
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradlew"
  };

  @Override
  public BuildToolAnalysisResult analyze(UnknownApplication unknownApplication) {
    Path root = unknownApplication.file().toPath();

    var gradleDetected = isGradleKind(root);
    var mavenDetected = isMavenKind(root);

    if (gradleDetected && mavenDetected) {
      return new BuildToolAnalysisResult(
          root,
          MULTIPLE,
          ApplicationImportLog.error(
              "Unable to determine the build tool. Detected configuration files for both Gradle and"
                  + " Maven."));
    }

    if (mavenDetected) {
      return checkBuildFiles(root, MVN_FILENAMES, MAVEN, "Maven");
    }

    if (gradleDetected) {
      return checkBuildFiles(root, GRADLE_FILENAMES, GRADLE, "Gradle");
    }

    return new BuildToolAnalysisResult(
        root,
        NONE,
        ApplicationImportLog.error(
            "No build tool detected. Missing Gradle or Maven configuration files."));
  }

  private static boolean isGradleKind(Path root) {
    return Files.exists(root.resolve(BUILD_GRADLE));
  }

  private static boolean isMavenKind(Path root) {
    return Files.exists(root.resolve(POM_XML));
  }

  private static BuildToolAnalysisResult checkBuildFiles(
      Path root, String[] requiredFiles, BuildTool tool, String toolName) {
    List<Path> paths = Stream.of(requiredFiles).map(root::resolve).toList();
    List<Path> missingFiles = paths.stream().filter(Files::notExists).toList();

    if (!missingFiles.isEmpty()) {
      return incompleteBtResult(root, toolName, missingFiles);
    }

    return new BuildToolAnalysisResult(root, tool).buildToolFilePaths(paths);
  }

  private static BuildToolAnalysisResult incompleteBtResult(
      Path root, String toolName, List<Path> missingPaths) {
    String files =
        missingPaths.stream().map(Path::getFileName).map(Path::toString).collect(joining(", "));

    return new BuildToolAnalysisResult(
        root,
        INCOMPLETE,
        ApplicationImportLog.error(
            "Incomplete " + toolName + " configuration. Missing required files: " + files));
  }
}
