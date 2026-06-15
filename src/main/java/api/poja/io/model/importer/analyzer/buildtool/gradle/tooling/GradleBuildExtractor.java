package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.importer.model.FallibleResult;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * This class implements the {@code ProjectAwareGradlePropertyExtractor} interface. The {{@link
 * #extract(GradleProject)}} method extracts the `GradleBuild` for the specified `build.gradle`
 * {@link GradleBuild}
 */
@AllArgsConstructor
public final class GradleBuildExtractor
    implements ProjectAwareGradlePropertyExtractor<GradleBuild> {

  // Default instance for convenience
  public static final GradleBuildExtractor DEFAULT;

  static {
    var fileWriter = new FileWriter(new ExtensionGuesser());
    DEFAULT =
        new GradleBuildExtractor(
            new GradleJavaPropertyExtractor(),
            new GradlePluginsExtractor(fileWriter),
            new GradleDependenciesExtractor(fileWriter),
            new GradleRepositoriesExtractor(fileWriter));
  }

  private final GradleJavaPropertyExtractor javaPropertyExtractor;
  private final GradlePluginsExtractor pluginsExtractor;
  private final GradleDependenciesExtractor dependenciesExtractor;
  private final GradleRepositoriesExtractor repositoriesExtractor;

  @Override
  public FallibleResult<GradleBuild, GBFReadWarning, GBFReadError> extract(GradleProject project) {
    var pluginsResult = pluginsExtractor.extract(project);
    var javaPropertyResult = javaPropertyExtractor.extract(project.buildFile());
    var dependenciesResult = dependenciesExtractor.extract(project);
    var repositoriesResult = repositoriesExtractor.extract(project);

    var results =
        List.of(pluginsResult, javaPropertyResult, dependenciesResult, repositoriesResult);
    List<GBFReadError> errors = results.stream().flatMap(e -> e.errors().stream()).toList();
    List<GBFReadWarning> warnings = results.stream().flatMap(e -> e.warnings().stream()).toList();

    var gradleBuild =
        GradleBuild.builder()
            .file(project.directory())
            .dependencies(dependenciesResult.value())
            .repositories(repositoriesResult.value())
            .plugins(pluginsResult.value())
            .javaConfig(javaPropertyResult.value())
            .build();
    return new FallibleResult<>(gradleBuild, warnings, errors);
  }

  public static GradleBuildExtractor defaultGradleBuildExtractor() {
    return DEFAULT;
  }
}
