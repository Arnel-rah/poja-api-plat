package api.poja.io.model.importer.analyzer.lang;

import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;

import api.poja.io.model.importer.analyzer.AbstractResult;
import api.poja.io.model.importer.model.ApplicationImportLog;
import java.nio.file.Path;
import java.util.stream.Stream;

public class AppLangAnalyzerResult extends AbstractResult<AppLangAnalyzerData> {
  private final Path mainMethodPath;

  public AppLangAnalyzerResult(Path mainMethodPath, ApplicationImportLog... logs) {
    super(Stream.of(logs).toList());
    this.mainMethodPath = mainMethodPath;
  }

  @Override
  public AppLangAnalyzerData data() {
    return new AppLangAnalyzerData(mainMethodPath);
  }

  @Override
  public Status status() {
    return mainMethodPath != null ? SUCCESS : FAILED;
  }
}
