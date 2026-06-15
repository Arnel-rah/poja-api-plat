package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error;

import java.io.File;

public final class CouldNotReadJavaSourceVersionError extends GBFReadJavaError {
  public CouldNotReadJavaSourceVersionError(File gbf) {
    super(gbf);
  }
}
