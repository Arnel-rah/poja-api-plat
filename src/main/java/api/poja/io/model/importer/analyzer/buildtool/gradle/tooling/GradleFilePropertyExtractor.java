package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import java.io.File;

/**
 * Represents a Gradle property extractor that operates on a {@code String}
 *
 * @param <T> The property to extract
 */
public non-sealed interface GradleFilePropertyExtractor<T>
    extends GradlePropertyExtractor<File, T> {}
