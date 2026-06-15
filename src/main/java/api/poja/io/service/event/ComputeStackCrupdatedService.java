package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.enums.StackCrupdateStatus.CRUPDATE_FAILED;
import static api.poja.io.endpoint.event.model.enums.StackCrupdateStatus.CRUPDATE_SUCCESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELING;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYMENT_FAILED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE;
import static api.poja.io.service.event.AppEnvDeployRequestedService.EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.ComputeStackCrupdated;
import api.poja.io.endpoint.event.model.StackCrupdated;
import api.poja.io.endpoint.event.model.enums.StackCrupdateStatus;
import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.AppEnvironmentDeploymentService;
import api.poja.io.service.stack.StackCloudService;
import api.poja.io.service.stack.StackService;
import api.poja.io.service.workflows.DeploymentStateService;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ComputeStackCrupdatedService implements Consumer<ComputeStackCrupdated> {
  private final DeploymentStateService deploymentStateService;
  private final EventProducer<StackCrupdated> stackCrupdatedEventProducer;
  private final StackService stackService;
  private final StackCloudService stackCloudService;
  private final AppEnvironmentDeploymentService appEnvironmentDeploymentService;

  @Override
  public void accept(ComputeStackCrupdated computeStackCrupdated) {
    String appEnvDeploymentId = computeStackCrupdated.getAppEnvDeploymentId();
    DeploymentStateEnum latestDeploymentState =
        deploymentStateService
            .getLatestDeploymentStateByDeploymentId(appEnvDeploymentId)
            .getProgressionStatus();
    if (CANCELED.equals(latestDeploymentState)) {
      log.info("deployment {} has been canceled", appEnvDeploymentId);
      return;
    }
    if (!CANCELING.equals(latestDeploymentState)
        && !COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS.equals(latestDeploymentState)) {
      log.info(
          EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES,
          appEnvDeploymentId,
          COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS,
          latestDeploymentState);
      return;
    }
    StackCrupdateStatus stackDeploymentState = computeStackCrupdated.getStackDeploymentState();
    String orgId = computeStackCrupdated.getOrgId();
    var stack = getStack(computeStackCrupdated, stackDeploymentState);
    if (CRUPDATE_SUCCESS.equals(stackDeploymentState)) {
      String apiUrl = computeStackCrupdated.getApiUrl();
      if (apiUrl != null) {
        appEnvironmentDeploymentService.updateDeployedUri(
            computeStackCrupdated.getAppEnvDeploymentId(), URI.create(apiUrl));
      }
      saveStackStateAndFireEvents(
          computeStackCrupdated, appEnvDeploymentId, COMPUTE_STACK_DEPLOYED, orgId, stack);
    } else if (CRUPDATE_FAILED.equals(stackDeploymentState)) {
      saveStackStateAndFireEvents(
          computeStackCrupdated, appEnvDeploymentId, COMPUTE_STACK_DEPLOYMENT_FAILED, orgId, stack);
      if (CANCELING.equals(latestDeploymentState)) {
        log.info(
            "deployment has reached CANCELING state, progression is stopped at {} for this"
                + " deployment {}",
            COMPUTE_STACK_DEPLOYMENT_FAILED,
            appEnvDeploymentId);
        deploymentStateService.save(computeStackCrupdated.getAppId(), appEnvDeploymentId, CANCELED);
      }
    }
  }

  private void saveStackStateAndFireEvents(
      ComputeStackCrupdated computeStackCrupdated,
      String appEnvDeploymentId,
      DeploymentStateEnum computeStackDeploymentState,
      String orgId,
      Stack stack) {
    deploymentStateService.save(
        computeStackCrupdated.getAppId(), appEnvDeploymentId, computeStackDeploymentState);
    stackCrupdatedEventProducer.accept(
        List.of(
            /* TODO: forced to set parentAppEnvDeployRequested to null, should find a better way to resend this event as ComputeStack is received from deployer, yet we need appEnvDepl info and appEnvDepl event*/
            StackCrupdated.builder()
                .orgId(orgId)
                .stack(stack)
                .parentAppEnvDeployRequested(null)
                .appEnvDeplId(appEnvDeploymentId)
                .build()));
  }

  private Stack getStack(
      ComputeStackCrupdated computeStackCrupdated, StackCrupdateStatus stackDeploymentState) {
    String applicationId = computeStackCrupdated.getAppId();
    String environmentId = computeStackCrupdated.getEnvId();
    String stackName = computeStackCrupdated.getStackName();
    Optional<String> cfStackId = stackCloudService.getCloudformationStackId(stackName);
    if (cfStackId.isPresent() || CRUPDATE_FAILED.equals(stackDeploymentState)) {
      return stackService.save(
          Stack.builder()
              .name(stackName)
              .cfStackId(cfStackId.orElse(null))
              .applicationId(applicationId)
              .environmentId(environmentId)
              .type(COMPUTE)
              .appEnvDeplId(computeStackCrupdated.getAppEnvDeploymentId())
              .build());
    }
    throw new RuntimeException("unexpected error, stack should have been created already");
  }
}
