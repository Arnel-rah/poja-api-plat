package api.poja.io.service.stack;

import static api.poja.io.endpoint.rest.model.StackType.COMPUTE_PERMISSION;
import static api.poja.io.endpoint.rest.model.StackType.EVENT;
import static api.poja.io.endpoint.rest.model.StackType.EVENT_SCHEDULER;
import static api.poja.io.endpoint.rest.model.StackType.STORAGE_BUCKET;
import static api.poja.io.model.CancelMethod.CANCEL_UPDATE_STACK;
import static api.poja.io.model.CancelMethod.DELETE_STACK;
import static api.poja.io.model.CancelResult.NEEDS_BACKOFF;
import static java.time.Instant.now;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.DELETE_COMPLETE;

import api.poja.io.aws.cloudformation.CloudformationComponent;
import api.poja.io.aws.cloudformation.CloudformationTemplateConf;
import api.poja.io.aws.lambda.LambdaComponent;
import api.poja.io.endpoint.event.model.AppEnvDeployRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackCrupdateCompleted;
import api.poja.io.endpoint.event.model.StackCrupdateRequested;
import api.poja.io.endpoint.event.model.StackCrupdated;
import api.poja.io.endpoint.rest.model.StackDeployment;
import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.model.CancelResult;
import api.poja.io.model.StackStatus;
import api.poja.io.model.UpdateStackResult;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.model.ComputeStackResource;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Stack;
import api.poja.io.repository.model.WorkerFunction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Service
@AllArgsConstructor
@Slf4j
public class StackCloudService {
  private final StackService stackService;
  private final CloudformationComponent cloudformationComponent;
  private final CloudformationTemplateConf cloudformationTemplateConf;
  private final LambdaComponent lambdaComponent;

  public List<PojaEvent> updateStackOrCreateIfNotExistsOnCloud(
      StackDeployment independantStackToDeploy,
      String orgId,
      String applicationId,
      String environmentId,
      AppEnvDeployRequested appEnvDeployRequested,
      Map<String, String> parameters,
      EnvDeploymentConf envDeploymentConf,
      Map<String, String> tags,
      StackCrupdateRequested.StackPair stackPair,
      Optional<StackCrupdateRequested.StackPair> dependantStack) {
    var previousStack = stackPair.first();
    var stackToUpdate = stackPair.last();
    if (previousStack.getCfStackId() == null) {
      return createStack(
          independantStackToDeploy,
          orgId,
          applicationId,
          environmentId,
          appEnvDeployRequested,
          parameters,
          envDeploymentConf,
          tags,
          stackToUpdate,
          dependantStack);
    }
    var updateStackResult =
        updateStack(
            orgId,
            applicationId,
            environmentId,
            independantStackToDeploy,
            parameters,
            envDeploymentConf,
            previousStack.getName(),
            tags);
    if (updateStackResult.isUpdated()) {
      Stack saved =
          stackService.save(
              stackToUpdate.toBuilder().cfStackId(updateStackResult.stackId()).build());
      return getStackCrupdatedEvents(orgId, saved, appEnvDeployRequested, dependantStack);
    }
    if (updateStackResult.isSuccess()) {
      var saved =
          stackService.save(
              stackToUpdate.toBuilder().cfStackId(previousStack.getCfStackId()).build());
      var events = new ArrayList<PojaEvent>();
      var stackCrupdatedEvents =
          getStackCrupdatedEvents(orgId, saved, appEnvDeployRequested, dependantStack);
      var resourceRetrievingEvents =
          getResourceRetrievingEvents(orgId, saved, appEnvDeployRequested.getAppEnvDeploymentId());
      events.addAll(stackCrupdatedEvents);
      events.addAll(resourceRetrievingEvents);
      return events;
    }
    return List.of();
  }

  public List<PojaEvent> createStack(
      StackDeployment independantStackToDeploy,
      String orgId,
      String applicationId,
      String environmentId,
      AppEnvDeployRequested appEnvDeployRequested,
      Map<String, String> parameters,
      EnvDeploymentConf envDeploymentConf,
      Map<String, String> tags,
      Stack concernedStack,
      Optional<StackCrupdateRequested.StackPair> dependantStack) {
    String stackName = concernedStack.getName();
    String cfStackId =
        createStack(
            orgId,
            applicationId,
            environmentId,
            independantStackToDeploy,
            parameters,
            envDeploymentConf,
            stackName,
            tags);
    Stack saved = stackService.save(concernedStack.toBuilder().cfStackId(cfStackId).build());
    return getStackCrupdatedEvents(orgId, saved, appEnvDeployRequested, dependantStack);
  }

