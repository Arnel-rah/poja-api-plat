package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum.COMPLETED;
import static api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum.FAILED;
import static api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum.IN_PROGRESS;

import api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum;
import api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class OrganizationSetupStateEnumStatusMapper {
  public OrganizationSetupStatusEnum toRest(OrganizationSetupStateStatusEnum domain) {
    return switch (domain) {
      case ORGANIZATION_SETUP_IN_PROGRESS -> IN_PROGRESS;
      case ORGANIZATION_SETUP_COMPLETED -> COMPLETED;
      case ORGANIZATION_SETUP_FAILED -> FAILED;
    };
  }
}
