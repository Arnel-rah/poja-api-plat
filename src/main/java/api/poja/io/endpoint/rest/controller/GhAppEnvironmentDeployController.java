package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.model.BuildUploadRequestResponse;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.GithubWorkflowState;
import api.poja.io.endpoint.rest.model.GithubWorkflowStateResponse;
import api.poja.io.endpoint.rest.security.model.ApplicationPrincipal;
import api.poja.io.service.EnvironmentBuildService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class GhAppEnvironmentDeployController {
  private final EnvironmentBuildService environmentBuildService;

  @GetMapping("/gh-repos/{repo_owner}/{repo_name}/upload-build-uri")
  public BuildUploadRequestResponse createFileUploadUri(
      @PathVariable("repo_owner") String repoOwner,
      @PathVariable("repo_name") String repoName,
      @RequestParam(name = "environment_type") EnvironmentType environmentType) {
    return environmentBuildService.getZippedBuildUploadRequestDetails(environmentType);
  }

  @PostMapping("/gh-repos/{repo_owner}/{repo_name}/github-workflow-state")
  public GithubWorkflowStateResponse updateGithubWorkflowState(
      @PathVariable("repo_owner") String repoOwner,
      @PathVariable("repo_name") String repoName,
      @RequestBody GithubWorkflowState state) {
    return environmentBuildService.updateGithubWorkflowState(repoOwner, repoName, state);
  }

  @PutMapping("/gh-repos/{repo_owner}/{repo_name}/env-deploys")
  public BuiltEnvInfo deployEnv(
      @PathVariable("repo_owner") String repoOwnerName,
      @PathVariable("repo_name") String repoName,
      @RequestParam(name = "environment_type") EnvironmentType environmentType,
      @AuthenticationPrincipal ApplicationPrincipal principal,
      @RequestBody BuiltEnvInfo payload) {
    environmentBuildService.initiateDeployment(
        repoOwnerName, repoName, principal.getInstallationId(), payload);
    return payload;
  }
}
