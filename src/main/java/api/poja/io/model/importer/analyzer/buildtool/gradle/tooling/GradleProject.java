package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleTask;

@Getter
@Slf4j
@Accessors(fluent = true)
public class GradleProject implements AutoCloseable {
  private final File directory;
  private final File buildFile;
  private final File settingsFile;
  private final GradleConnector connector;
  private final ProjectConnection connection;

  public GradleProject(Path root, File gradleDist) {
    this.directory = root.toFile();
    this.buildFile = new File(directory, "build.gradle");
    this.settingsFile = new File(directory, "settings.gradle");
    this.connector = newGradleConnector(directory, gradleDist);
    this.connection = connector.connect();
  }

  public GradleProject(Path root) {
    this.directory = root.toFile();
    this.buildFile = new File(directory, "build.gradle");
    this.settingsFile = new File(directory, "settings.gradle");
    this.connector = newGradleConnector(directory);
    this.connection = connector.connect();
  }

  /** Returns the task run output */
  public String runTask(String taskName) {
    try (var out = new ByteArrayOutputStream()) {
      runTask(taskName, out);
      return out.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to run task: " + taskName, e);
    }
  }

  /**
   * Executes the task while sending the standard output logging to the specified {@link
   * OutputStream}
   *
   * @param taskName The task
   * @param outputStream The output stream which should receive standard output logging generated
   *     while running the task
   */
  private void runTask(String taskName, OutputStream outputStream) {
    var task = getGradleTask(taskName);
    try {
      connection.newBuild().forTasks(task).setStandardOutput(outputStream).run();
    } finally {
      try {
        outputStream.flush();
      } catch (IOException e) {
        log.error("Failed to flush output stream", e);
      }
    }
  }

  private Optional<? extends GradleTask> findGradleTask(String taskName) {
    var project = connection.getModel(org.gradle.tooling.model.GradleProject.class);
    return project.getTasks().stream().filter(t -> t.getName().equals(taskName)).findFirst();
  }

  private GradleTask getGradleTask(String taskName) {
    return findGradleTask(taskName)
        .orElseThrow(
            () -> new RuntimeException(String.format("GradleTask.name=%s not found", taskName)));
  }

  private static GradleConnector newGradleConnector(File projectDir) {
    if (!projectDir.exists()) {
      throw new IllegalArgumentException("Project directory does not exist: " + projectDir);
    }
    return GradleConnector.newConnector().forProjectDirectory(projectDir).useGradleVersion("8.5");
  }

  private static GradleConnector newGradleConnector(File projectDir, File gradleDist) {
    if (!projectDir.exists()) {
      throw new IllegalArgumentException("Project directory does not exist: " + projectDir);
    }
    return GradleConnector.newConnector()
        .forProjectDirectory(projectDir)
        .useInstallation(gradleDist);
  }

  /**
   * Should be invoked on every change done by external process
   *
   * @param paths Paths which have been changed by external process
   */
  public void refreshProjectFiles(Path... paths) {
    connection.notifyDaemonsAboutChangedPaths(List.of(paths));
  }

  @Override
  public void close() {
    connection.close();
    connector.disconnect();
  }
}
