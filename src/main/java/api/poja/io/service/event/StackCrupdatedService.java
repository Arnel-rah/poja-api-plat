package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.enums.IndependentStacksStateEnum.PENDING;
import static api.poja.io.endpoint.event.model.enums.StackCrupdateStatus.CRUPDATE_FAILED;
import static api.poja.io.endpoint.event.model.enums.StackCrupdateStatus.CRUPDATE_IN_PROGRESS;
import static api.poja.io.endpoint.event.model.enums.StackCrupdateStatus.CRUPDATE_SUCCESS;
import static api.poja.io.endpoint.rest.model.StackResourceStatusType.CREATE_COMPLETE;
import static api.poja.io.endpoint.rest.model.StackResourceStatusType.UPDATE_COMPLETE;
import static api.poja.io.endpoint.rest.model.StackResourceStatusType.UPDATE_ROLLBACK_FAILED;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE;
import static api.poja.io.endpoint.rest.model.StackType.EVENT;
import static api.poja.io.service.stack.StackEventService.getOrgStackEventsBucketKey;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.AppEnvDeployRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackCrupdateCompleted;
import api.poja.io.endpoint.event.model.StackCrupdateRequested;
import api.poja.io.endpoint.event.model.StackCrupdateRequested.StackPair;
import api.poja.io.endpoint.event.model.StackCrupdated;
import api.poja.io.endpoint.event.model.enums.StackCrupdateStatus;
import api.poja.io.endpoint.rest.model.StackDeployment;
import api.poja.io.endpoint.rest.model.StackEvent;
import api.poja.io.endpoint.rest.model.StackResourceStatusType;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.EnvDeploymentConfService;
import api.poja.io.service.stack.StackEventService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class StackCrupdatedService implements Consumer<StackCrupdated> {
  private final EventProducer<PojaEvent> eventProducer;
  private final StackEventService stackEventService;
  private final EnvDeploymentConfService envDeplConfService;

  @Override
  public void accept(StackCrupdated stackCrupdated) {
    Stack stack = stackCrupdated.getStack();
    String orgId = stackCrupdated.getOrgId();
    String stackEventsBucketKey =
        getOrgStackEventsBucketKey(
            orgId, stack.getApplicationId(), stack.getEnvironmentId(), stack.getType());
    StackCrupdateStatus stackCrupdateStatus =
        getStackCrupdateStatus(stack.getName(), stack.getCfStackId(), stackEventsBucketKey);
    StackPair dependantStack = stackCrupdated.getDependantStack();
    switch (stackCrupdateStatus) {
      case CRUPDATE_IN_PROGRESS ->
          throw new EventConsumptionBackOffException("fail to trigger event backoff.");
      case CRUPDATE_SUCCESS -> {
        if (!COMPUTE.equals(stack.getType())) {
          AppEnvDeployRequested appEnvDeployRequested =
              stackCrupdated.getParentAppEnvDeployRequested();
          String appEnvDeplId = stackCrupdated.getAppEnvDeplId();
          if (EVENT.equals(stack.getType()) && dependantStack != null) {
            deployDependantStackAndFireResourceRetrievingEvents(
                stackCrupdated, appEnvDeplId, dependantStack, orgId, stack, appEnvDeployRequested);
            return;
          }
          var listOfEvents =
              getIndependentStackDeploymentSuccessEvents(
                  orgId, stack, appEnvDeplId, appEnvDeployRequested);
          eventProducer.accept(listOfEvents);
        } else {
          eventProducer.accept(
              getResourceRetrievingEvents(orgId, stack, stackCrupdated.getAppEnvDeplId()));
        }
      }
      case CRUPDATE_FAILED -> {
        log.info("crupdate failed, persisting compute stack resource to allow log inspection.");
        if (COMPUTE.equals(stack.getType())) {
          eventProducer.accept(
              getResourceRetrievingEvents(orgId, stack, stackCrupdated.getAppEnvDeplId()));
        }
      }
    }
  }

  private void deployDependantStackAndFireResourceRetrievingEvents(
      StackCrupdated stackCrupdated,
      String appEnvDeplId,
      StackPair dependantStack,
      String orgId,
      Stack stack,
      AppEnvDeployRequested appEnvDeployRequested) {
    var envDeplConf = envDeplConfService.getByAppEnvDeplId(appEnvDeplId);
    var deployDependantStackEvent =
        StackCrupdateRequested.builder()
            .independantStackToDeploy(
                new StackDeployment().stackType(dependantStack.last().getType()))
            .orgId(orgId)
            .applicationId(stack.getApplicationId())
            .environmentId(stack.getEnvironmentId())
            .appEnvDeplId(appEnvDeplId)
            .appEnvDeployRequested(appEnvDeployRequested)
            .envDeploymentConf(envDeplConf)
            .stackToCrupdate(stackCrupdated.getDependantStack())
            .dependantStack(null)
            .build();
    var listOfEvents =
        getEventStackDeploymentSuccessEventsWithDependantStack(
            orgId, stack, appEnvDeplId, deployDependantStackEvent);
    eventProducer.accept(listOfEvents);
  }

  private static List<PojaEvent> getIndependentStackDeploymentSuccessEvents(
      String orgId, Stack stack, String appEnvDeplId, AppEnvDeployRequested appEnvDeployRequested) {
    var resourceRetrievingEvents = getResourceRetrievingEvents(orgId, stack, appEnvDeplId);
    var listOfEvents = new ArrayList<PojaEvent>();
    listOfEvents.add(
        appEnvDeployRequested.toBuilder().currentIndependentStacksState(PENDING).build());
    listOfEvents.addAll(resourceRetrievingEvents);
    return listOfEvents;
  }

  private static List<PojaEvent> getEventStackDeploymentSuccessEventsWithDependantStack(
      String orgId,
      Stack stack,
      String appEnvDeplId,
      StackCrupdateRequested stackCrupdateRequested) {
    var resourceRetrievingEvents = getResourceRetrievingEvents(orgId, stack, appEnvDeplId);
    var listOfEvents = new ArrayList<>(resourceRetrievingEvents);
    listOfEvents.add(stackCrupdateRequested);
    return listOfEvents;
  }

  private StackCrupdateStatus getStackCrupdateStatus(
      String stackName, String stackId, String bucketKey) {
    List<StackEvent> stackEvents = stackEventService.crupdateStackEvents(stackId, bucketKey);
    StackEvent latestEvent = stackEvents.getFirst();
    StackResourceStatusType status = latestEvent.getResourceStatus();
    if (status.equals(UPDATE_ROLLBACK_FAILED)) {
      return CRUPDATE_FAILED;
    }
    if (status != null
        && status.toString().contains("COMPLETE")
        && Objects.equals(latestEvent.getLogicalResourceId(), stackName)) {
      return (status.equals(CREATE_COMPLETE) || status.equals(UPDATE_COMPLETE))
          ? CRUPDATE_SUCCESS
          : CRUPDATE_FAILED;
    }
    return CRUPDATE_IN_PROGRESS;
  }

  public static List<StackEvent> mergeAndSortStackEventList(
      List<StackEvent> actual, List<StackEvent> newEvents) {
    Set<StackEvent> mergedSet = new HashSet<>(actual);
    mergedSet.addAll(newEvents);
    return mergedSet.stream()
        .sorted(
            (e1, e2) -> {
              Instant i1 = e1.getTimestamp();
              Instant i2 = e2.getTimestamp();
              if (i1 == null && i2 == null) return 0;
              if (i1 == null) return 1;
              if (i2 == null) return -1;
              return i2.compareTo(i1);
            })
        .toList();
  }

  private static List<PojaEvent> getResourceRetrievingEvents(
      String orgId, Stack stack, String appEnvDeplId) {
    return switch (stack.getType()) {
      case COMPUTE, STORAGE_BUCKET, EVENT ->
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
    };
  }
}
