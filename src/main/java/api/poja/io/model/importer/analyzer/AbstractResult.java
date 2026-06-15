package api.poja.io.model.importer.analyzer;

import api.poja.io.model.importer.model.ApplicationImportLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractResult<T> implements Result<T> {
  private final List<ApplicationImportLog> logs;

  protected AbstractResult(Collection<ApplicationImportLog> logs) {
    this.logs = new ArrayList<>(logs);
  }

  protected AbstractResult() {
    this(new ArrayList<>());
  }

  protected void logError(String errorMessage) {
    log.error(errorMessage);
    log(ApplicationImportLog.error(errorMessage));
  }

  protected void logError(String errorMessage, Throwable throwable) {
    log.error(errorMessage, throwable);
    log(ApplicationImportLog.error(errorMessage));
  }

  protected void logWarning(String warningMessage) {
    log.warn(warningMessage);
    log(ApplicationImportLog.warning(warningMessage));
  }

  protected void logInfo(String infoMessage) {
    log.info(infoMessage);
    log(ApplicationImportLog.info(infoMessage));
  }

  protected void log(ApplicationImportLog log) {
    this.logs.add(log);
  }

  @Override
  public final List<ApplicationImportLog> logs() {
    return logs;
  }
}
