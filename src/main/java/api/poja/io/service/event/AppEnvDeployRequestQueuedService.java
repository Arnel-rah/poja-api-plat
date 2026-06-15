package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.enums.IndependentStacksStateEnum.NOT_READY;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELING;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_INITIATED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_QUEUED;
import static api.poja.io.model.CancelResult.NEEDS_BACKOFF;
import static api.poja.io.model.CancelResult.RESUMABLE;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.AppEnvDeployRequestQueued;
import api.poja.io.endpoint.event.model.AppEnvDeployRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.EnvBuildRequest;
import api.poja.io.service.AppEnvDeployCancelService;
import api.poja.io.service.AppEnvironmentDeploymentService;
import api.poja.io.service.EnvBuildRequestService;
import api.poja.io.service.workflows.DeploymentStateService;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AppEnvDeployRequestQueuedService implements Consumer<AppEnvDeployRequestQueued> {
  private final EventProducer<PojaEvent> eventProducer;
  private final AppEnvironmentDeploymentService appEnvDeploymentService;
  private final DeploymentStateService deploymentStateService;
  private final EnvBuildRequestService envBuildRequestService;
  private final AppEnvDeployCancelService cancelService;

  @Override
  public void accept(AppEnvDeployRequestQueued appEnvDeployRequestQueued) {
    AppEnvDeployRequested appEnvDeployRequested =
        appEnvDeployRequestQueued.getAppEnvDeployRequested();
    String appId = appEnvDeployRequested.getAppId();
    String appEnvDeploymentId = appEnvDeployRequested.getAppEnvDeploymentId();
    DeploymentStateEnum progressionStatus =
        deploymentStateService
            .getLatestDeploymentStateByDeploymentId(appEnvDeploymentId)
            .getProgressionStatus();
    if (CANCELED.equals(progressionStatus)) {
      log.info("deployment {} has been canceled", appEnvDeploymentId);
      return;
    }
    if (CANCELING.equals(progressionStatus)) {
      log.info(
          "deployment has reached CANCELING state, progression is stopped at {} for this"
              + " deployment {}",
          INDEPENDENT_STACKS_DEPLOYMENT_QUEUED,
          appEnvDeploymentId);
      deploymentStateService.save(appEnvDeployRequested.getAppId(), appEnvDeploymentId, CANCELED);
      return;
    }
    List<AppEnvironmentDeployment> pendingDepls =
        appEnvDeploymentService.findAllPendingDeployments(appId, appEnvDeployRequested.getEnvId());
    if (!pendingDepls.isEmpty()) {
      AppEnvironmentDeployment toDepl = pendingDepls.getLast();
      var currentDeplStateOpt = toDepl.currentState();
      boolean isToDeplCurrentDepl = Objects.equals(toDepl.getId(), appEnvDeploymentId);
      if (currentDeplStateOpt.isPresent()
          && INDEPENDENT_STACKS_DEPLOYMENT_QUEUED.equals(
              currentDeplStateOpt.get().getProgressionStatus())) {
        if (!isToDeplCurrentDepl) {
          deploymentStateService.save(appId, appEnvDeploymentId, CANCELED);
          return;
        }
        cancelDepls(
            appEnvDeployRequested.getEnvId(), pendingDepls.subList(0, pendingDepls.size() - 1));
        log.info("deploying queued appEnvDepl.id = {}", toDepl.getId());
        BuiltEnvInfo builtEnvInfo = getBuiltEnvInfo(toDepl);
        deploymentStateService.save(
            toDepl.getAppId(), toDepl.getId(), INDEPENDENT_STACKS_DEPLOYMENT_INITIATED);
        eventProducer.accept(
            List.of(getAppEnvDeployRequestedEvent(appEnvDeployRequested, builtEnvInfo, toDepl)));
        return;
      }
      if (!isToDeplCurrentDepl) {
        log.info("a more recent depl {} is already running", toDepl.getId());
      }
    }
  }

  private void cancelDepls(String envId, List<AppEnvironmentDeployment> deplsToCancel) {
    if (deplsToCancel.isEmpty()) {
      return;
    }
    log.info("cancel {} previous deployments", deplsToCancel.size());
    var cancelResult = cancelService.apply(envId, deplsToCancel);
    if (NEEDS_BACKOFF.equals(cancelResult)) {
      throw new EventConsumptionBackOffException("backOff waiting for all depls to be canceled");
    }
    if (RESUMABLE.equals(cancelResult)) {
      return;
    }
  }

  private static AppEnvDeployRequested getAppEnvDeployRequestedEvent(
      AppEnvDeployRequested appEnvDeployRequested,
      BuiltEnvInfo builtEnvInfo,
      AppEnvironmentDeployment toDepl) {
    return AppEnvDeployRequested.builder()
        .orgId(appEnvDeployRequested.getOrgId())
        .builtEnvInfo(builtEnvInfo)
        .deploymentConfId(toDepl.getEnvDeplConfId())
        .requestInstant(now())
        .builtZipFormattedFilekey(builtEnvInfo.getFormattedBucketKey())
        .envId(toDepl.getEnv().getId())
        .appId(toDepl.getAppId())
        .currentIndependentStacksState(NOT_READY)
        .appEnvDeploymentId(toDepl.getId())
        .build();
  }

  private BuiltEnvInfo getBuiltEnvInfo(AppEnvironmentDeployment appEnvironmentDeployment) {
    EnvBuildRequest buildRequest =
        envBuildRequestService.getByAppEnvDeplId(appEnvironmentDeployment.getId());
    return new BuiltEnvInfo()
        .id(buildRequest.getId())
        .appEnvDeploymentId(appEnvironmentDeployment.getId())
        .commitSha(appEnvironmentDeployment.getGhCommitSha())
        .environmentType(appEnvironmentDeployment.getEnv().getEnvironmentType())
        .formattedBucketKey(buildRequest.getBuiltZipFileKey());
  }
}
