package api.poja.io.model.importer;

import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.BUILD_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.SETTINGS_GRADLE;
import static java.nio.file.Files.createTempDirectory;

import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleProject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class TestUtils {

  public static FileWriter fileWriter = new FileWriter(new ExtensionGuesser());
  static FileUnzipper fileUnzipper = new FileUnzipper(fileWriter);
  static final String MOCK_PROJECT_DIRECTORY_PATH = "files/import/gradle/project_mock";

  @SneakyThrows
  public static void mockGradleProject(String directoryPath, Consumer<GradleProject> consumer) {
    var projectDirectory = getMockProjectPath(Path.of(directoryPath)).toFile();
    var tempDir = createTempDirectory("project_clone").toFile();

    Files.copy(
        Path.of(projectDirectory.getPath(), BUILD_GRADLE),
        Path.of(tempDir.getPath(), BUILD_GRADLE));

    var projectSettingsFilePath = Path.of(projectDirectory.getPath(), SETTINGS_GRADLE);
    var destSettingsFilePath = Path.of(tempDir.getPath(), SETTINGS_GRADLE);
    if (projectSettingsFilePath.toFile().exists()) {
      Files.copy(projectSettingsFilePath, destSettingsFilePath);
    } else {
      Files.createFile(destSettingsFilePath);
    }

    var gradleDist = getGradleDist();
    try (var gradleProject = new GradleProject(tempDir.toPath(), gradleDist)) {
      consumer.accept(gradleProject);
    }
  }

  @SneakyThrows
  public static Path getMockProjectPath(Path suffix) {
    return new ClassPathResource(Path.of(MOCK_PROJECT_DIRECTORY_PATH, suffix.toString()).toString())
        .getFile()
        .toPath();
  }

  @SneakyThrows
  static File getGradleDist() {
    var file = new ClassPathResource("files/gradle-dist/gradle-8.5-bin.zip").getFile();
    var tempDir = createTempDirectory("gradle-dist");
    fileUnzipper.apply(new ZipFile(file), tempDir);
    return tempDir.resolve("gradle-8.5").toFile();
  }
}
