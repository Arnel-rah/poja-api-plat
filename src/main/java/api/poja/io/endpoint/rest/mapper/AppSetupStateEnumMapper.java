package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.ENV_CREATION_FAILED;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.ENV_CREATION_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.ENV_CREATION_SUCCESS;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATED;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_FAILED;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.REPO_CREATION_FAILED;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.REPO_CREATION_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.AppSetupStateEnum.REPO_CREATION_SUCCESS;

import api.poja.io.endpoint.rest.model.AppSetupStateEnum;
import org.springframework.stereotype.Component;

@Component
public class AppSetupStateEnumMapper {
  public AppSetupStateEnum toRest(api.poja.io.repository.model.enums.AppSetupStateEnum domain) {
    return switch (domain) {
      case ENV_CREATION_IN_PROGRESS -> ENV_CREATION_IN_PROGRESS;
      case ENV_CREATION_FAILED -> ENV_CREATION_FAILED;
      case ENV_CREATION_SUCCESS -> ENV_CREATION_SUCCESS;
      case REPO_CREATION_IN_PROGRESS -> REPO_CREATION_IN_PROGRESS;
      case REPO_CREATION_FAILED -> REPO_CREATION_FAILED;
      case REPO_CREATION_SUCCESS -> REPO_CREATION_SUCCESS;
      case ENV_DEPLOYMENT_INITIATION_FAILED -> ENV_DEPLOYMENT_INITIATION_FAILED;
      case ENV_DEPLOYMENT_INITIATION_IN_PROGRESS -> ENV_DEPLOYMENT_INITIATION_IN_PROGRESS;
      case ENV_DEPLOYMENT_INITIATED -> ENV_DEPLOYMENT_INITIATED;
    };
  }
}
