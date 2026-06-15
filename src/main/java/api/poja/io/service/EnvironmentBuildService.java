package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_GENERATION_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_PUSH_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_PUSH_SUCCESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.DEPLOYMENT_WORKFLOW_FAILED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.DEPLOYMENT_WORKFLOW_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.TEMPLATE_FILE_CHECK_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.GithubWorkflowStateEnum.RUNNING;
import static api.poja.io.file.ExtendedBucketComponent.getOrgBucketKey;
import static api.poja.io.file.ExtendedBucketComponent.getTempBucketKey;
import static api.poja.io.file.ExtendedBucketComponent.getUserBucketKey;
import static api.poja.io.file.FileType.BUILT_PACKAGE;
import static api.poja.io.file.FileType.DEPLOYMENT_FILE;
import static api.poja.io.service.event.PojaConfUploadedService.JCLOUDIFY_BOT_USERNAME;
import static api.poja.io.service.event.PojaConfUploadedService.POJA_BOT_USERNAME;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.CheckTemplateIntegrityTriggered;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.rest.model.BuildUploadRequestResponse;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.GithubWorkflowState;
import api.poja.io.endpoint.rest.model.GithubWorkflowStateEnum;
import api.poja.io.endpoint.rest.model.GithubWorkflowStateResponse;
import api.poja.io.endpoint.rest.security.AuthenticatedResourceProvider;
import api.poja.io.endpoint.validator.BuiltEnvInfoValidator;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.DeploymentState;
import api.poja.io.repository.model.EnvBuildRequest;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.workflows.DeploymentStateService;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class EnvironmentBuildService {
  private static final String ZIP_FILE_EXTENSION = ".zip";
  private final ExtendedBucketComponent bucketComponent;
  private final AuthenticatedResourceProvider authenticatedResourceProvider;
  private final EnvironmentService environmentService;
  private final EventProducer<PojaEvent> eventProducer;
  private final EnvBuildRequestService envBuildRequestService;
  private final BuiltEnvInfoValidator builtEnvInfoValidator;
  private final AppEnvironmentDeploymentService appEnvironmentDeploymentService;
  private final DeploymentStateService deploymentStateService;
  private final EnvDeploymentConfService envDeploymentConfService;
  private static final Pattern POJA_BOT_COMMIT_PATTERN =
      Pattern.compile("deployment ID:\\s*(\\S+)");

  private AppEnvironmentDeployment newAppEnvDeploymentFromScratch(
      String repoOwnerName, String repoName, GithubWorkflowState state) {
    Application authenticatedApplication =
        authenticatedResourceProvider.getAuthenticatedApplication();
    String appId = authenticatedApplication.getId();
    Environment environment =
        environmentService.getUserApplicationEnvironmentByIdAndType(
            appId, state.getEnvironmentType());
    EnvDeploymentConf latestDeploymentConf = environment.getLatestDeploymentConf();
    return AppEnvironmentDeployment.builder()
        .appId(appId)
        .env(environment)
        .envDeplConfId(latestDeploymentConf.getId())
        .ghRepoName(repoName)
        .ghRepoOwnerName(repoOwnerName)
        .ghCommitMessage(state.getCommitMessage())
        .ghCommitSha(state.getCommitSha())
        .build();
  }

  private AppEnvironmentDeployment newAppEnvDeploymentFromExisting(
      String repoOwnerName,
      String repoName,
      GithubWorkflowState state,
      AppEnvironmentDeployment appEnvironmentDeployment) {
    Application authenticatedApplication =
        authenticatedResourceProvider.getAuthenticatedApplication();
    String appId = authenticatedApplication.getId();
    Environment environment =
        environmentService.getUserApplicationEnvironmentByIdAndType(
            appId, state.getEnvironmentType());
    return AppEnvironmentDeployment.builder()
        .appId(appId)
        .env(environment)
        .envDeplConfId(appEnvironmentDeployment.getEnvDeplConfId())
        .ghRepoName(repoName)
        .ghRepoOwnerName(repoOwnerName)
        .ghCommitMessage(state.getCommitMessage())
        .ghCommitSha(state.getCommitSha())
        .build();
  }

  private void savePreviousDeploymentStates(String appId, String appEnvDeploymentId) {
    deploymentStateService.save(appId, appEnvDeploymentId, CODE_GENERATION_IN_PROGRESS);
    deploymentStateService.save(appId, appEnvDeploymentId, CODE_PUSH_IN_PROGRESS);
    deploymentStateService.save(appId, appEnvDeploymentId, CODE_PUSH_SUCCESS);
  }

  private AppEnvironmentDeployment updateJcloudifyBotAppEnvDepl(
      String appEnvDeploymentId, String repoOwnerName, String repoName, GithubWorkflowState state) {
    AppEnvironmentDeployment actual =
        appEnvironmentDeploymentService.getByIdAndEnvType(
            appEnvDeploymentId, state.getEnvironmentType());
    Optional<DeploymentState> optionalLatestState =
        deploymentStateService.getOptionalLatestDeploymentStateByDeploymentId(appEnvDeploymentId);
    boolean isManualRedeploy = optionalLatestState.filter(this::isManualRedeploy).isPresent();
    if (isManualRedeploy) {
      var appEnvDeployment =
          appEnvironmentDeploymentService.save(
              newAppEnvDeploymentFromExisting(repoOwnerName, repoName, state, actual));
      log.info(
          "Manual redeploy detected for appEnvDeploymentId {}. Created new appEnvDeployment {}",
          appEnvDeploymentId,
          appEnvDeployment);
      return appEnvDeployment;
    }
    return appEnvironmentDeploymentService.save(
        actual.toBuilder()
            .ghRepoName(repoName)
            .ghRepoOwnerName(repoOwnerName)
            .ghCommitMessage(state.getCommitMessage())
            .ghCommitSha(state.getCommitSha())
            .build());
  }

  private boolean isManualRedeploy(DeploymentState state) {
    DeploymentStateEnum progressionStatus = state.getProgressionStatus();
    return !DEPLOYMENT_WORKFLOW_IN_PROGRESS.equals(progressionStatus)
        && !CODE_PUSH_SUCCESS.equals(progressionStatus);
  }

  public GithubWorkflowStateResponse updateGithubWorkflowState(
      String repoOwnerName, String repoName, GithubWorkflowState state) {
    log.info(
        "Update github workflow state for repo {}/{} with state {}",
        repoOwnerName,
        repoName,
        state);
    String commitMessage = state.getCommitMessage();
    if (commitMessage != null) {
      GithubWorkflowStateEnum actualStatus = state.getStatus();
      var appEnvironmentDeployment =
          handleCommitAndUpdateState(repoOwnerName, repoName, state, actualStatus);
      return new GithubWorkflowStateResponse()
          .appEnvDeploymentId(appEnvironmentDeployment.getId())
          .status(state.getStatus())
          .commitMessage(commitMessage)
          .commitSha(state.getCommitSha())
          .commitAuthorName(state.getCommitAuthorName())
          .workflowRunId(state.getWorkflowRunId())
          .workflowRunAttempt(state.getWorkflowRunAttempt())
          .environmentType(state.getEnvironmentType());
    }
    throw new BadRequestException("Invalid commitMessage");
  }

  private AppEnvironmentDeployment handleCommitAndUpdateState(
      String repoOwnerName,
      String repoName,
      GithubWorkflowState state,
      GithubWorkflowStateEnum actualStatus) {
    var appEnvironmentDeployment = handleCommit(repoOwnerName, repoName, state);
    var updatedAppEnvDepl =
        saveWorkflowId(
            appEnvironmentDeployment, state.getWorkflowRunId(), state.getWorkflowRunAttempt());
    deploymentStateService.save(
        appEnvironmentDeployment.getAppId(),
        appEnvironmentDeployment.getId(),
        RUNNING.equals(actualStatus)
            ? DEPLOYMENT_WORKFLOW_IN_PROGRESS
            : DEPLOYMENT_WORKFLOW_FAILED);
    return updatedAppEnvDepl;
  }

  private AppEnvironmentDeployment saveWorkflowId(
      AppEnvironmentDeployment appEnvironmentDeployment,
      String workflowId,
      String workflowRunAttempt) {
    appEnvironmentDeploymentService.updateWorkflowRunId(
        appEnvironmentDeployment.getId(), workflowId, workflowRunAttempt);
    appEnvironmentDeployment.setGhWorkflowRunId(workflowId);
    return appEnvironmentDeployment;
  }

  private AppEnvironmentDeployment handleCommit(
      String repoOwnerName, String repoName, GithubWorkflowState state) {
    Matcher matcher =
        POJA_BOT_COMMIT_PATTERN.matcher(Objects.requireNonNull(state.getCommitMessage()));
    boolean match = matcher.find();
    boolean isJcloudifyBotCommit =
        match
            && (POJA_BOT_USERNAME.equals(state.getCommitAuthorName())
                || JCLOUDIFY_BOT_USERNAME.equals(state.getCommitAuthorName()));
    var runsOnCustomAppEnvDepl = state.getAppEnvDeploymentId() != null;
    if (runsOnCustomAppEnvDepl || isJcloudifyBotCommit) {
      var appEnvDeplId =
          state.getAppEnvDeploymentId() != null ? state.getAppEnvDeploymentId() : matcher.group(1);
      return handleJCloudifyBotCommit(repoOwnerName, repoName, state, appEnvDeplId);
    } else {
      boolean isFakeJcloudifyBotCommit =
          (!match
                  && (POJA_BOT_USERNAME.equals(state.getCommitAuthorName())
                      || JCLOUDIFY_BOT_USERNAME.equals(state.getCommitAuthorName())))
              || (match
                  && (!POJA_BOT_USERNAME.equals(state.getCommitAuthorName())
                      && !JCLOUDIFY_BOT_USERNAME.equals(state.getCommitAuthorName())));
      if (isFakeJcloudifyBotCommit) {
        throw new BadRequestException("Invalid commitMessage");
      } else {
        return handleUserCommit(repoOwnerName, repoName, state);
      }
    }
  }

  private AppEnvironmentDeployment handleJCloudifyBotCommit(
      String repoOwnerName, String repoName, GithubWorkflowState state, String appEnvDeploymentId) {
    try {
      return updateJcloudifyBotAppEnvDepl(appEnvDeploymentId, repoOwnerName, repoName, state);
    } catch (NotFoundException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  private AppEnvironmentDeployment handleUserCommit(
      String repoOwnerName, String repoName, GithubWorkflowState state) {
    String appEnvDeploymentId = state.getAppEnvDeploymentId();
    AppEnvironmentDeployment appEnvironmentDeployment =
        updateUserAppEnvDepl(repoOwnerName, repoName, state, appEnvDeploymentId);
    savePreviousDeploymentStates(
        appEnvironmentDeployment.getAppId(), appEnvironmentDeployment.getId());
    return appEnvironmentDeployment;
  }

  private AppEnvironmentDeployment updateUserAppEnvDepl(
      String repoOwnerName, String repoName, GithubWorkflowState state, String appEnvDeploymentId) {
    if (appEnvDeploymentId == null) {
      return appEnvironmentDeploymentService.save(
          newAppEnvDeploymentFromScratch(repoOwnerName, repoName, state));
    } else {
      return appEnvironmentDeploymentService.getByIdAndEnvType(
          appEnvDeploymentId, state.getEnvironmentType());
    }
  }

  public BuildUploadRequestResponse getZippedBuildUploadRequestDetails(
      EnvironmentType environmentType) {
    Application authenticatedApplication =
        authenticatedResourceProvider.getAuthenticatedApplication();
    String appId = authenticatedApplication.getId();
    String orgId = authenticatedApplication.getOrgId();
    String userId = authenticatedApplication.getUserId();
    Environment env =
        environmentService.getUserApplicationEnvironmentByIdAndType(appId, environmentType);
    String environmentId = env.getId();
    String bucketKey = getTempBucketKey(ZIP_FILE_EXTENSION);
    Duration fifteenMinutes = Duration.ofMinutes(15);
    var uri = bucketComponent.getPresignedPutObjectUri(bucketKey, fifteenMinutes);
    var buildTemplateFilename = env.getLatestDeploymentConf().getBuildTemplateFile();
    var buildTemplateUri =
        bucketComponent.presignGetObject(
            getBuildTemplateBucketKey(orgId, userId, appId, environmentId, buildTemplateFilename),
            fifteenMinutes);
    return new BuildUploadRequestResponse()
        .uri(uri)
        .filename(bucketKey)
        .buildTemplateFileUri(buildTemplateUri);
  }

  private String getBuildTemplateBucketKey(
      String orgId, String userId, String appId, String environmentId, String filename) {
    var orgBucketKey = getOrgBucketKey(orgId, appId, environmentId, DEPLOYMENT_FILE, filename);
    var userBucketKey = getUserBucketKey(userId, appId, environmentId, DEPLOYMENT_FILE, filename);

    if (!bucketComponent.doesExist(orgBucketKey)) {
      return userBucketKey;
    }
    return orgBucketKey;
  }

  @Transactional
  public void initiateDeployment(
      String repoOwnerName, String repoName, String installationId, BuiltEnvInfo builtEnvInfo) {
    if (envBuildRequestService.existsById(builtEnvInfo.getId())) {
      throw new BadRequestException("EnvBuildRequest has already been sent");
    }
    builtEnvInfoValidator.accept(builtEnvInfo);
    Application authenticatedApplication =
        authenticatedResourceProvider.getAuthenticatedApplication();
    String appId = authenticatedApplication.getId();
    String orgId = authenticatedApplication.getOrgId();
    var environment =
        environmentService.getUserApplicationEnvironmentByIdAndType(
            appId, builtEnvInfo.getEnvironmentType());
    var linkedAppEnvDepl =
        appEnvironmentDeploymentService.getById(builtEnvInfo.getAppEnvDeploymentId());
    var latestDeploymentConf =
        getLatestDeploymentConf(builtEnvInfo.getEnvDeplConfId(), linkedAppEnvDepl, environment);
    var formattedOriginalTemplateFilename =
        getOrgBucketKey(
            orgId,
            appId,
            environment.getId(),
            DEPLOYMENT_FILE,
            latestDeploymentConf.getBuildTemplateFile());
    String builtPackageBucketKey =
        getOrgBucketKey(
            orgId,
            appId,
            environment.getId(),
            BUILT_PACKAGE,
            "build" + randomUUID() + ZIP_FILE_EXTENSION);
    copyFromTempToRealKey(builtEnvInfo.getFormattedBucketKey(), builtPackageBucketKey);
    saveAndUpdateAppEnvironmentDeployment(linkedAppEnvDepl, installationId, latestDeploymentConf);
    envBuildRequestService.save(
        EnvBuildRequest.builder()
            .id(builtEnvInfo.getId())
            .appId(appId)
            .appEnvDeplId(linkedAppEnvDepl.getId())
            .envId(environment.getId())
            .orgId(orgId)
            .builtZipFileKey(builtPackageBucketKey)
            .creationTimestamp(now())
            .build());
    if (builtEnvInfo.getTagName() != null
        && !builtEnvInfo.getTagName().isBlank()
        && builtEnvInfo.getTagMessage() != null) {
      appEnvironmentDeploymentService.updateGhTagInfo(
          linkedAppEnvDepl.getId(), builtEnvInfo.getTagName(), builtEnvInfo.getTagMessage());
    }
    eventProducer.accept(
        List.of(
            CheckTemplateIntegrityTriggered.builder()
                .orgId(orgId)
                .appId(appId)
                .envId(environment.getId())
                .builtEnvInfo(builtEnvInfo)
                .builtProjectBucketKey(builtPackageBucketKey)
                .templateFileBucketKey(formattedOriginalTemplateFilename)
                .deploymentConfId(latestDeploymentConf.getId())
                .appEnvDeploymentId(linkedAppEnvDepl.getId())
                .build()));
    deploymentStateService.save(
        linkedAppEnvDepl.getAppId(), linkedAppEnvDepl.getId(), TEMPLATE_FILE_CHECK_IN_PROGRESS);
  }

  private EnvDeploymentConf getLatestDeploymentConf(
      String envDeplConfId,
      AppEnvironmentDeployment savedAppEnvironmentDepl,
      Environment environment) {
    if (envDeplConfId != null) {
      return envDeploymentConfService.getById(envDeplConfId);
    }
    String linkedAppEnvDeplEnvDeplConfId = savedAppEnvironmentDepl.getEnvDeplConfId();
    if (linkedAppEnvDeplEnvDeplConfId == null) {
      return environment.getLatestDeploymentConf();
    }
    return envDeploymentConfService.getById(linkedAppEnvDeplEnvDeplConfId);
  }

  private AppEnvironmentDeployment saveAndUpdateAppEnvironmentDeployment(
      AppEnvironmentDeployment actual, String installationId, EnvDeploymentConf deploymentConf) {
    return appEnvironmentDeploymentService.save(actual, installationId, deploymentConf);
  }

  private void copyFromTempToRealKey(String tempFilePath, String realFilePath) {
    bucketComponent.moveFile(tempFilePath, realFilePath);
    bucketComponent.deleteFile(tempFilePath);
  }
}
