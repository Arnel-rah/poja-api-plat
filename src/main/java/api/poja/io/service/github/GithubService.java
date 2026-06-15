package api.poja.io.service.github;

import api.poja.io.endpoint.rest.model.RefreshToken;
import api.poja.io.endpoint.rest.model.Token;
import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.github.model.CreateRepoFromTemplateRequestBody;
import api.poja.io.service.github.model.CreateRepoRequestBody;
import api.poja.io.service.github.model.CreateRepoResponse;
import api.poja.io.service.github.model.GhAppInstallation;
import api.poja.io.service.github.model.GhListAppInstallationReposResponse;
import api.poja.io.service.github.model.GhSecret;
import api.poja.io.service.github.model.GhWorkflowRunRequestBody;
import api.poja.io.service.github.model.UpdateRepoRequestBody;
import api.poja.io.service.github.model.UpdateRepoResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GithubService {
  private final GithubComponent githubComponent;

  public Token exchangeCodeToToken(String code) {
    return githubComponent.exchangeCodeToToken(code);
  }

  public CreateRepoResponse createRepoFor(CreateRepoRequestBody requestBody, String token) {
    return githubComponent.createRepoFor(requestBody, token);
  }

  public CreateRepoResponse createRepoFromTemplateFor(
      CreateRepoFromTemplateRequestBody requestBody, String token) {
    return githubComponent.createRepoFromTemplateFor(requestBody, token);
  }

  public UpdateRepoResponse updateRepoFor(
      UpdateRepoRequestBody application,
      String repositoryName,
      String token,
      String githubUsername) {
    return githubComponent.updateRepoFor(application, repositoryName, token, githubUsername);
  }

  public Set<GhAppInstallation> listApplicationInstallations() {
    return githubComponent.listInstallations();
  }

  public List<GhListAppInstallationReposResponse.Repository> listApplicationInstallationRepos(
      long installationId, PageFromOne page, BoundedPageSize pageSize) {
    return githubComponent
        .listInstallationRepositories(installationId, page, pageSize)
        .repositories();
  }

  public List<GhListAppInstallationReposResponse.Repository> searchReposWithToken(
      String q, String token, PageFromOne page, BoundedPageSize pageSize) {
    return githubComponent.searchReposWithToken(token, q, page, pageSize).repositories();
  }

  public String getInstallationToken(long installationId, Duration duration) {
    return githubComponent.getAppInstallationToken(installationId, duration);
  }

  public GhAppInstallation getInstallationByGhId(long ghId) {
    return githubComponent.getInstallationById(ghId);
  }

  public Token refreshToken(RefreshToken refreshToken) {
    return githubComponent.refreshToken(refreshToken.getRefreshToken());
  }

  public void runWorkflowDispatch(
      String owner,
      String repoName,
      String token,
      String workflowId,
      GhWorkflowRunRequestBody workflowRunRequestBody) {
    githubComponent.runWorkflowDispatch(owner, repoName, token, workflowId, workflowRunRequestBody);
  }

  public void crupdateSecret(String owner, String repoName, String token, GhSecret ghSecret) {
    githubComponent.crupdateSecret(owner, repoName, token, ghSecret);
  }

  public void crupdateSecrets(
      Set<GhSecret> ghSecrets, String repoOwner, String repoName, String token) {
    ghSecrets.forEach(s -> crupdateSecret(repoOwner, repoName, token, s));
  }
}
