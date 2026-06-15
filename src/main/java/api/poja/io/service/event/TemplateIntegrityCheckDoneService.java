package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.enums.IndependentStacksStateEnum.NOT_READY;
import static api.poja.io.endpoint.event.model.enums.TemplateIntegrityStatus.AUTHENTIC;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.*;
import static api.poja.io.service.event.AppEnvDeployRequestedService.EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppEnvDeployRequestQueued;
import api.poja.io.endpoint.event.model.AppEnvDeployRequested;
import api.poja.io.endpoint.event.model.TemplateIntegrityCheckDone;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.service.workflows.DeploymentStateService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TemplateIntegrityCheckDoneService implements Consumer<TemplateIntegrityCheckDone> {
  private final EventProducer<AppEnvDeployRequestQueued> eventProducer;
  private final DeploymentStateService deploymentStateService;

  @Override
  public void accept(TemplateIntegrityCheckDone templateIntegrityCheckDone) {
    String orgId = templateIntegrityCheckDone.getOrgId();
    String appId = templateIntegrityCheckDone.getAppId();
    String envId = templateIntegrityCheckDone.getEnvId();
    String appEnvDeploymentId = templateIntegrityCheckDone.getAppEnvDeploymentId();
    DeploymentStateEnum latestState =
        deploymentStateService
            .getLatestDeploymentStateByDeploymentId(
                templateIntegrityCheckDone.getAppEnvDeploymentId())
            .getProgressionStatus();
    if (!TEMPLATE_FILE_CHECK_IN_PROGRESS.equals(latestState)) {
      log.info(
          EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES,
          appEnvDeploymentId,
          TEMPLATE_FILE_CHECK_IN_PROGRESS,
          latestState);
      return;
    }
    if (AUTHENTIC.equals(templateIntegrityCheckDone.getStatus())) {
      handleAuthenticTemplateFile(
          orgId,
          envId,
          appId,
          templateIntegrityCheckDone.getBuiltEnvInfo(),
          templateIntegrityCheckDone.getDeploymentConfId(),
          templateIntegrityCheckDone.getBuiltProjectBucketKey(),
          templateIntegrityCheckDone.getAppEnvDeploymentId());
    } else {
      handleCorruptedTemplateFile(appId, appEnvDeploymentId);
    }
  }

  private void handleCorruptedTemplateFile(String appId, String appEnvDeploymentId) {
    deploymentStateService.save(appId, appEnvDeploymentId, TEMPLATE_FILE_CHECK_FAILED);
  }

  private void handleAuthenticTemplateFile(
      String orgId,
      String envId,
      String appId,
      BuiltEnvInfo builtEnvInfo,
      String deploymentConfId,
      String builtProjectBucketKey,
      String appEnvDeploymentId) {
    eventProducer.accept(
        List.of(
            AppEnvDeployRequestQueued.builder()
                .appEnvDeployRequested(
                    getAppEnvDeployRequested(
                        orgId,
                        envId,
                        appId,
                        builtEnvInfo,
                        deploymentConfId,
                        builtProjectBucketKey,
                        appEnvDeploymentId))
                .build()));
    deploymentStateService.save(appId, appEnvDeploymentId, INDEPENDENT_STACKS_DEPLOYMENT_QUEUED);
  }

  private static AppEnvDeployRequested getAppEnvDeployRequested(
      String orgId,
      String envId,
      String appId,
      BuiltEnvInfo builtEnvInfo,
      String deploymentConfId,
      String builtProjectBucketKey,
      String appEnvDeploymentId) {
    return AppEnvDeployRequested.builder()
        .orgId(orgId)
        .builtEnvInfo(builtEnvInfo)
        .deploymentConfId(deploymentConfId)
        .requestInstant(now())
        .builtZipFormattedFilekey(builtProjectBucketKey)
        .envId(envId)
        .appId(appId)
        .currentIndependentStacksState(NOT_READY)
        .appEnvDeploymentId(appEnvDeploymentId)
        .build();
  }
}
