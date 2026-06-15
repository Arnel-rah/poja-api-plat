package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import static java.nio.file.Files.readString;
import static java.util.UUID.randomUUID;

import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFExtractDepsError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements the {@code ProjectAwareGradlePropertyExtractor} interface. The {{@link
 * #extract(GradleProject)}} method extract all the dependencies for the specified {@link
 * GradleProject}
 */
public final class GradleDependenciesExtractor
    implements ProjectAwareGradlePropertyExtractor<List<GradleDependency>> {
  // "   ,    " and "," are both valid separator
  private static final String GRADLE_DEP_PROPERTY_SEPARATOR = "\\s*,\\s*";

  private final FileWriter fileWriter;
  private final String dependencyLineStart;
  private final String dependencyNullPropMarker;
  private final String taskName;

  public GradleDependenciesExtractor(FileWriter fileWriter) {
    this.fileWriter = fileWriter;
    this.dependencyLineStart = String.format("%s=", randomUUID());
    this.dependencyNullPropMarker = randomUUID().toString();
    this.taskName = String.format("listDependencies%s", randomUUID());
  }

  @Override
  public FallibleResult extract(GradleProject gradleProject) {
    var build = gradleProject.buildFile();

    try {
      var buildContent = readString(build.toPath());
      var newBuildContent = addPropertyExtractorTask(buildContent);

      fileWriter.write(newBuildContent.getBytes(), null, build.getPath());
      gradleProject.refreshProjectFiles(build.toPath());

      try {
        var output = gradleProject.runTask(taskName);
        return new FallibleResult(parseOutputStr(output), List.of(), List.of());
      } finally {
        fileWriter.write(buildContent.getBytes(), null, build.getPath());
      }
    } catch (IOException e) {
      return new FallibleResult(null, List.of(), List.of(new GBFExtractDepsError(build)));
    }
  }

  private List<GradleDependency> parseOutputStr(String s) {
    return Arrays.stream(s.split("\\R"))
        .map(String::trim)
        .map(this::parseOutputLine)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Optional<GradleDependency> parseOutputLine(String line) {
    if (line.length() == dependencyLineStart.length() || !line.startsWith(dependencyLineStart)) {
      return Optional.empty();
    }

    var lineWithoutStart = line.substring(dependencyLineStart.length());
    var parts = lineWithoutStart.split(GRADLE_DEP_PROPERTY_SEPARATOR);
    if (parts.length != 4) {
      return Optional.empty();
    }

    // [config_name,group,name,version?]
    var properties =
        Arrays.stream(parts).map(s -> dependencyNullPropMarker.equals(s) ? null : s).toList();
    // todo: comparable version
    return Optional.of(
        new GradleDependency(
            properties.get(0), properties.get(1), properties.get(2), properties.get(3)));
  }

  private String addPropertyExtractorTask(String s) {
    return String.join("\n", s, listDepsTask());
  }

  private String listDepsTask() {
    return String.format(
        """
        tasks.register("%s") {
            doLast {
                configurations.each { config ->
                    config.dependencies.each { dep ->
                        def group = dep.group ?: "%s"
                        def name = dep.name ?: "%s"
                        def version = dep.version ?: "%s"
                        println "%s${config.name},${group},${name},${version}"
                    }
                }
            }
        }
        """,
        taskName,
        dependencyNullPropMarker,
        dependencyNullPropMarker,
        dependencyNullPropMarker,
        dependencyLineStart);
  }

  public static final class FallibleResult
      extends api.poja.io.model.importer.model.FallibleResult<
          List<GradleDependency>, GBFReadWarning, GBFReadError> {
    public FallibleResult(
        @Nullable List<GradleDependency> value,
        List<GBFReadWarning> warnings,
        List<GBFReadError> errors) {
      super(value, warnings, errors);
    }
  }
}
