package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error;

import java.io.File;

public final class GBFExtractDepsError extends GBFReadError {
  public GBFExtractDepsError(File gbf) {
    super(gbf);
  }
}
