package api.poja.io.endpoint.rest.mapper;

import static java.lang.Boolean.TRUE;
import static java.util.Map.Entry.comparingByKey;

import api.poja.io.endpoint.rest.model.FunctionMonitoringResource;
import api.poja.io.endpoint.rest.model.GroupedMonitoringResources;
import api.poja.io.endpoint.rest.model.WorkerMonitoringResources;
import api.poja.io.repository.model.ComputeStackResource;
import api.poja.io.repository.model.WorkerFunction;
import api.poja.io.service.ComputeStackResourceService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ComputeStackResourceMapper {
  private final ComputeStackResourceService computeStackResourceService;

  public GroupedMonitoringResources toRest(List<ComputeStackResource> domain) {
    List<FunctionMonitoringResource> frontalFunctionLogs = new ArrayList<>();
    Map<Integer, List<FunctionMonitoringResource>> workerFunctionLogs = new HashMap<>();

    for (int i = 0; i < domain.size(); i++) {
      var stackResource = domain.get(i);
      var isLatest = i == 0;
      if (stackResource.getFrontalFunctionName() != null) {
        frontalFunctionLogs.add(frontalFunctionFrom(stackResource, isLatest));
      }
      for (int j = 0; j < stackResource.getWorkerFunctions().size(); j++) {
        var worker = stackResource.getWorkerFunctions().get(j);
        workerFunctionLogs
            .computeIfAbsent(j, k -> new ArrayList<>())
            .add(workerFunctionFrom(worker, stackResource.getCreationDatetime(), isLatest));
      }
    }

    return new GroupedMonitoringResources()
        .frontalFunctionMonitoringResources(distinctList(frontalFunctionLogs))
        .workerFunctionMonitoringResources(
            workerFunctionLogs.entrySet().stream()
                .sorted(comparingByKey())
                .map(
                    e ->
                        new WorkerMonitoringResources()
                            .workerIndex(e.getKey())
                            .monitoringResources(distinctList(e.getValue())))
                .toList());
  }

  private FunctionMonitoringResource frontalFunctionFrom(
      ComputeStackResource stackResource, boolean isLatest) {
    return functionMonitoringResourceFrom(
        stackResource.getFrontalFunctionName(),
        stackResource.getCreationDatetime(),
        isLatest && !stackResource.isFrontalFunctionDeleted());
  }

  private FunctionMonitoringResource workerFunctionFrom(
      WorkerFunction worker, Instant creationDatetime, boolean isLatest) {
    return functionMonitoringResourceFrom(
        worker.getName(), creationDatetime, isLatest && !worker.isDeleted());
  }

  private FunctionMonitoringResource functionMonitoringResourceFrom(
      String functionName, Instant creationDatetime, boolean withMonitoringUri) {
    return new FunctionMonitoringResource()
        .monitoringUri(
            withMonitoringUri
                ? computeStackResourceService.getFunctionDashboardUrl(functionName)
                : null)
        .name(functionName)
        .creationDatetime(creationDatetime);
  }

  private List<FunctionMonitoringResource> distinctList(
      List<FunctionMonitoringResource> resources) {
    return resources.stream().filter(distinctByKey(FunctionMonitoringResource::getName)).toList();
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), TRUE) == null;
  }
}
