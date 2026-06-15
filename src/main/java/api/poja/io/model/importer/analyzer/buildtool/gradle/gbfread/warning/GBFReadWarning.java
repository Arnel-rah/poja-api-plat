package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning;

import api.poja.io.model.importer.analyzer.buildtool.gradle.GradleWarning;
import java.io.File;

public abstract sealed class GBFReadWarning extends GradleWarning
    permits CouldNotReadJavaTargetVersionWarning {
  public GBFReadWarning(File gbf) {
    super(gbf);
  }
}
