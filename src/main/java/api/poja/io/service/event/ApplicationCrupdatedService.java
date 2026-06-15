package api.poja.io.service.event;

import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppArchivalRequested;
import api.poja.io.endpoint.event.model.ApplicationCrupdated;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.CreateRepoRequestBody;
import api.poja.io.service.github.model.CreateRepoResponse;
import api.poja.io.service.github.model.UpdateRepoRequestBody;
import api.poja.io.service.github.model.UpdateRepoResponse;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ApplicationCrupdatedService implements Consumer<ApplicationCrupdated> {
  private final GithubService githubService;
  private final AppInstallationService installationService;
  private final ApplicationRepository repository;
  private final EventProducer<AppArchivalRequested> eventProducer;

  @Override
  @Transactional
  public void accept(ApplicationCrupdated applicationCrupdated) {
    if (applicationCrupdated.isArchived()) {
      archiveApplication(applicationCrupdated.getApplicationId());
      return;
    }

    var persistedInstallation =
        installationService.getById(applicationCrupdated.getInstallationId());
    var token =
        githubService.getInstallationToken(
            persistedInstallation.getGhId(), applicationCrupdated.maxConsumerDuration());
    var owner = persistedInstallation.getOwnerGithubLogin();

    switch (applicationCrupdated.getCrupdateType()) {
      case CREATE -> {
        var created = createRepo(owner, applicationCrupdated, token);
        repository.updateApplicationRepoUrl(
            applicationCrupdated.getApplicationId(),
            String.valueOf(created.htmlUrl()),
            created.id());
      }
      case UPDATE -> {
        var updated = updateRepo(owner, applicationCrupdated, token);
        repository.updateApplicationRepoUrl(
            applicationCrupdated.getApplicationId(),
            String.valueOf(updated.htmlUrl()),
            updated.id());
      }
    }
  }

  private UpdateRepoResponse updateRepo(
      String repoOwnerUsername, ApplicationCrupdated applicationCrupdated, String token) {
    var updateRepoResponse =
        githubService.updateRepoFor(
            new UpdateRepoRequestBody(
                applicationCrupdated.getApplicationRepoName(),
                applicationCrupdated.getDescription(),
                applicationCrupdated.isRepoPrivate(),
                applicationCrupdated.isArchived()),
            applicationCrupdated.getPreviousApplicationRepoName() == null
                ? applicationCrupdated.getApplicationRepoName()
                : applicationCrupdated.getPreviousApplicationRepoName(),
            token,
            repoOwnerUsername);
    log.info("update repo {}", updateRepoResponse);
    return updateRepoResponse;
  }

  private CreateRepoResponse createRepo(
      String owner, ApplicationCrupdated applicationCrupdated, String token) {
    var createRepoResponse =
        githubService.createRepoFor(
            new CreateRepoRequestBody(
                owner,
                applicationCrupdated.getApplicationRepoName(),
                applicationCrupdated.getDescription(),
                applicationCrupdated.isRepoPrivate()),
            token);
    log.info("create repo {}", createRepoResponse);
    return createRepoResponse;
  }

  private void archiveApplication(String applicationId) {
    eventProducer.accept(
        List.of(AppArchivalRequested.builder().requestedAt(now()).appId(applicationId).build()));
  }
}
