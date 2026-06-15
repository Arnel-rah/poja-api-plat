package api.poja.io.model.importer.analyzer.buildtool.gradle;

import api.poja.io.model.importer.UncheckedFileContentReader;
import java.io.File;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GradleWarning {
  private final File gbf;

  public String gbfContent() {
    return new UncheckedFileContentReader().apply(gbf);
  }
}
