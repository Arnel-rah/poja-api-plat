package api.poja.io.file;

public enum ApplicationImportFileType {
  ENV_VARS_FILE("env-vars"),
  GRADLE_BUILD_FILE("gradle-build-files"),
  POJA_CONF_FILE("poja-files"),
  ZIPPED_CODE("zipped-codes"),
  ANALYSIS_RESULT("analysis-results");

  private final String directoryName;

  ApplicationImportFileType(String directoryName) {
    this.directoryName = directoryName;
  }

  public String getDirectoryName() {
    return directoryName;
  }
}
