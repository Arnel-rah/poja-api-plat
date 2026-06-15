package api.poja.io.repository.model.mapper;

import static java.lang.Boolean.TRUE;
import static java.time.Instant.now;

import api.poja.io.endpoint.rest.model.CloneTemplateApplication;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.ApplicationTemplate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component("DomainApplicationMapper")
@AllArgsConstructor
public class ApplicationMapper {

  public Application toDomain(api.poja.io.endpoint.rest.model.ApplicationBase rest) {
    var githubRepository = rest.getGithubRepository();
    var isArchived = TRUE.equals(rest.getArchived());
    var archivedAt = isArchived ? now() : null;
    var githubRepoId =
        githubRepository.getId() != null ? githubRepository.getId().toString() : null;

    return Application.builder()
        .id(rest.getId())
        .name(rest.getName())
        .githubRepositoryName(githubRepository.getName())
        .isGithubRepositoryPrivate(TRUE.equals(githubRepository.getIsPrivate()))
        .userId(rest.getUserId())
        .orgId(rest.getOrgId())
        .archived(isArchived)
        .archivedAt(archivedAt)
        .description(githubRepository.getDescription())
        .installationId(githubRepository.getInstallationId())
        .importId(rest.getApplicationImport() != null ? rest.getApplicationImport().getId() : null)
        .githubRepositoryUrl(
            githubRepository.getHtmlUrl() != null ? githubRepository.getHtmlUrl().toString() : null)
        .githubRepositoryId(githubRepoId)
        .build();
  }

  public Application toDomain(ApplicationImport appImport, String id) {
    return Application.builder()
        .id(id)
        .name(appImport.getAppName())
        .githubRepositoryId(appImport.getGithubRepositoryId())
        .githubRepositoryName(appImport.getGithubRepositoryName())
        .githubRepositoryUrl(appImport.getGithubRepositoryHttpUrl())
        .isGithubRepositoryPrivate(appImport.isGithubRepositoryPrivate())
        .userId(appImport.getUserId())
        .orgId(appImport.getOrgId())
        .description(appImport.getGithubRepositoryDescription())
        .installationId(appImport.getAppInstallationId())
        .importId(appImport.getId())
        .archived(false)
        .build();
  }

  public Application toDomain(
      String orgId, ApplicationTemplate template, CloneTemplateApplication app) {
    return Application.builder()
        .id(app.getId())
        .name(app.getName())
        .userId(app.getUserId())
        .orgId(orgId)
        .archived(false)
        .archivedAt(null)
        .description(template.getDescription())
        .githubRepositoryName(app.getRepositoryName())
        .isGithubRepositoryPrivate(app.getIsPrivate())
        .installationId(app.getInstallationId())
        .build();
  }
}
