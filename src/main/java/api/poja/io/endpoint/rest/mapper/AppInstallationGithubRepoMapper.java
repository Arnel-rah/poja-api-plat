package api.poja.io.endpoint.rest.mapper;

import static java.net.URI.create;

import api.poja.io.endpoint.rest.model.GithubRepositoryListItem;
import api.poja.io.service.github.model.GhListAppInstallationReposResponse;
import org.springframework.stereotype.Component;

@Component
public class AppInstallationGithubRepoMapper {
  public GithubRepositoryListItem toRest(
      String installationId, GhListAppInstallationReposResponse.Repository domain) {
    return new GithubRepositoryListItem()
        .id(String.valueOf(domain.id()))
        .installationId(installationId)
        .isPrivate(domain.isPrivate())
        .description(domain.description())
        .htmlUrl(create(domain.htmlUrl()))
        .defaultBranch(domain.defaultBranch())
        .isEmpty(domain.size() == 0)
        .name(domain.name());
  }
}
