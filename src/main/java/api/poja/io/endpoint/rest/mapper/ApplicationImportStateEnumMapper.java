package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationImportStateEnum;
import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import org.springframework.stereotype.Component;

@Component
public class ApplicationImportStateEnumMapper {
  public ApplicationImportStateEnum toRest(ApplicationImportStateStatus domain) {
    return ApplicationImportStateEnum.fromValue(domain.name());
  }
}
