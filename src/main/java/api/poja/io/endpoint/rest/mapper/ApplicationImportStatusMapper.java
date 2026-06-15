package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationImportStatusEnum;
import api.poja.io.repository.model.enums.ApplicationImportStatus;
import org.springframework.stereotype.Component;

@Component
public class ApplicationImportStatusMapper {
  public ApplicationImportStatusEnum toRest(ApplicationImportStatus domain) {
    return switch (domain) {
      case PENDING -> ApplicationImportStatusEnum.PENDING;
      case IN_PROGRESS -> ApplicationImportStatusEnum.IN_PROGRESS;
      case SUCCESSFUL -> ApplicationImportStatusEnum.SUCCESSFUL;
      case FAILED -> ApplicationImportStatusEnum.FAILED;
    };
  }
}
