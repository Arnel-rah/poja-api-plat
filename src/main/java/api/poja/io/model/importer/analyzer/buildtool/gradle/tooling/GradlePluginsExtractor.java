package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import static java.nio.file.Files.readString;
import static java.util.UUID.randomUUID;

import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFExtractPluginsError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradlePlugin;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements the {@code ProjectAwareGradlePropertyExtractor} interface. The {{@link
 * #extract(GradleProject)}} method extract all the plugins for the specified {@link GradleProject}
 */
public final class GradlePluginsExtractor
    implements ProjectAwareGradlePropertyExtractor<List<GradlePlugin>> {
  // "   ,    " and "," are both valid separator
  private static final String GRADLE_PLUGIN_PROP_SEPARATOR = "\\s*,\\s*";

  private final FileWriter fileWriter;
  private final String taskName;
  private final String pluginLineStart;
  private final String pluginNullPropMarker;

  public GradlePluginsExtractor(FileWriter fileWriter) {
    this.fileWriter = fileWriter;
    this.taskName = String.format("dummyTask%s", randomUUID());
    this.pluginLineStart = String.format("%s=", randomUUID());
    this.pluginNullPropMarker = randomUUID().toString();
  }

  /**
   * @implNote Plugin IDs and versions are only available during plugin requests in settings.gradle.
   *     After that phase, only implementation class names remain. To capture them, we inject a hook
   *     that prints the info and then run a dummy task to trigger its execution.
   */
  @Override
  public FallibleResult extract(GradleProject gradleProject) {
    var build = gradleProject.buildFile();
    var settings = gradleProject.settingsFile();

    try {
      var buildFileStr = readString(build.toPath());
      var settingsFileStr = settings.exists() ? readString(settings.toPath()) : "";

      var newBuildContent = String.join("\n", buildFileStr, dummyTask());
      // note: This hook must be placed at the very top of settings.gradle
      var newSettingsContent = String.join("\n", listPluginsHook(), settingsFileStr);

      fileWriter.write(newBuildContent.getBytes(), null, build.getPath());
      fileWriter.write(newSettingsContent.getBytes(), null, settings.getPath());
      gradleProject.refreshProjectFiles(build.toPath(), settings.toPath());

      try {
        var output = gradleProject.runTask(taskName);
        return new FallibleResult(parseOutputStr(output), List.of(), List.of());
      } finally {
        fileWriter.write(buildFileStr.getBytes(), null, build.getPath());
        fileWriter.write(settingsFileStr.getBytes(), null, settings.getPath());
      }
    } catch (IOException e) {
      return new FallibleResult(null, List.of(), List.of(new GBFExtractPluginsError(build)));
    }
  }

  private List<GradlePlugin> parseOutputStr(String s) {
    return Arrays.stream(s.split("\\R"))
        .map(String::trim)
        .map(this::parseOutputLine)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Optional<GradlePlugin> parseOutputLine(String line) {
    if (line.length() == pluginLineStart.length() || !line.startsWith(pluginLineStart)) {
      return Optional.empty();
    }

    var lineWithoutStart = line.substring(pluginLineStart.length());
    var parts = lineWithoutStart.split(GRADLE_PLUGIN_PROP_SEPARATOR);
    if (parts.length != 2) {
      return Optional.empty();
    }

    // [id,version?]
    var properties =
        Arrays.stream(parts).map(s -> pluginNullPropMarker.equals(s) ? null : s).toList();
    return Optional.of(new GradlePlugin(properties.get(0), properties.get(1)));
  }

  private String dummyTask() {
    return String.format(
        """
        tasks.register("%s") {
            doLast { }
        }
        """,
        taskName);
  }

  private String listPluginsHook() {
    return String.format(
        """
        pluginManagement {
            resolutionStrategy {
                eachPlugin {details ->
                    def req = details.requested
                    println "%s${req.id.id ?: "%s"},${req.version ?: "%s"}"
                }
            }
        }
        """,
        pluginLineStart, pluginNullPropMarker, pluginNullPropMarker);
  }

  public static final class FallibleResult
      extends api.poja.io.model.importer.model.FallibleResult<
          List<GradlePlugin>, GBFReadWarning, GBFReadError> {
    public FallibleResult(
        @Nullable List<GradlePlugin> value,
        List<GBFReadWarning> warnings,
        List<GBFReadError> errors) {
      super(value, warnings, errors);
    }
  }
}
