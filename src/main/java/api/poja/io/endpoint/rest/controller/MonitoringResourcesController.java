package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.ComputeStackResourceMapper;
import api.poja.io.endpoint.rest.model.GroupedMonitoringResources;
import api.poja.io.service.ComputeStackResourceService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
/*
 * MonitoringResources is only comprised of ComputeStackResources because Compute Stacks are the
 * only ones we can monitor
 */
public class MonitoringResourcesController {
  private final ComputeStackResourceService computeStackResourceService;
  private final ComputeStackResourceMapper computeStackResourceMapper;

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/monitoringResources")
  public GroupedMonitoringResources getComputeStackResources(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @RequestParam(required = false) String environmentId) {
    var data =
        computeStackResourceService.findAllByCriteriaOrderByLatest(
            orgId, applicationId, environmentId);
    return computeStackResourceMapper.toRest(data);
  }
}
