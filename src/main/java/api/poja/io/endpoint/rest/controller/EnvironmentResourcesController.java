package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.model.PagedEnvResourceDeploymentResponse;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.EnvResourcesByDeploymentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class EnvironmentResourcesController {
  private final EnvResourcesByDeploymentService envResourcesByDeploymentService;

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/resources")
  public PagedEnvResourceDeploymentResponse getEnvResourcesGroupedByDeployment(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @RequestParam(value = "page", required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", required = false, defaultValue = "5")
          BoundedPageSize pageSize) {
    var pagedData =
        envResourcesByDeploymentService.getEnvironmentResourcesByDeploymentByCriteria(
            orgId, applicationId, environmentId, page, pageSize);
    return new PagedEnvResourceDeploymentResponse()
        .count(pagedData.count())
        .data(pagedData.data().stream().toList())
        .pageSize(pagedData.queryPageSize().getValue())
        .pageNumber(pagedData.queryPage().getValue())
        .hasPrevious(pagedData.hasPrevious());
  }
}
