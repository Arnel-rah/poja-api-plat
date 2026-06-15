package api.poja.io.service;

import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.page.Page;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.GhListAppInstallationReposResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppInstallationGithubRepoService {
  private final AppInstallationService appInstallationService;
  private final GithubService githubService;

  public Page<GhListAppInstallationReposResponse.Repository> getOrgInstallationRepositories(
      String installationId, PageFromOne page, BoundedPageSize pageSize) {
    AppInstallation installation = appInstallationService.getById(installationId);
    return getOrgInstallationRepositories(installation, page, pageSize);
  }

  private Page<GhListAppInstallationReposResponse.Repository> getOrgInstallationRepositories(
      AppInstallation installation, PageFromOne page, BoundedPageSize pageSize) {
    return new Page<>(
        page,
        pageSize,
        githubService.listApplicationInstallationRepos(installation.getGhId(), page, pageSize));
  }

  private Page<GhListAppInstallationReposResponse.Repository> searchAppInstallationRepositories(
      AppInstallation appInstallation,
      String token,
      String q,
      PageFromOne page,
      BoundedPageSize pageSize) {
    return new Page<>(
        page,
        pageSize,
        githubService.searchReposWithToken(
            formatScopedQuery(q, appInstallation.getOwnerGithubLogin()), token, page, pageSize));
  }

  public Page<GhListAppInstallationReposResponse.Repository> searchReposWithBearer(
      String token, String installationId, String q, PageFromOne page, BoundedPageSize pageSize) {
    var appInstallation = appInstallationService.getById(installationId);
    return switch (appInstallation.getRepositorySelection()) {
      case ALL -> searchAppInstallationRepositories(appInstallation, token, q, page, pageSize);
      case SELECTED ->
          getOrgInstallationRepositories(appInstallation, page, pageSize)
              .filter(e -> repoMatches(e, q));
    };
  }

  private static boolean repoMatches(GhListAppInstallationReposResponse.Repository repo, String q) {
    return repo.name().toLowerCase().contains(q.toLowerCase());
  }

  private static String formatScopedQuery(String q, String scope) {
    return String.format("user:%s+fork:true+%s", scope, q);
  }
}
