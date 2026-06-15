package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.StackMapper;
import api.poja.io.endpoint.rest.model.PagedStackEvents;
import api.poja.io.endpoint.rest.model.PagedStacksResponse;
import api.poja.io.endpoint.rest.model.Stack;
import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.stack.StackEventService;
import api.poja.io.service.stack.StackService;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class StackController {
  private final StackMapper mapper;
  private final StackService service;
  private final StackEventService stackEventService;

  @GetMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/stacks/{stackId}")
  public Stack getStacks(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @PathVariable String stackId) {
    return mapper.toRest(service.getById(orgId, applicationId, environmentId, stackId));
  }

  @GetMapping("/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/stacks")
  public PagedStacksResponse getStacks(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @RequestParam(required = false, name = "appEnvDeplId") String appEnvDeplId,
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "10", name = "page_size")
          BoundedPageSize pageSize) {
    var pagedData =
        service.findAllBy(orgId, applicationId, environmentId, appEnvDeplId, page, pageSize);
    return new PagedStacksResponse()
        .count(pagedData.count())
        .hasPrevious(pagedData.hasPrevious())
        .pageSize(pagedData.queryPageSize().getValue())
        .pageNumber(pagedData.queryPage().getValue())
        .data(pagedData.data().stream().map(mapper::toRest).toList());
  }

  @GetMapping(
      "/orgs/{orgId}/applications/{applicationId}/environments/{environmentId}/stacks/{stackType}/events")
  public PagedStackEvents getStackEvents(
      @PathVariable String orgId,
      @PathVariable String applicationId,
      @PathVariable String environmentId,
      @PathVariable StackType stackType,
      @RequestParam(required = false, name = "from") Instant from,
      @RequestParam(required = false, name = "to") Instant to,
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "10", name = "page_size")
          BoundedPageSize pageSize) {
    var stackEventData =
        stackEventService.getStackEvents(
            orgId, applicationId, environmentId, stackType, from, to, page, pageSize);
    var pagedData = stackEventData.stackData();
    return new PagedStackEvents()
        .count(pagedData.count())
        .hasPrevious(pagedData.hasPrevious())
        .pageSize(pagedData.queryPageSize().getValue())
        .pageNumber(pagedData.queryPage().getValue())
        .latestStackEventTimestamp(stackEventData.latestStackEventTimestamp())
        .data(pagedData.data().stream().toList());
  }
}
