package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.ApplicationImportMapper;
import api.poja.io.endpoint.rest.mapper.ApplicationImportStateMapper;
import api.poja.io.endpoint.rest.model.ApplicationImport;
import api.poja.io.endpoint.rest.model.CreateApplicationImportRequestBody;
import api.poja.io.endpoint.rest.model.GetApplicationImportStates;
import api.poja.io.endpoint.rest.model.PagedApplicationImportsResponse;
import api.poja.io.endpoint.rest.model.UpdateApplicationImportStatesRequestBody;
import api.poja.io.endpoint.rest.security.model.ApplicationImportPrincipal;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.page.Page;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationImportController {
  private final ApplicationImportService service;
  private final ApplicationImportStateService stateService;
  private final ApplicationImportMapper mapper;
  private final ApplicationImportStateMapper stateMapper;

  public ApplicationImportController(
      ApplicationImportService service,
      ApplicationImportStateService stateService,
      @Qualifier("applicationImportRestMapper") ApplicationImportMapper mapper,
      ApplicationImportStateMapper stateMapper) {
    this.service = service;
    this.stateService = stateService;
    this.mapper = mapper;
    this.stateMapper = stateMapper;
  }

  @PostMapping("/orgs/{orgId}/imports")
  public ApplicationImport importApplication(
      @PathVariable String orgId,
      @RequestBody CreateApplicationImportRequestBody createImportRequestBody) {
    return mapper.toRest(service.importApplication(orgId, createImportRequestBody));
  }

  @PostMapping("/orgs/{orgId}/imports/{importId}/states")
  public GetApplicationImportStates updateApplicationImportStates(
      @PathVariable String orgId,
      @PathVariable String importId,
      @RequestBody UpdateApplicationImportStatesRequestBody requestBody,
      @AuthenticationPrincipal ApplicationImportPrincipal principal) {
    var appImport = service.getByIdAndRepositoryId(importId, principal.getRepoId());
    var data =
        stateService.updateApplicationImportStates(
            appImport.getOrgId(), appImport.getId(), requestBody);
    return new GetApplicationImportStates().data(data.stream().map(stateMapper::toRest).toList());
  }

  @GetMapping("/orgs/{orgId}/imports")
  public PagedApplicationImportsResponse getOrganizationApplicationImports(
      @PathVariable String orgId,
      @RequestParam(required = false, name = "page", defaultValue = "1") PageFromOne pageFromOne,
      @RequestParam(required = false, name = "page_size", defaultValue = "10")
          BoundedPageSize boundedPageSize) {
    Page<ApplicationImport> paged =
        service.findPaginatedByOrgId(orgId, pageFromOne, boundedPageSize).map(mapper::toRest);
    return new PagedApplicationImportsResponse()
        .pageNumber(paged.queryPage().getValue())
        .pageSize(paged.queryPageSize().getValue())
        .hasPrevious(paged.hasPrevious())
        .count(paged.count())
        .data(paged.data().stream().toList());
  }

  @GetMapping("/orgs/{orgId}/imports/{importId}")
  public ApplicationImport getApplicationImportById(
      @PathVariable String orgId, @PathVariable String importId) {
    return mapper.toRest(service.getByOrgIdAndId(orgId, importId));
  }

  @GetMapping("/orgs/{orgId}/imports/{importId}/states")
  public GetApplicationImportStates getApplicationImportStates(
      @PathVariable String orgId, @PathVariable String importId) {
    // assert orgId != null;
    var data =
        stateService.getStatesByImportId(importId).stream().map(stateMapper::toRest).toList();
    return new GetApplicationImportStates().data(data);
  }
}
