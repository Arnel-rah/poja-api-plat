package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.AppSetupStateEnum.REPO_CREATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.REPO_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.REPO_CREATION_SUCCESS;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.ApplicationCreated;
import api.poja.io.endpoint.event.model.EnvironmentDeploymentRequested;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.AppSetupStateService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.CreateRepoRequestBody;
import api.poja.io.service.github.model.CreateRepoResponse;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
@AllArgsConstructor
@Slf4j
public class ApplicationCreatedService implements Consumer<ApplicationCreated> {

  private final ApplicationService applicationService;
  private final GithubService githubService;
  private final AppInstallationService installationService;
  private final ApplicationRepository repository;
  private final EnvironmentService environmentService;
  private final EventProducer<EnvironmentDeploymentRequested> eventProducer;
  private final AppSetupStateService appSetupStateService;

  @SneakyThrows
  @Override
  @Transactional
  public void accept(ApplicationCreated event) {
    var orgId = event.getOrgId();
    var appId = event.getAppId();
    var app = applicationService.getById(appId, orgId);
    var installation = installationService.getById(app.getInstallationId());
    var token =
        githubService.getInstallationToken(installation.getGhId(), event.maxConsumerDuration());
    var owner = installation.getOwnerGithubLogin();
    var env = environmentService.getByApplicationId(appId);
    var created = createRepo(owner, app, token);
    repository.updateApplicationRepoUrl(
        app.getId(), String.valueOf(created.htmlUrl()), created.id());
    log.info(
        "Repository created for app {}: url={}, id={}",
        app.getId(),
        created.htmlUrl(),
        created.id());
    eventProducer.accept(
        List.of(
            new EnvironmentDeploymentRequested(orgId, appId, env.getId(), env.getCurrentConfId())));
  }

  private CreateRepoResponse createRepo(String owner, Application app, String token)
      throws IllegalStateTransitionException {
    appSetupStateService.save(app.getOrgId(), app.getId(), REPO_CREATION_IN_PROGRESS);
    try {
      var repoCreated =
          githubService.createRepoFor(
              new CreateRepoRequestBody(
                  owner,
                  app.getGithubRepositoryName(),
                  app.getDescription(),
                  app.isGithubRepositoryPrivate()),
              token);
      appSetupStateService.save(app.getOrgId(), app.getId(), REPO_CREATION_SUCCESS);

      return repoCreated;
    } catch (RestClientException e) {
      appSetupStateService.save(app.getOrgId(), app.getId(), REPO_CREATION_FAILED);
      throw e;
    }
  }
}