  public Optional<String> getCloudformationStackId(String stackName) {
    return cloudformationComponent.findStackIdByName(stackName);
  }

  public void initiateStackDelete(Stack stack) {
    cloudformationComponent.deleteStack(stack.getName());
  }

  private StackStatus getStackStatus(String stackId) {
    Optional<StackStatus> stackStatus = cloudformationComponent.getStackStatus(stackId);
    return stackStatus.orElseThrow(() -> new NotFoundException("stack not found : " + stackId));
  }

  public CancelResult cancelStackDepl(String envId) {
    List<Stack> envStacks = stackService.findAllByEnvId(envId);
    for (Stack envStack : envStacks) {
      String stackId = envStack.getCfStackId();
      StackStatus stackStatus = getStackStatus(stackId);
      boolean cancellable = stackStatus.isCancellable();
      if (cancellable) {
        cancelStackDeployment(stackStatus, stackId);
      }
    }
    return NEEDS_BACKOFF;
  }

  private void cancelStackDeployment(StackStatus stackStatus, String stackId) {
    if (CANCEL_UPDATE_STACK.equals(stackStatus.cancelMethod())) {
      cloudformationComponent.cancelExistingStackUpdate(stackId);
    } else if (DELETE_STACK.equals(stackStatus.cancelMethod())) {
      cloudformationComponent.deleteStack(stackId);
    }
    throw new RuntimeException("Stack deployment cannot be cancelled");
  }

  private static List<PojaEvent> getStackCrupdatedEvents(
      String orgId,
      Stack saved,
      AppEnvDeployRequested appEnvDeployRequested,
      Optional<StackCrupdateRequested.StackPair> dependantStack) {
    if (dependantStack.isPresent()) {
      return List.of(
          StackCrupdated.builder()
              .orgId(orgId)
              .stack(saved)
              .dependantStack(dependantStack.get())
              .parentAppEnvDeployRequested(appEnvDeployRequested)
              .appEnvDeplId(appEnvDeployRequested.getAppEnvDeploymentId())
              .build());
    }
    return List.of(
        StackCrupdated.builder()
            .orgId(orgId)
            .stack(saved)
            .parentAppEnvDeployRequested(appEnvDeployRequested)
            .appEnvDeplId(appEnvDeployRequested.getAppEnvDeploymentId())
            .build());
  }

  private static List<PojaEvent> getResourceRetrievingEvents(
      String orgId, Stack stack, String appEnvDeplId) {
    return switch (stack.getType()) {
      case STORAGE_BUCKET, EVENT ->
          List.of(
              StackCrupdateCompleted.builder()
                  .appEnvDeplId(appEnvDeplId)
                  .orgId(orgId)
                  .completionTimestamp(now())
                  .crupdatedStack(stack)
                  .build());
      case COMPUTE_PERMISSION, EVENT_SCHEDULER -> {
        log.info("Get resources for stack type={} not implemented", stack.getType());
        yield List.of();
      }
      case COMPUTE ->
          throw new RuntimeException("Compute stack update is not done by StackService");
    };
  }

  private String createStack(
      String orgId,
      String appId,
      String envId,
      StackDeployment toDeploy,
      Map<String, String> parameters,
      EnvDeploymentConf envDeploymentConf,
      String stackName,
      Map<String, String> tags) {
    return cloudformationComponent.createStack(
        stackName,
        getStackTemplateUrlFrom(orgId, appId, envId, toDeploy.getStackType(), envDeploymentConf),
        parameters,
        tags);
  }

  private UpdateStackResult updateStack(
      String orgId,
      String appId,
      String envId,
      StackDeployment toDeploy,
      Map<String, String> parameters,
      EnvDeploymentConf envDeploymentConf,
      String stackName,
      Map<String, String> tags) {
    return cloudformationComponent.updateStack(
        stackName,
        getStackTemplateUrlFrom(orgId, appId, envId, toDeploy.getStackType(), envDeploymentConf),
        parameters,
        tags);
  }

