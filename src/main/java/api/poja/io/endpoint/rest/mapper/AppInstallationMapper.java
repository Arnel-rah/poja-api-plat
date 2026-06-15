package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.GithubAppInstallationRepositorySelection.ALL;
import static api.poja.io.endpoint.rest.model.GithubAppInstallationRepositorySelection.fromValue;

import api.poja.io.endpoint.rest.model.CreateGithubAppInstallation;
import api.poja.io.endpoint.rest.model.GithubAppInstallation;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.service.github.GithubService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppInstallationMapper {
  private final GithubService githubService;

  public GithubAppInstallation toRest(AppInstallation domain) {
    return new GithubAppInstallation()
        .owner(domain.getOwnerGithubLogin())
        .ghInstallationId(domain.getGhId())
        .id(domain.getId())
        .type(domain.getType())
        .repositorySelection(
            domain.getRepositorySelection() != null
                ? fromValue(domain.getRepositorySelection().toString())
                : ALL)
        .ghAvatarUrl(domain.getAvatarUrl());
  }

  public AppInstallation toDomain(String orgId, CreateGithubAppInstallation rest) {
    var app = githubService.getInstallationByGhId(rest.getGhInstallationId());
    String ownerGithubLogin = app.ownerGithubLogin();
    return AppInstallation.builder()
        .id(rest.getId())
        .ownerGithubLogin(ownerGithubLogin)
        .ghId(rest.getGhInstallationId())
        .type(app.type())
        .avatarUrl(app.avatarUrl())
        .orgId(orgId)
        .repositorySelection(app.repositorySelection())
        .build();
  }

  public AppInstallation toDomainForUser(String userId, CreateGithubAppInstallation rest) {
    var app = githubService.getInstallationByGhId(rest.getGhInstallationId());
    String ownerGithubLogin = app.ownerGithubLogin();
    return AppInstallation.builder()
        .id(rest.getId())
        .ownerGithubLogin(ownerGithubLogin)
        .ghId(rest.getGhInstallationId())
        .type(app.type())
        .avatarUrl(app.avatarUrl())
        .userId(userId)
        .repositorySelection(app.repositorySelection())
        .build();
  }
}
