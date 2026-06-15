package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELING;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYED;

import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.model.CancelResult;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.DeploymentState;
import api.poja.io.service.stack.StackCloudService;
import api.poja.io.service.workflows.DeploymentStateService;
import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AppEnvDeployCancelService
    implements BiFunction<String, List<AppEnvironmentDeployment>, CancelResult> {
  private final DeploymentStateService deploymentStateService;
  private final StackCloudService stackCloudService;

  @Override
  public CancelResult apply(String envId, List<AppEnvironmentDeployment> appEnvDepls) {
    appEnvDepls.forEach(
        (deployment) -> {
          var appEnvDeplId = deployment.getId();
          DeploymentState latestState =
              deploymentStateService.getLatestDeploymentStateByDeploymentId(appEnvDeplId);
          DeploymentStateEnum progressionStatus = latestState.getProgressionStatus();
          if (CANCELING.equals(progressionStatus)) {
            return;
          }
          if (CANCELED.equals(progressionStatus)) {
            return;
          }
          if (COMPUTE_STACK_DEPLOYED.equals(progressionStatus)) {
            return;
          }
          deploymentStateService.save(deployment.getAppId(), appEnvDeplId, CANCELING);
        });
    return stackCloudService.cancelStackDepl(envId);
  }
}
