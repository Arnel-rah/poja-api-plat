package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CANCELING;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_FAILED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_INITIATED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE_PERMISSION;
import static api.poja.io.endpoint.rest.model.StackType.EVENT;
import static api.poja.io.endpoint.rest.model.StackType.EVENT_SCHEDULER;
import static api.poja.io.endpoint.rest.model.StackType.STORAGE_BUCKET;
import static api.poja.io.repository.model.Environment.formatEnvironmentType;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppEnvComputeDeployRequested;
import api.poja.io.endpoint.event.model.AppEnvDeployRequested;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.endpoint.rest.model.StackDeployment;
import api.poja.io.endpoint.rest.model.StackEvent;
import api.poja.io.endpoint.rest.model.StackResourceStatusType;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvDeploymentConfService;
import api.poja.io.service.stack.StackCloudCrupdateService;
import api.poja.io.service.stack.StackEventService;
import api.poja.io.service.stack.StackService;
import api.poja.io.service.workflows.DeploymentStateService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AppEnvDeployRequestedService implements Consumer<AppEnvDeployRequested> {
  public static final String EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES =
      "appEnvDepl {} expected state(s) {} for this step but was {}, action skipped";
  private final EventProducer<AppEnvComputeDeployRequested>
      appEnvComputeDeployRequestedEventProducer;
  private final EnvDeploymentConfService envDeploymentConfService;
  private final StackService stackService;
  private final StackEventService stackEventService;
  private final StackCloudCrupdateService stackCloudCrupdateService;
  private final ApplicationService appService;
  private final DeploymentStateService deploymentStateService;

  @Override
  public void accept(AppEnvDeployRequested appEnvDeployRequested) {
    String appEnvDeploymentId = appEnvDeployRequested.getAppEnvDeploymentId();
    var latestDeploymentState =
        deploymentStateService
            .getLatestDeploymentStateByDeploymentId(appEnvDeploymentId)
            .getProgressionStatus();
    if (CANCELED.equals(latestDeploymentState)) {
      log.info("deployment {} has been canceled", appEnvDeploymentId);
      return;
    }
    String orgId = appEnvDeployRequested.getOrgId();
    String appId = appEnvDeployRequested.getAppId();
    String envId = appEnvDeployRequested.getEnvId();
    switch (appEnvDeployRequested.getCurrentIndependentStacksState()) {
      case NOT_READY ->
          deployIndependantStacks(
              appEnvDeployRequested,
              latestDeploymentState,
              appEnvDeploymentId,
              envId,
              orgId,
              appId);
      case PENDING -> {
        if (shouldDeployComputeStack(
            latestDeploymentState, appEnvDeploymentId, orgId, appId, envId)) {
          deployComputeStack(
              appEnvDeployRequested.getBuiltEnvInfo(),
              appEnvDeployRequested.getBuiltZipFormattedFilekey(),
              appEnvDeploymentId,
              appId,
              orgId,
              envId);
        }
      }
      case READY -> {
        if (CANCELING.equals(latestDeploymentState)) {
          log.info(
              "deployment has reached CANCELING state, compute stack won't be deployed for this"
                  + " deployment {}",
              appEnvDeploymentId);
          deploymentStateService.save(appId, appEnvDeploymentId, CANCELED);
          return;
        }
        log.info("deprecated");
        deployComputeStack(
            appEnvDeployRequested.getBuiltEnvInfo(),
            appEnvDeployRequested.getBuiltZipFormattedFilekey(),
            appEnvDeploymentId,
            appId,
            orgId,
            envId);
      }
    }
  }

  private void deployComputeStack(
      BuiltEnvInfo builtEnvInfo,
      String builtZipFormattedFileKey,
      String appEnvDeploymentId,
      String appId,
      String orgId,
      String envId) {
    DeploymentStateEnum latestDeploymentState =
        deploymentStateService
            .getLatestDeploymentStateByDeploymentId(appEnvDeploymentId)
            .getProgressionStatus();
    if (!INDEPENDENT_STACKS_DEPLOYED.equals(latestDeploymentState)) {
      log.info(
          EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES,
          appEnvDeploymentId,
          INDEPENDENT_STACKS_DEPLOYED,
          latestDeploymentState);
    }
    deploymentStateService.save(appId, appEnvDeploymentId, COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS);
    var app = appService.getById(appId);
    var envType = builtEnvInfo.getEnvironmentType();
    var envTypeString = formatEnvironmentType(envType);

    var computeStackName = stackCloudCrupdateService.getStackName(COMPUTE, envTypeString, app);
    var independentStackNames =
        stackCloudCrupdateService.getIndependentStacksNames(envTypeString, app);

    log.info("Trigger compute stack deployment");
    appEnvComputeDeployRequestedEventProducer.accept(
        List.of(
            AppEnvComputeDeployRequested.builder()
                .orgId(orgId)
                .appId(appId)
                .envId(envId)
                .appName(app.getName())
                .stackName(computeStackName)
                .independentStackNames(independentStackNames)
                .formattedBucketKey(builtZipFormattedFileKey)
                .requestInstant(Instant.now())
                .environmentType(envType)
                .appEnvDeploymentId(appEnvDeploymentId)
                .build()));
  }

  private boolean shouldDeployComputeStack(
      DeploymentStateEnum latestDeploymentState,
      String appEnvDeploymentId,
      String orgId,
      String appId,
      String envId) {
    IndependentStacksDeploymentStateEnum independentStacksDeploymentState =
        checkStacksDeploymentState(orgId, appId, envId, appEnvDeploymentId);
    return switch (independentStacksDeploymentState) {
      case PENDING -> {
        log.info("Waiting for independent stacks to be deployed");
        yield false;
      }
      case DEPLOYED -> {
        log.info("Compute stack ready to be deployed");
        if (INDEPENDENT_STACKS_DEPLOYED.equals(latestDeploymentState)) {
          log.info("independent stacks were already deployed");
          yield false;
        }
        if (CANCELING.equals(latestDeploymentState)) {
          log.info(
              "deployment has reached CANCELING state, progression is stopped at {} for this"
                  + " deployment {}",
              INDEPENDENT_STACKS_DEPLOYED,
              appEnvDeploymentId);
          deploymentStateService.save(appId, appEnvDeploymentId, INDEPENDENT_STACKS_DEPLOYED);
          deploymentStateService.save(appId, appEnvDeploymentId, CANCELED);
          yield false;
        }
        if (!INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS.equals(latestDeploymentState)) {
          log.info(
              EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES,
              appEnvDeploymentId,
              List.of(INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS, INDEPENDENT_STACKS_DEPLOYED),
              latestDeploymentState);
          yield false;
        }
        deploymentStateService.save(appId, appEnvDeploymentId, INDEPENDENT_STACKS_DEPLOYED);
        yield true;
      }
      case NOT_DEPLOYED -> {
        log.info("Independent stacks deployment failed");
        if (INDEPENDENT_STACKS_DEPLOYED.equals(latestDeploymentState)) {
          log.info("independent stacks were already deployed");
          yield false;
        }
        if (CANCELING.equals(latestDeploymentState)) {
          log.info(
              "deployment has reached CANCELING state, progression is stopped at {} for this"
                  + " deployment {}",
              INDEPENDENT_STACKS_DEPLOYMENT_FAILED,
              appEnvDeploymentId);
          deploymentStateService.save(
              appId, appEnvDeploymentId, INDEPENDENT_STACKS_DEPLOYMENT_FAILED);
          deploymentStateService.save(appId, appEnvDeploymentId, CANCELED);
          yield false;
        }
        if (!INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS.equals(latestDeploymentState)) {
          log.info(
              EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES,
              appEnvDeploymentId,
              List.of(INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS, INDEPENDENT_STACKS_DEPLOYED),
              latestDeploymentState);
          yield false;
        }
        deploymentStateService.save(
            appId, appEnvDeploymentId, INDEPENDENT_STACKS_DEPLOYMENT_FAILED);
        yield false;
      }
    };
  }

  private void deployIndependantStacks(
      AppEnvDeployRequested appEnvDeployRequested,
      DeploymentStateEnum latestDeploymentState,
      String appEnvDeploymentId,
      String envId,
      String orgId,
      String appId) {
    if (CANCELING.equals(latestDeploymentState)) {
      log.info(
          "deployment has reached CANCELING state, independant stacks won't be deployed for this"
              + " deployment {}",
          appEnvDeploymentId);
      deploymentStateService.save(appId, appEnvDeploymentId, CANCELED);
      return;
    }
    if (!INDEPENDENT_STACKS_DEPLOYMENT_INITIATED.equals(latestDeploymentState)) {
      log.info(
          EARLY_RETURN_LOG_MESSAGE_FOR_NOT_MATCHING_DEPLOYMENT_STATES,
          appEnvDeploymentId,
          INDEPENDENT_STACKS_DEPLOYMENT_INITIATED,
          latestDeploymentState);
      return;
    }
    List<StackDeployment> independantStacksToDeploy =
        retrieveIndependantStacksToDeploy(appEnvDeploymentId);
    log.info("Cloudformation independant stacks to deploy: {}", independantStacksToDeploy);
    stackCloudCrupdateService.processIndependantStacksDeployment(
        independantStacksToDeploy, orgId, appId, envId, appEnvDeployRequested);
    deploymentStateService.save(
        appId, appEnvDeploymentId, INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS);
  }

  private List<StackDeployment> retrieveIndependantStacksToDeploy(String appEnvDeplId) {
    EnvDeploymentConf envDeploymentConf = envDeploymentConfService.getByAppEnvDeplId(appEnvDeplId);
    List<StackDeployment> stacksToDeploy = new ArrayList<>();
    stacksToDeploy.add(new StackDeployment().stackType(COMPUTE_PERMISSION));
    if (envDeploymentConf.getEventStackFileKey() != null) {
      if (envDeploymentConf.getEventSchedulerStackFileKey() == null) {
        stacksToDeploy.add(new StackDeployment().stackType(EVENT));
      } else {
        stacksToDeploy.add(
            new StackDeployment().stackType(EVENT).dependantStackType(EVENT_SCHEDULER));
      }
    }
    if (envDeploymentConf.getStorageBucketStackFileKey() != null) {
      stacksToDeploy.add(new StackDeployment().stackType(STORAGE_BUCKET));
    }
    return stacksToDeploy;
  }

  private IndependentStacksDeploymentStateEnum checkStacksDeploymentState(
      String orgId, String appId, String envId, String appEnvDeplId) {
    List<Stack> environmentStacks =
        stackService
            .findAllBy(
                orgId, appId, envId, appEnvDeplId, new PageFromOne(1), new BoundedPageSize(5))
            .data()
            .stream()
            .filter(stack -> !Objects.equals(stack.getType(), COMPUTE))
            .toList();
    List<IndependentStacksDeploymentStateEnum> stackDeploymentStates =
        environmentStacks.stream()
            .map(stack -> this.getCrupdateStackDeplState(orgId, appId, envId, stack))
            .toList();
    if (stackDeploymentStates.stream()
        .allMatch(IndependentStacksDeploymentStateEnum.DEPLOYED::equals))
      return IndependentStacksDeploymentStateEnum.DEPLOYED;
    if (stackDeploymentStates.stream()
        .noneMatch(IndependentStacksDeploymentStateEnum.NOT_DEPLOYED::equals))
      return IndependentStacksDeploymentStateEnum.PENDING;
    return IndependentStacksDeploymentStateEnum.NOT_DEPLOYED;
  }

  private IndependentStacksDeploymentStateEnum getCrupdateStackDeplState(
      String orgId, String appId, String envId, Stack stack) {
    List<StackEvent> stackEventsOrderedByTime =
        stackEventService
            .getStackEvents(
                orgId, appId, envId, stack.getType(), new PageFromOne(1), new BoundedPageSize(5))
            .data()
            .stream()
            .toList();
    return getCrupdateStackDeplState(stack, stackEventsOrderedByTime);
  }

  private static IndependentStacksDeploymentStateEnum getCrupdateStackDeplState(
      Stack stack, List<StackEvent> stackEventsOrderedByTime) {
    if (stackEventsOrderedByTime.isEmpty()) {
      return IndependentStacksDeploymentStateEnum.PENDING;
    }

    StackEvent latestEvent = stackEventsOrderedByTime.getFirst();
    if (!stack.getName().equals(latestEvent.getLogicalResourceId())) {
      return IndependentStacksDeploymentStateEnum.PENDING;
    }

    StackResourceStatusType latestResourceStatus = latestEvent.getResourceStatus();
    return switch (latestResourceStatus) {
      case CREATE_COMPLETE, UPDATE_COMPLETE -> IndependentStacksDeploymentStateEnum.DEPLOYED;
      case CREATE_FAILED,
              UPDATE_FAILED,
              // Assuming rollbacks only occur on error
              // Then any rollback status means not deployed
              ROLLBACK_IN_PROGRESS,
              ROLLBACK_COMPLETE,
              ROLLBACK_FAILED,
              UPDATE_ROLLBACK_IN_PROGRESS,
              UPDATE_ROLLBACK_COMPLETE,
              UPDATE_ROLLBACK_FAILED ->
          IndependentStacksDeploymentStateEnum.NOT_DEPLOYED;
      case UNKNOWN_TO_SDK_VERSION -> noUnknownToSdkState(latestEvent);
      case IMPORT_IN_PROGRESS,
              IMPORT_COMPLETE,
              IMPORT_FAILED,
              IMPORT_ROLLBACK_IN_PROGRESS,
              IMPORT_ROLLBACK_COMPLETE,
              IMPORT_ROLLBACK_FAILED,
              DELETE_IN_PROGRESS,
              DELETE_FAILED,
              DELETE_COMPLETE,
              DELETE_SKIPPED ->
          noImportNorDeleteState(latestEvent);
      case CREATE_IN_PROGRESS, UPDATE_IN_PROGRESS -> IndependentStacksDeploymentStateEnum.PENDING;
    };
  }

  private static IndependentStacksDeploymentStateEnum noUnknownToSdkState(StackEvent stackEvent) {
    log.error("UNKNOWN_TO_SDK_VERSION state is not expected, yet got " + stackEvent);
    // We assume we already gave distinct mapping to each and every AWS final states.
    // Hence the states that are indistinctly mapped to UNKNOWN_TO_SDK_VERSION are non-final.
    // They will eventually resolve into a known (final) state.
    return IndependentStacksDeploymentStateEnum.PENDING;
  }

  private static IndependentStacksDeploymentStateEnum noImportNorDeleteState(
      StackEvent stackEvent) {
    log.error("Stack event cannot be of import nor delete type, yet got " + stackEvent);
    return IndependentStacksDeploymentStateEnum.NOT_DEPLOYED;
  }

  private enum IndependentStacksDeploymentStateEnum {
    PENDING,
    DEPLOYED,
    NOT_DEPLOYED
  }
}
