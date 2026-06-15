package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.GithubWorkflowRunRequested;
import api.poja.io.model.exception.ApiException;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.GhWorkflowRunRequestBody;
import api.poja.io.service.github.model.exception.WorkflowRunFailed;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GithubWorkflowRunRequestedService implements Consumer<GithubWorkflowRunRequested> {
  private final GithubService githubService;
  private final AppInstallationService appInstallationService;

  @Override
  public void accept(GithubWorkflowRunRequested githubWorkflowRunRequested) {
    var installation =
        appInstallationService.getById(githubWorkflowRunRequested.getAppInstallationId());
    var token = getInstallationToken(installation.getGhId(), Duration.ofMinutes(1));
    try {
      githubService.runWorkflowDispatch(
          githubWorkflowRunRequested.getGithubLogin(),
          githubWorkflowRunRequested.getRepositoryName(),
          token,
          githubWorkflowRunRequested.getToRun().getWorkflowId(),
          new GhWorkflowRunRequestBody(
              githubWorkflowRunRequested.getBranchName(),
              githubWorkflowRunRequested.getWorkflowParameters()));
    } catch (ApiException | WorkflowRunFailed e) {
      // Thrown exception is always of type model.ApiException due to restTemplate error handler
      githubService.runWorkflowDispatch(
          githubWorkflowRunRequested.getGithubLogin(),
          githubWorkflowRunRequested.getRepositoryName(),
          token,
          githubWorkflowRunRequested.getToRun().getWorkflowId(),
          new GhWorkflowRunRequestBody(
              githubWorkflowRunRequested.getBranchName(),
              githubWorkflowRunRequested.getDeprecatedWorkflowParameters()));
    }
  }

  private String getInstallationToken(long githubId, Duration tokenDuration) {
    return githubService.getInstallationToken(githubId, tokenDuration);
  }
}
