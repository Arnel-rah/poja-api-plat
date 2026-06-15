package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.model.FallibleResult;

public sealed interface GradlePropertyExtractor<T, U>
    permits ProjectAwareGradlePropertyExtractor, GradleFilePropertyExtractor {
  FallibleResult<U, GBFReadWarning, GBFReadError> extract(T input);
}
