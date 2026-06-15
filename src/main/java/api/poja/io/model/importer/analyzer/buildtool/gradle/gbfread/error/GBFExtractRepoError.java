package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error;

import java.io.File;

public final class GBFExtractRepoError extends GBFReadError {
  public GBFExtractRepoError(File gbf) {
    super(gbf);
  }
}
