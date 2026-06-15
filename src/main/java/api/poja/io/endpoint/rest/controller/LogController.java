package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.LogQueryMapper;
import api.poja.io.endpoint.rest.model.CreateLogQueryRequestBody;
import api.poja.io.endpoint.rest.model.CreateLogQueryResponse;
import api.poja.io.endpoint.rest.model.EnvFunctionLog;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.FunctionType;
import api.poja.io.endpoint.rest.model.PagedLogQuery;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.LogService;
import api.poja.io.service.logQuery.LogQueryService;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class LogController {
  private final LogService logService;
  private final LogQueryService logQueryService;
  private final LogQueryMapper logQueryMapper;

  @GetMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/functions/{functionName}/logs")
  public EnvFunctionLog getEnvFunctionLogByFunction(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @PathVariable String functionName) {
    return logService.getStackLogGroupByFunctionName(functionName);
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/log-queries/{logQueryId}")
  public PagedLogQuery getLogQueryById(
      @PathVariable String logQueryId,
      @RequestParam(required = false, defaultValue = "1", name = "page") PageFromOne page,
      @RequestParam(required = false, defaultValue = "10", name = "page_size")
          BoundedPageSize pageSize,
      @RequestParam(required = false, defaultValue = "PREPROD,PROD", name = "env_types")
          Set<EnvironmentType> environmentTypes,
      @RequestParam(required = false, defaultValue = "FRONTAL,WORKER", name = "function_types")
          Set<FunctionType> functionTypes) {
    return logQueryService.restById(logQueryId, page, pageSize, environmentTypes, functionTypes);
  }

  @PostMapping("/orgs/{orgId}/applications/{applicationId}/log-queries")
  public CreateLogQueryResponse createLogQuery(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @RequestBody CreateLogQueryRequestBody requestBody) {
    var saved = logQueryService.createNew(orgId, applicationId, requestBody);
    return logQueryMapper.toRest(saved, requestBody);
  }
}
