package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationImportLogTypeEnum;
import api.poja.io.repository.model.enums.ApplicationImportLogType;
import org.springframework.stereotype.Component;

@Component
public class ApplicationImportLogTypeMapper {
  public ApplicationImportLogTypeEnum toRest(ApplicationImportLogType domain) {
    return ApplicationImportLogTypeEnum.fromValue(domain.toString());
  }
}
