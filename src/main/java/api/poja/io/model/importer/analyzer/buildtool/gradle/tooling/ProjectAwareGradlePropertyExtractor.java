package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

/**
 * Represents a Gradle property extractor that operates on a {@link GradleProject}
 *
 * @param <T> The property to extract
 */
public non-sealed interface ProjectAwareGradlePropertyExtractor<T>
    extends GradlePropertyExtractor<GradleProject, T> {}
