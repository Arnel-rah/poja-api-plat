package api.poja.io.model.importer.model;

import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.INFO;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.WARNING;
import static java.util.UUID.randomUUID;

import api.poja.io.repository.model.enums.ApplicationImportLogType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class ApplicationImportLog {
  private final String id;
  private final ApplicationImportLogType type;
  private final String message;

  public ApplicationImportLog(ApplicationImportLogType type, String message) {
    this(randomUUID().toString(), type, message);
  }

  public static ApplicationImportLog error(String message) {
    return new ApplicationImportLog(ERROR, message);
  }

  public static ApplicationImportLog warning(String message) {
    return new ApplicationImportLog(WARNING, message);
  }

  public static ApplicationImportLog info(String message) {
    return new ApplicationImportLog(INFO, message);
  }
}
