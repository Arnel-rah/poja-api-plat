package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.mapper.ComputeStackResourceMapper.distinctByKey;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE;
import static api.poja.io.model.CancelResult.NEEDS_BACKOFF;
import static java.util.stream.Collectors.groupingBy;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.consumer.model.exception.EventConsumptionBackOffException;
import api.poja.io.endpoint.event.model.EnvArchivalRequested;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.StackCloudPermissionRemovalRequested;
import api.poja.io.endpoint.event.model.StackDeletionRequested;
import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.Stack;
import api.poja.io.service.AppEnvDeployCancelService;
import api.poja.io.service.AppEnvironmentDeploymentService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.stack.StackService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EnvArchivalRequestedService implements Consumer<EnvArchivalRequested> {
  private EventProducer<PojaEvent> eventProducer;
  @Deprecated private final ApplicationService applicationService;
  private final StackService stackService;
  private final AppEnvDeployCancelService cancelService;
  private final AppEnvironmentDeploymentService appEnvDeploymentService;

  @Override
  public void accept(EnvArchivalRequested envArchivalRequested) {
    var orgId = applicationService.getById(envArchivalRequested.getAppId()).getOrgId();
    var envId = envArchivalRequested.getEnvId();
    var pendingDepls =
        appEnvDeploymentService.findAllPendingDeployments(envArchivalRequested.getAppId(), envId);

    cancelDepls(envId, pendingDepls);

    List<Stack> stacksToArchive =
        stackService.findAllByEnvId(envId).stream().filter(s -> !s.isArchived()).toList();
    stackService.archiveStacks(
        stacksToArchive.stream().filter(s -> s.getCfStackId() == null).toList());
    List<PojaEvent> events =
        getEvents(orgId, stacksToArchive.stream().filter(s -> s.getCfStackId() != null).toList());

    eventProducer.accept(events);
  }

  private static List<PojaEvent> getEvents(String orgId, List<Stack> stacks) {
    var res = new ArrayList<>(getStackDeletionEvents(stacks));
    Map<StackType, List<Stack>> stackMap = stacks.stream().collect(groupingBy(Stack::getType));
    var computeStacks = stackMap.getOrDefault(COMPUTE, List.of());
    if (computeStacks.isEmpty()) {
      return res;
    }
    // will retrieve compute resources for each compute stack and remove associated cloud permission
    // No issue with duplicates, as compute resources will be deduplicated later.
    res.add(
        StackCloudPermissionRemovalRequested.builder()
            .orgId(orgId)
            .computeStacks(computeStacks)
            .build());
    return res;
  }

  private static List<PojaEvent> getStackDeletionEvents(List<Stack> stacks) {
    // Stacks are saved multiple times to track usage across different deployments.
    // Delete each unique stack, identified by its cfStackId.
    return stacks.stream()
        .filter(distinctByKey(Stack::getCfStackId))
        .map(EnvArchivalRequestedService::toStackDeletionRequestedEvent)
        .toList();
  }

  private static PojaEvent toStackDeletionRequestedEvent(Stack stack) {
    return new StackDeletionRequested(stack);
  }

  private void cancelDepls(String envId, List<AppEnvironmentDeployment> deplsToCancel) {
    if (deplsToCancel.isEmpty()) {
      return;
    }
    log.info("cancel {} pending deployments", deplsToCancel.size());
    var cancelResult = cancelService.apply(envId, deplsToCancel);
    if (NEEDS_BACKOFF.equals(cancelResult)) {
      throw new EventConsumptionBackOffException("backOff waiting for all depls to be canceled");
    }
  }
}
