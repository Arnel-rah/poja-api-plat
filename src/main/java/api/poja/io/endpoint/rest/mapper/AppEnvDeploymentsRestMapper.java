package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.service.event.PojaConfUploadedService.JCLOUDIFY_BOT_USERNAME;
import static api.poja.io.service.event.PojaConfUploadedService.POJA_BOT_USERNAME;

import api.poja.io.endpoint.rest.model.AppEnvDeployment;
import api.poja.io.endpoint.rest.model.GithubMeta;
import api.poja.io.endpoint.rest.model.GithubMetaCommit;
import api.poja.io.endpoint.rest.model.GithubMetaRepo;
import api.poja.io.endpoint.rest.model.GithubMetaTag;
import api.poja.io.endpoint.rest.model.GithubMetaWorkflow;
import api.poja.io.endpoint.rest.model.GithubUserMeta;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class AppEnvDeploymentsRestMapper {
  public AppEnvDeployment toRest(AppEnvironmentDeployment deployment) {
    GithubMetaRepo repo =
        new GithubMetaRepo()
            .ownerName(deployment.getGhRepoOwnerName())
            .name(deployment.getGhRepoName());
    String ghCommitterName = deployment.getGhCommitterName();
    String ghCommitterAvatarUrl = deployment.getGhCommitterAvatarUrl();
    URI avatarUrl = ghCommitterAvatarUrl == null ? null : URI.create(ghCommitterAvatarUrl);
    GithubUserMeta committer =
        new GithubUserMeta()
            .avatarUrl(avatarUrl)
            .email(deployment.getGhCommitterEmail())
            .githubId(deployment.getGhCommitterId())
            .isJcBot(
                POJA_BOT_USERNAME.equals(ghCommitterName)
                    || JCLOUDIFY_BOT_USERNAME.equals(ghCommitterName))
            .login(deployment.getGhCommitterLogin())
            .name(ghCommitterName);
    String ghCommitUrl = deployment.getGhCommitUrl();
    GithubMetaCommit commit =
        new GithubMetaCommit()
            .branch(deployment.getGhCommitBranch())
            .committer(committer)
            .message(deployment.getGhCommitMessage())
            .sha(deployment.getGhCommitSha())
            .url(ghCommitUrl == null ? null : URI.create(ghCommitUrl));
    String ghTagName = deployment.getGhTagName();
    GithubMetaTag tag =
        ghTagName == null || ghTagName.isBlank()
            ? null
            : new GithubMetaTag()
                .name(ghTagName)
                .message(deployment.getGhTagMessage())
                .url(deployment.getGhTagHtmlUri());
    GithubMetaWorkflow workflow =
        deployment.getGhWorkflowRunId() == null
            ? null
            : new GithubMetaWorkflow()
                .runId(deployment.getGhWorkflowRunId())
                .runAttempt(deployment.getGhWorkflowRunAttempt())
                .runUri(deployment.getGhWorkflowUri());
    var githubMeta = new GithubMeta().tag(tag).commit(commit).repo(repo).workflow(workflow);
    return new AppEnvDeployment()
        .applicationId(deployment.getAppId())
        .creationDatetime(deployment.getCreationDatetime())
        .deployedUrl(
            deployment.getDeployedUrl() == null ? null : URI.create(deployment.getDeployedUrl()))
        .environmentId(deployment.getEnv().getId())
        .confId(deployment.getEnvDeplConfId())
        .githubMeta(githubMeta)
        .id(deployment.getId());
  }
}
