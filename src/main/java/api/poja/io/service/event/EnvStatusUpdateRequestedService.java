package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.LambdaFunctionStatusUpdateRequested.StatusAlteration.ACTIVATE;
import static api.poja.io.endpoint.event.model.LambdaFunctionStatusUpdateRequested.StatusAlteration.SUSPEND;
import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.ACTIVE;
import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.SUSPENDED;

import api.poja.io.aws.lambda.LambdaComponent;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.EnvStatusUpdateRequested;
import api.poja.io.endpoint.event.model.LambdaFunctionStatusUpdateRequested;
import api.poja.io.repository.model.ComputeStackResource;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.ComputeStackResourceService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.sys.platform.SaasOnly;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@SaasOnly
public class EnvStatusUpdateRequestedService implements Consumer<EnvStatusUpdateRequested> {
  private final EnvironmentService environmentService;
  private final EventProducer<LambdaFunctionStatusUpdateRequested> eventProducer;
  private final ComputeStackResourceService computeStackResourceService;
  private final LambdaComponent lambdaComponent;

  @Override
  public void accept(EnvStatusUpdateRequested envStatusUpdateRequested) {
    Environment environment = environmentService.getById(envStatusUpdateRequested.getEnvId());
    var envId = environment.getId();

    switch (envStatusUpdateRequested.getStatus()) {
      case SUSPEND -> {
        if (SUSPENDED.equals(environment.getStatus())) {
          return;
        }
        environmentService.updateEnvStatus(envId, SUSPENDED);
        fireLambdaStatusUpdateEvent(envId, SUSPEND);
      }
      case ACTIVATE -> {
        if (ACTIVE.equals(environment.getStatus())) {
          return;
        }
        environmentService.updateEnvStatus(envId, ACTIVE);
        fireLambdaStatusUpdateEvent(envId, ACTIVATE);
      }
    }
  }

  private void fireLambdaStatusUpdateEvent(
      String envId, LambdaFunctionStatusUpdateRequested.StatusAlteration statusAlteration) {
    var optionalLatestComputeResource =
        computeStackResourceService.findLatestByEnvironmentId(envId);

    if (optionalLatestComputeResource.isPresent()) {
      var latestComputeResource =
          setFunctionsReservedConcurrency(optionalLatestComputeResource.get());
      Map<String, Integer> functions =
          getFunctionNamesAndReservedConcurrency(latestComputeResource);
      eventProducer.accept(
          functions.entrySet().stream()
              .map(f -> toLambdaStatusUpdateEvent(f.getKey(), f.getValue(), statusAlteration))
              .toList());
    }
  }

  private static Map<String, Integer> getFunctionNamesAndReservedConcurrency(
      ComputeStackResource computeStackResource) {
    Map<String, Integer> functions = new HashMap<>();
    if (computeStackResource.getFrontalFunctionName() != null) {
      functions.put(
          computeStackResource.getFrontalFunctionName(),
          computeStackResource.getFrontalFunctionReservedConcurrency());
    }
    for (var workerFunction : computeStackResource.getWorkerFunctions()) {
      functions.put(
          workerFunction.getName(), workerFunction.getWorkerFunctionReservedConcurrency());
    }
    return functions;
  }

  private static LambdaFunctionStatusUpdateRequested toLambdaStatusUpdateEvent(
      String functionName,
      Integer functionReservedConcurrency,
      LambdaFunctionStatusUpdateRequested.StatusAlteration statusAlteration) {
    return LambdaFunctionStatusUpdateRequested.builder()
        .functionName(functionName)
        .functionReservedConcurrency(functionReservedConcurrency)
        .status(statusAlteration)
        .build();
  }

  private ComputeStackResource setFunctionsReservedConcurrency(ComputeStackResource resource) {
    if (!resource.isFrontalFunctionDeleted()
        && resource.getFrontalFunctionReservedConcurrency() == null) {
      resource.setFrontalFunctionReservedConcurrency(
          lambdaComponent.getFunctionReservedConcurrency(resource.getFrontalFunctionName()));
    }

    if (resource.getWorkerFunctions() != null && !resource.getWorkerFunctions().isEmpty()) {
      resource.getWorkerFunctions().stream()
          .filter(f -> !f.isDeleted())
          .filter(f -> f.getWorkerFunctionReservedConcurrency() == null)
          .forEach(
              f ->
                  f.setWorkerFunctionReservedConcurrency(
                      lambdaComponent.getFunctionReservedConcurrency(f.getName())));
    }

    return computeStackResourceService.save(resource);
  }
}
