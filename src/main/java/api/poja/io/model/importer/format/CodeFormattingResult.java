package api.poja.io.model.importer.format;

import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;

import api.poja.io.model.importer.analyzer.AbstractResult;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.CodeFormattingData;
import java.nio.file.Path;
import java.util.List;

public class CodeFormattingResult extends AbstractResult<CodeFormattingData> {
  private final Path codeFormatted;

  public CodeFormattingResult(Path codeFormatted, List<ApplicationImportLog> logs) {
    super(logs);
    this.codeFormatted = codeFormatted;
  }

  @Override
  public CodeFormattingData data() {
    return new CodeFormattingData(codeFormatted);
  }

  @Override
  public Status status() {
    return logs().stream().anyMatch(log -> log.getType() == ERROR) ? FAILED : SUCCESS;
  }
}
