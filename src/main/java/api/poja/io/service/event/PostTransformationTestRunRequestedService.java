package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.FORMATTED_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static api.poja.io.service.git.GitUtils.gitRm;
import static api.poja.io.service.git.GitUtils.pushAndCheckResult;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static api.poja.io.service.git.GitUtils.updateLocalUpstream;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.toUnmodifiableSet;

import api.poja.io.endpoint.event.model.PostTransformationTestRunRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.EnvVar;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.GhSecret;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PostTransformationTestRunRequestedService
    implements Consumer<PostTransformationTestRunRequested> {
  private final ExtendedBucketComponent bucketComponent;
  private final ApplicationImportMapper applicationImportMapper;
  private final GithubService githubService;
  private final AppInstallationService appInstallationService;
  private final ApplicationImportService applicationImportService;
  private final ApplicationImportStateService stateService;
  public static final String S3_POST_TRANSFORMATION_CI_DIR =
      "poja-templates/post-transformation-ci.yml";
  private static final String GITHUB_WORKFLOWS_DIR = ".github/workflows/";
  private static final String LOCAL_CI_DIR = GITHUB_WORKFLOWS_DIR + "post-transformation-ci.yml";
  private static final String ORG_ID_PLACEHOLDER = "<?org-id>";
  private static final String IMPORT_ID_PLACEHOLDER = "<?import-id>";
  private static final String ENV_VARS_PLACEHOLDER = "<?test-env-vars>";

  @Override
  public void accept(PostTransformationTestRunRequested event) {
    var importId = event.getImportId();

    log.info("Post transformation test run requested for ApplicationImport.id={}", importId);

    var importOpt = applicationImportService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found. Skipping post transformation test", importId);
      return;
    }
    var appImport = importOpt.get();

    var currentStateStatus =
        stateService.getStatesByImportId(importId).getFirst().getProgressionStatus();
    if (currentStateStatus == POST_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS
        || currentStateStatus == POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL) {
      log.info(
          "Post transformation test already {} for import {}. Skipping retry.",
          currentStateStatus,
          appImport.getId());
      return;
    }

    log.info("Running tests after code transformation for import {}", appImport.getId());
    stateService.updateState(
        appImport.getId(), POST_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS);

    var unknownApplication =
        applicationImportMapper.toUnknownApplication(appImport, FORMATTED_ZIPPED_CODE_SNAPSHOT);
    var appCode = unknownApplication.file();
    var appEnvVars = unknownApplication.envVars();
    var appInstallation = appInstallationService.getById(appImport.getAppInstallationId());
    var ghToken =
        githubService.getInstallationToken(appInstallation.getGhId(), event.maxConsumerDuration());
    var credentials = new UsernamePasswordCredentialsProvider("x-access-token", ghToken);

    try (var git = Git.open(appCode)) {
      var appCodePath = appCode.toPath();
      var branchName = appImport.ghBranchNamePrefix() + "-post-transformation";
      var doesBranchExist = doesBranchExist(credentials, git, branchName);

      checkoutBranch(doesBranchExist, git, branchName);
      rmExistingWorkflows(git, appCodePath);
      configureCI(appCodePath, appImport.getOrgId(), appImport.getId(), appEnvVars);
      git.add().addFilepattern(LOCAL_CI_DIR).call();
      unsignedCommitAsBot(git, "poja: import ID " + appImport.getId(), credentials, false);
      configureRepositorySecrets(
          appEnvVars,
          appInstallation.getOwnerGithubLogin(),
          appImport.getGithubRepositoryName(),
          ghToken);
      pushAndCheckResult(credentials, branchName, git);
      updateLocalUpstream(git, branchName);
    } catch (IOException | GitAPIException | ApiException e) {
      log.error("Error during post transformation test run for import {}", appImport.getId(), e);
      var errorLog = ApplicationImportLog.error(e.getMessage());
      stateService.updateState(
          appImport.getId(), POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED, List.of(errorLog));
    }
  }

  private void configureRepositorySecrets(
      Set<EnvVar> envVars, String ownerLogin, String repoName, String token) {
    var repoSecrets =
        envVars.stream()
            .filter(e -> e.testValue() != null && !e.testValue().isBlank())
            .map(PostTransformationTestRunRequestedService::toGhSecret)
            .collect(toUnmodifiableSet());
    githubService.crupdateSecrets(repoSecrets, ownerLogin, repoName, token);
  }

  private void configureCI(Path codePath, String orgId, String importId, Set<EnvVar> envVars) {
    var rawCIFile = bucketComponent.download(S3_POST_TRANSFORMATION_CI_DIR);
    var ghWorkflowDir = Path.of(codePath.toString(), LOCAL_CI_DIR);
    var envVarsAsString = getWorkflowEnvVarsString(envVars);
    try {
      var rawFilePath = Paths.get(rawCIFile.toURI());
      var fileContent = new String(Files.readAllBytes(rawFilePath));
      var replacedContent =
          fileContent
              .replace(ORG_ID_PLACEHOLDER, orgId)
              .replace(IMPORT_ID_PLACEHOLDER, importId)
              .replace(ENV_VARS_PLACEHOLDER, envVarsAsString);

      Files.write(rawFilePath, replacedContent.getBytes(), TRUNCATE_EXISTING);
      Files.createDirectories(ghWorkflowDir.getParent());
      Files.copy(rawFilePath, ghWorkflowDir, REPLACE_EXISTING);
    } catch (IOException e) {
      log.error("Error occurred during configuration of ci file: ", e);
      var errorLog = ApplicationImportLog.error(e.getMessage());
      stateService.updateState(
          importId, POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED, List.of(errorLog));
    }
  }

  private static String getWorkflowEnvVarsString(Set<EnvVar> envVars) {
    var envLines =
        envVars.stream()
            .filter(e -> e.testValue() != null && !e.testValue().isBlank())
            .map(envVar -> toWorkflowEnvVarString(envVar.name().toUpperCase()))
            .collect(Collectors.joining("\n      "));

    if (envLines.isBlank()) {
      return "";
    }

    return "env:\n      " + envLines;
  }

  public static String toWorkflowEnvVarString(String name) {
    return name + ": ${{ secrets." + toGhSecretName(name) + " }}";
  }

  public static String toGhSecretName(String envVarName) {
    return "TEST_" + envVarName;
  }

  private static GhSecret toGhSecret(EnvVar envVar) {
    return new GhSecret(toGhSecretName(envVar.name()), envVar.testValue());
  }

  private static void rmExistingWorkflows(Git git, Path projectDir) throws GitAPIException {
    var workflowsPath = projectDir.resolve(GITHUB_WORKFLOWS_DIR);

    if (Files.exists(workflowsPath) && Files.isDirectory(workflowsPath)) {
      log.info("Removed existing workflows directory: {}", workflowsPath);
      gitRm(git, List.of(Path.of(GITHUB_WORKFLOWS_DIR)));
    }
  }
}
