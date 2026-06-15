package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_IN_PROGRESS;

import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.EnvironmentDeploymentRequested;
import api.poja.io.service.AppSetupStateService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EnvironmentDeploymentRequestedService
    implements Consumer<EnvironmentDeploymentRequested> {
  private final EnvironmentService environmentService;
  private final ApplicationService applicationService;
  private final AppSetupStateService appSetupStateService;

  @Override
  public void accept(EnvironmentDeploymentRequested event) {
    var app = applicationService.getById(event.getAppId());
    if (app.getGithubRepositoryUrl() == null) {
      throw new EventConsumptionBackOffException("repository url is missing.");
    }
    log.info("ENV_DEPLOYMENT_INITIATION_IN_PROGRESS for app.environment.id={}", event.getEnvId());
    appSetupStateService.save(
        event.getOrgId(), event.getAppId(), ENV_DEPLOYMENT_INITIATION_IN_PROGRESS);
    try {
      environmentService.deployEnvWithConf(
          app.getOrgId(), event.getAppId(), event.getEnvId(), event.getEnvDeplConfId());
      appSetupStateService.save(event.getOrgId(), event.getAppId(), ENV_DEPLOYMENT_INITIATED);
    } catch (Exception e) {
      log.error("", e);
      appSetupStateService.save(
          event.getOrgId(), event.getAppId(), ENV_DEPLOYMENT_INITIATION_FAILED);
      throw e;
    }
  }
}
