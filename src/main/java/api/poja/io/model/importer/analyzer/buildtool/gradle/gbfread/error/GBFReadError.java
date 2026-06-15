package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error;

import api.poja.io.model.importer.analyzer.buildtool.gradle.GradleError;
import java.io.File;

public abstract sealed class GBFReadError extends GradleError
    permits GBFExtractDepsError, GBFReadJavaError, GBFExtractPluginsError, GBFExtractRepoError {
  public GBFReadError(File gbf) {
    super(gbf);
  }
}
