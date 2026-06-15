package api.poja.io.model.importer.model;

import api.poja.io.model.EnvVar;
import java.io.File;
import java.util.Set;

public record UnknownApplication(File file, Set<EnvVar> envVars) {
  public UnknownApplication(File file) {
    this(file, Set.of());
  }
}
