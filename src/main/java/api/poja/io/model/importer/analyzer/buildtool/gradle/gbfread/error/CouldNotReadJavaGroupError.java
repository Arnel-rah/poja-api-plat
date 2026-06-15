package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error;

import java.io.File;

public final class CouldNotReadJavaGroupError extends GBFReadJavaError {
  public CouldNotReadJavaGroupError(File gbf) {
    super(gbf);
  }
}
