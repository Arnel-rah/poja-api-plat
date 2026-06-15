package api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer;

import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.INCOMPLETE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.MAVEN;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.MULTIPLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.NONE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolAnalyzer.GRADLE_FILENAMES;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;
import static java.nio.file.Files.createTempDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.endpoint.EndpointConf;
import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.model.UnknownApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class BuildToolAnalyzerTest {
  static final String YML_FILE_PATH = "files/poja_6.yml";
  static final String GRADLE_PROJECT_RESOURCE_PATH = "files/poja-base-prod.zip";
  static final String MAVEN_PROJECT_RESOURCE_PATH = "files/poja-maven-project.zip";
  static final String GRADLE_INCOMPLETE_PROJECT_RESOURCE_PATH =
      "files/poja-gradle-project-incomplete.zip";
  static final String MAVEN_INCOMPLETE_PROJECT_RESOURCE_PATH =
      "files/poja-maven-project-incomplete.zip";
  static final String PROJECT_WITH_TWO_BUILD_TOOLS_PATH = "files/project_with_two_build_tools.zip";
  static final String PROJECT_WITHOUT_TOOLS_PATH = "files/project_without_build_tools.zip";

  private final ObjectMapper om = new EndpointConf().objectMapper();
  private final ExtensionGuesser extensionGuesser = new ExtensionGuesser();
  private final FileWriter fileWriter = new FileWriter(extensionGuesser);
  private final FileUnzipper unzipper = new FileUnzipper(fileWriter);

  @Test
  @SneakyThrows
  void success_status_for_existing_gradle_files() {
    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(GRADLE_PROJECT_RESOURCE_PATH)), tempDir);

    var projectRoot = tempDir.toFile();
    var unknownApp = new UnknownApplication(projectRoot, Set.of());
    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(unknownApp);
    var logs = result.logs();

    assertTrue(logs.isEmpty());
    assertTrue(result.successful());
    assertEquals(GRADLE, result.data().buildTool());
  }

  @Test
  @SneakyThrows
  void success_status_for_existing_maven_files() {
    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(MAVEN_PROJECT_RESOURCE_PATH)), tempDir);

    var projectRoot = tempDir.toFile();
    var unknownApp = new UnknownApplication(projectRoot, Set.of());
    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(unknownApp);

    assertTrue(result.logs().isEmpty());
    assertTrue(result.successful());
    assertEquals(MAVEN, result.data().buildTool());
  }

  @Test
  @SneakyThrows
  void failed_status_for_incomplete_gradle_files() {
    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(GRADLE_INCOMPLETE_PROJECT_RESOURCE_PATH)), tempDir);

    var projectRoot = tempDir.toFile();
    var unknownApp = new UnknownApplication(projectRoot, Set.of());
    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(unknownApp);
    var logs = result.logs();

    assertFalse(logs.isEmpty());
    assertTrue(result.failed());
    assertEquals(INCOMPLETE, result.data().buildTool());
    assertTrue(
        logs.getFirst()
            .getMessage()
            .startsWith("Incomplete Gradle configuration. Missing required files:"));
  }

  @Test
  @SneakyThrows
  void failed_status_for_incomplete_mvn_files() {
    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(MAVEN_INCOMPLETE_PROJECT_RESOURCE_PATH)), tempDir);

    var projectRoot = tempDir.toFile();
    var unknownApp = new UnknownApplication(projectRoot, Set.of());
    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(unknownApp);
    var logs = result.logs();

    assertFalse(logs.isEmpty());
    assertTrue(result.failed());
    assertEquals(INCOMPLETE, result.data().buildTool());
    assertTrue(
        logs.getFirst()
            .getMessage()
            .startsWith("Incomplete Maven configuration. Missing required files:"));
  }

  @Test
  @SneakyThrows
  void failed_status_for_project_with_two_build_tools() {
    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(PROJECT_WITH_TWO_BUILD_TOOLS_PATH)), tempDir);

    var projectRoot = tempDir.toFile();
    var unknownApp = new UnknownApplication(projectRoot, Set.of());
    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(unknownApp);
    var logs = result.logs();

    assertTrue(result.data().relativeBuildToolFilePaths().isEmpty());
    assertTrue(result.failed());
    assertEquals(MULTIPLE, result.data().buildTool());
    assertFalse(logs.isEmpty());
    assertTrue(
        logs.stream()
            .anyMatch(
                l ->
                    l.getType().equals(ERROR)
                        && l.getMessage()
                            .equals(
                                "Unable to determine the build tool. Detected configuration files"
                                    + " for both Gradle and Maven.")));
  }

  @Test
  @SneakyThrows
  void failed_status_for_project_without_build_tool() {
    var projectRoot = getFile(YML_FILE_PATH).getParentFile();
    var unknownApp = new UnknownApplication(projectRoot, Set.of());
    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(unknownApp);

    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(PROJECT_WITHOUT_TOOLS_PATH)), tempDir);

    var projectJavaRoot = tempDir.toFile();
    var javaApp = new UnknownApplication(projectJavaRoot, Set.of());
    var resultJava = analyzer.analyze(javaApp);
    var logs = resultJava.logs();

    assertTrue(result.failed());
    assertTrue(resultJava.failed());
    assertEquals(NONE, result.data().buildTool());
    assertEquals(NONE, resultJava.data().buildTool());
    assertFalse(logs.isEmpty());
    assertTrue(
        logs.stream()
            .anyMatch(
                l ->
                    l.getType().equals(ERROR)
                        && l.getMessage()
                            .equals(
                                "No build tool detected. Missing Gradle or Maven configuration"
                                    + " files.")));
  }

  @SneakyThrows
  @Test
  void result_canBe_serialized_and_deserialized() {
    // same fixture as test (1)
    var tempDir = createTempDirectory("unzipped_java_project");
    unzipper.apply(new ZipFile(getFile(GRADLE_PROJECT_RESOURCE_PATH)), tempDir);

    var analyzer = new BuildToolAnalyzer();
    var result = analyzer.analyze(new UnknownApplication(tempDir.toFile(), Set.of()));
    BuildToolData data = result.data();
    var se = om.writeValueAsString(data);
    var dese = om.readValue(se, BuildToolData.class);

    assertEquals(GRADLE, dese.buildTool());
    assertEquals(
        Arrays.stream(GRADLE_FILENAMES).map(Path::of).sorted().toList(),
        dese.relativeBuildToolFilePaths().stream().sorted().toList());
  }
}
