package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.AppInstallationGithubRepoMapper;
import api.poja.io.endpoint.rest.mapper.AppInstallationMapper;
import api.poja.io.endpoint.rest.model.CrupdateGithubAppInstallationsRequestBody;
import api.poja.io.endpoint.rest.model.CrupdateGithubAppInstallationsResponse;
import api.poja.io.endpoint.rest.model.GithubAppInstallationsResponse;
import api.poja.io.endpoint.rest.model.GithubRepositoryListItem;
import api.poja.io.endpoint.rest.model.PagedGithubRepositoryResponse;
import api.poja.io.endpoint.rest.security.model.Principal;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.AppInstallationGithubRepoService;
import api.poja.io.service.AppInstallationService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AppInstallationController {
  private final AppInstallationService service;
  private final AppInstallationMapper mapper;
  private final AppInstallationGithubRepoService appInstallationGithubRepoService;
  private final AppInstallationGithubRepoMapper appInstallationGithubRepoMapper;

  @GetMapping("/orgs/{orgId}/installations")
  public GithubAppInstallationsResponse getOrgInstallations(@PathVariable String orgId) {
    var data = service.findAllByOrgId(orgId).stream().map(mapper::toRest).toList();
    return new GithubAppInstallationsResponse().data(data);
  }

  @GetMapping("/users/{userId}/installations")
  public GithubAppInstallationsResponse getUserInstallations(@PathVariable String userId) {
    var data = service.findAllByUserId(userId).stream().map(mapper::toRest).toList();
    return new GithubAppInstallationsResponse().data(data);
  }

  @PutMapping("/orgs/{orgId}/installations")
  public CrupdateGithubAppInstallationsResponse crupdateGithubAppInstallations(
      @PathVariable String orgId,
      @RequestBody CrupdateGithubAppInstallationsRequestBody requestBody) {
    var requestBodyData = requestBody.getData();
    var savedData =
        service.saveAll(
            requestBodyData.stream().map((rest) -> mapper.toDomain(orgId, rest)).toList());
    var data = savedData.stream().map(mapper::toRest).toList();
    return new CrupdateGithubAppInstallationsResponse().data(data);
  }

  @PutMapping("/users/{userId}/installations")
  public CrupdateGithubAppInstallationsResponse crupdateUserGithubAppInstallations(
      @PathVariable String userId,
      @RequestBody CrupdateGithubAppInstallationsRequestBody requestBody) {
    var requestBodyData = requestBody.getData();
    var savedData =
        service.saveAllForUser(
            requestBodyData.stream().map((rest) -> mapper.toDomainForUser(userId, rest)).toList());
    var data = savedData.stream().map(mapper::toRest).toList();
    return new CrupdateGithubAppInstallationsResponse().data(data);
  }

  @GetMapping("/orgs/{orgId}/installations/{installationId}/repositories")
  public PagedGithubRepositoryResponse getOrgInstallationRepositories(
      @PathVariable String orgId,
      @PathVariable String installationId,
      @RequestParam(value = "page", defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", defaultValue = "30") BoundedPageSize pageSize) {
    var paginatedData =
        appInstallationGithubRepoService.getOrgInstallationRepositories(
            installationId, page, pageSize);
    return new PagedGithubRepositoryResponse()
        .data(
            paginatedData.data().stream()
                .map(d -> appInstallationGithubRepoMapper.toRest(installationId, d))
                .toList())
        .count(paginatedData.count())
        .hasPrevious(paginatedData.hasPrevious())
        .pageNumber(paginatedData.queryPage().getValue())
        .pageSize(paginatedData.queryPageSize().getValue());
  }

  @GetMapping("/users/{userId}/installations/{installationId}/search/repositories")
  public PagedGithubRepositoryResponse searchInstallationRepositories(
      @AuthenticationPrincipal Principal principal,
      @PathVariable String userId,
      @PathVariable String installationId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(value = "page", defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", defaultValue = "30") BoundedPageSize pageSize) {
    assert userId != null;
    var paged =
        appInstallationGithubRepoService.searchReposWithBearer(
            principal.getBearer(), installationId, q, page, pageSize);
    return new PagedGithubRepositoryResponse()
        .data(
            (List<GithubRepositoryListItem>)
                paged.map(d -> appInstallationGithubRepoMapper.toRest(installationId, d)).data())
        .count(paged.count())
        .hasPrevious(paged.hasPrevious())
        .pageNumber(paged.queryPage().getValue())
        .pageSize(paged.queryPageSize().getValue());
  }

  @GetMapping("/users/{userId}/installations/{installationId}/repositories")
  public PagedGithubRepositoryResponse getUserInstallationRepositories(
      @PathVariable String userId,
      @PathVariable String installationId,
      @RequestParam(value = "page", defaultValue = "1") PageFromOne page,
      @RequestParam(value = "page_size", defaultValue = "30") BoundedPageSize pageSize) {
    var paginatedData =
        appInstallationGithubRepoService.getOrgInstallationRepositories(
            installationId, page, pageSize);
    return new PagedGithubRepositoryResponse()
        .data(
            paginatedData.data().stream()
                .map(d -> appInstallationGithubRepoMapper.toRest(installationId, d))
                .toList())
        .count(paginatedData.count())
        .hasPrevious(paginatedData.hasPrevious())
        .pageNumber(paginatedData.queryPage().getValue())
        .pageSize(paginatedData.queryPageSize().getValue());
  }
}
