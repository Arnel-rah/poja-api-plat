package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.AppEnvDeploymentsRestMapper;
import api.poja.io.endpoint.rest.mapper.DeploymentStateMapper;
import api.poja.io.endpoint.rest.model.AppEnvDeployment;
import api.poja.io.endpoint.rest.model.DeploymentState;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.endpoint.rest.model.PagedDeploymentsResponse;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.AppEnvironmentDeploymentService;
import api.poja.io.service.workflows.DeploymentStateService;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ApplicationDeploymentsController {
  private final AppEnvironmentDeploymentService appEnvironmentDeploymentService;
  private final AppEnvDeploymentsRestMapper mapper;
  private final DeploymentStateService deploymentStateService;
  private final DeploymentStateMapper deploymentStateMapper;

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/deployments")
  public PagedDeploymentsResponse getApplicationDeployments(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @RequestParam(required = false) Instant startDatetime,
      @RequestParam(required = false) Instant endDatetime,
      @RequestParam(required = false) EnvironmentType environmentType,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false, name = "page_size") BoundedPageSize pageSize) {
    var pagedResults =
        appEnvironmentDeploymentService.findAllByCriteria(
            orgId, applicationId, environmentType, startDatetime, endDatetime, page, pageSize);
    List<AppEnvDeployment> data = pagedResults.data().stream().map(mapper::toRest).toList();

    return new PagedDeploymentsResponse()
        .count(pagedResults.count())
        .hasPrevious(pagedResults.hasPrevious())
        .data(data);
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/deployments/{deploymentId}")
  public AppEnvDeployment getApplicationDeployment(@PathVariable String deploymentId) {
    return mapper.toRest(appEnvironmentDeploymentService.getById(deploymentId));
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/deployments/{deploymentId}/config")
  public OneOfPojaConf getApplicationDeploymentConfig(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String deploymentId) {
    return appEnvironmentDeploymentService.getConfig(orgId, applicationId, deploymentId);
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/deployments/{deploymentId}/states")
  public DeploymentState getApplicationDeploymentProgression(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String deploymentId) {
    return deploymentStateMapper.toRest(
        deploymentStateService.getSortedDeploymentStatesByDeploymentId(
            orgId, applicationId, deploymentId));
  }
}
