package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error;

import java.io.File;

public sealed class GBFReadJavaError extends GBFReadError
    permits CouldNotReadJavaGroupError, CouldNotReadJavaSourceVersionError {
  public GBFReadJavaError(File gbf) {
    super(gbf);
  }
}