  private String getStackTemplateUrlFrom(
      String orgId,
      String appId,
      String envId,
      StackType type,
      EnvDeploymentConf envDeploymentConf) {
    Map<StackType, Supplier<String>> stackFileKeyMap =
        Map.of(
            EVENT,
            envDeploymentConf::getEventStackFileKey,
            STORAGE_BUCKET,
            envDeploymentConf::getStorageBucketStackFileKey,
            COMPUTE_PERMISSION,
            envDeploymentConf::getComputePermissionStackFileKey,
            EVENT_SCHEDULER,
            envDeploymentConf::getEventSchedulerStackFileKey);
    String filename = stackFileKeyMap.getOrDefault(type, () -> null).get();
    return cloudformationTemplateConf
        .getCloudformationTemplateUrl(orgId, appId, envId, filename)
        .toString();
  }

  public List<StackResource> getStackResources(String stackName) {
    // check if stack still exists because we do not want to get stack by cf stack id
    // we do not want that because next operation will add resources, if stack is already deleted,
    // we do not want to operate on already delete resources
    var stack = cloudformationComponent.findStackByName(stackName);
    if (stack.isEmpty()) {
      return List.of();
    }
    return cloudformationComponent.getStackResources(stackName);
  }

  public static Optional<StackResource> filterStackResourceByLogicalId(
      List<StackResource> stackResources, String targetLogicalResourceId) {
    return stackResources.stream()
        .filter(stackResource -> stackResource.logicalResourceId().equals(targetLogicalResourceId))
        .findFirst();
  }

  public static String getPhysicalResourceId(
      List<StackResource> stackResources, String targetLogicalResourceId) {
    return stackResources.stream()
        .filter(stackResource -> stackResource.logicalResourceId().equals(targetLogicalResourceId))
        .map(StackResource::physicalResourceId)
        .findFirst()
        .orElse(null);
  }

  static final String FRONTAL_FUNCTION_LOGICAL_RESOURCE_ID = "FrontalFunction";

  public ComputeStackResource getComputeStackResource(
      String appEnvDeplId, Instant createdAt, Stack stack, List<StackResource> stackResources) {

    if (stackResources.isEmpty()) {
      throw new IllegalArgumentException("stack resources cannot be empty");
    }
    var frontalOpt =
        filterStackResourceByLogicalId(stackResources, FRONTAL_FUNCTION_LOGICAL_RESOURCE_ID);
    List<WorkerFunction> workerFunctions = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      var workerOpt = filterStackResourceByLogicalId(stackResources, "WorkerFunction" + i);
      workerOpt.ifPresent(
          w -> {
            var isFunctionDeleted = DELETE_COMPLETE.equals(w.resourceStatus());
            workerFunctions.add(
                WorkerFunction.builder()
                    .name(w.physicalResourceId())
                    .deleted(isFunctionDeleted)
                    .workerFunctionReservedConcurrency(getFunctionReservedConcurrency(workerOpt))
                    .build());
          });
    }

    return ComputeStackResource.builder()
        .frontalFunctionName(frontalOpt.map(StackResource::physicalResourceId).orElse(null))
        .frontalFunctionDeleted(
            frontalOpt.map(e -> DELETE_COMPLETE.equals(e.resourceStatus())).orElse(false))
        .frontalFunctionReservedConcurrency(getFunctionReservedConcurrency(frontalOpt))
        .workerFunctions(workerFunctions)
        .stackId(stack.getId())
        .environmentId(stack.getEnvironmentId())
        .appEnvDeplId(appEnvDeplId)
        .creationDatetime(createdAt)
        .build();
  }

  private Integer getFunctionReservedConcurrency(Optional<StackResource> functionOpt) {
    if (functionOpt.isEmpty()) {
      return null;
    }

    var function = functionOpt.get();
    var isFunctionDeleted = DELETE_COMPLETE.equals(function.resourceStatus());

    log.info("Getting reserved concurrency for function={}", function.physicalResourceId());
    return isFunctionDeleted
        ? null
        : lambdaComponent.getFunctionReservedConcurrency(function.physicalResourceId());
  }
}
