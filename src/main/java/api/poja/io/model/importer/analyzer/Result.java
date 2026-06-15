package api.poja.io.model.importer.analyzer;

import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;

import api.poja.io.model.importer.model.ApplicationImportLog;
import java.util.List;

public interface Result<T> {
  enum Status {
    SUCCESS,
    FAILED;
  }

  T data();

  Status status();

  default boolean successful() {
    return SUCCESS.equals(status());
  }

  default boolean failed() {
    return FAILED.equals(status());
  }

  List<ApplicationImportLog> logs();
}
