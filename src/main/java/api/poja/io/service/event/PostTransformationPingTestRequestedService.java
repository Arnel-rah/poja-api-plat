package api.poja.io.service.event;

import static api.poja.io.file.ApplicationImportFileType.ANALYSIS_RESULT;
import static api.poja.io.file.ExtendedBucketComponent.APP_LANG_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.FORMATTED_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_SUCCESSFUL;
import static api.poja.io.service.event.PostTransformationTestRunRequestedService.toGhSecretName;
import static api.poja.io.service.event.PostTransformationTestRunRequestedService.toWorkflowEnvVarString;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static api.poja.io.service.git.GitUtils.gitRm;
import static api.poja.io.service.git.GitUtils.pushAndCheckResult;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static api.poja.io.service.git.GitUtils.updateLocalUpstream;
import static java.util.stream.Collectors.toUnmodifiableSet;

import api.poja.io.endpoint.event.model.PostTransformationPingTestRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.EnvVar;
import api.poja.io.model.importer.analyzer.lang.AppLangAnalyzerData;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.GhSecret;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
public class PostTransformationPingTestRequestedService
    implements Consumer<PostTransformationPingTestRequested> {

  private final ExtendedBucketComponent bucketComponent;
  private final ApplicationImportService applicationImportService;
  private final ApplicationImportStateService stateService;
  private final GithubService githubService;
  private final AppInstallationService appInstallationService;
  private final ApplicationImportMapper applicationImportMapper;
  private final ObjectMapper objectMapper;

  public static final String S3_PING_IT_TEMPLATE = "poja-templates/PingIT.java.template";
  public static final String S3_PING_CI_TEMPLATE = "poja-templates/ping-test-ci.yml";
  private static final String PACKAGE_PLACEHOLDER = "<?package-name>";
  private static final String ORG_ID_PLACEHOLDER = "<?org-id>";
  private static final String IMPORT_ID_PLACEHOLDER = "<?import-id>";
  private static final String ENV_VARS_PLACEHOLDER = "<?test-env-vars>";
  private static final String GITHUB_WORKFLOWS_DIR = ".github/workflows/";
  private static final String PING_CI_FILENAME = "ping-test-ci.yml";
  private static final String MAIN_JAVA_SRC = "src/main/java/";
  private static final String JAVA_FILE_EXTENSION = ".java";

  @Override
  public void accept(PostTransformationPingTestRequested event) {
    var importId = event.getImportId();
    log.info("Ping test requested for ApplicationImport.id={}", importId);

    var importOpt = applicationImportService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found", importId);
      return;
    }
    var appImport = importOpt.get();

    var currentState = stateService.getStatesByImportId(importId).getFirst();
    if (shouldSkipPingTest(currentState.getProgressionStatus())) {
      log.info("Ping test already processed for import {}", importId);
      return;
    }

    log.info("Running ping test for import {}", appImport.getId());
    stateService.updateState(appImport.getId(), TEST_PING_ENDPOINT_IN_PROGRESS);

    try {
      var unknownApplication =
          applicationImportMapper.toUnknownApplication(appImport, FORMATTED_ZIPPED_CODE_SNAPSHOT);
      var appCode = unknownApplication.file();
      var appEnvVars = unknownApplication.envVars();

      var packageName = getJavaMainClassPackage(appImport.getOrgId(), appImport.getId());
      if (packageName.isEmpty()) {
        log.error("Could not detect root package in project");
        var errorLog = ApplicationImportLog.error("Could not detect root package in project");
        stateService.updateState(appImport.getId(), TEST_PING_ENDPOINT_FAILED, List.of(errorLog));
        return;
      }
      log.info("Detected root package: {}", packageName.get());

      // TODO: too many occurences inside event services, maybe try creating a dedicated method
      var appInstallation = appInstallationService.getById(appImport.getAppInstallationId());
      var ghToken =
          githubService.getInstallationToken(
              appInstallation.getGhId(), event.maxConsumerDuration());
      var credentials = new UsernamePasswordCredentialsProvider("x-access-token", ghToken);

      try (var git = Git.open(appCode)) {
        var appCodePath = appCode.toPath();
        var branchName = appImport.ghBranchNamePrefix() + "-ping-test";
        var doesBranchExist = doesBranchExist(credentials, git, branchName);

        checkoutBranch(doesBranchExist, git, branchName);
        rmExistingWorkflows(git, appCodePath);
        addPingITTest(appCodePath, packageName.get());
        configurePingCI(appCodePath, appImport.getOrgId(), appImport.getId(), appEnvVars);

        git.add().addFilepattern(".").call();
        unsignedCommitAsBot(
            git, "poja: ping test for import " + appImport.getId(), credentials, false);

        configureRepositorySecrets(
            appEnvVars,
            appInstallation.getOwnerGithubLogin(),
            appImport.getGithubRepositoryName(),
            ghToken);

        pushAndCheckResult(credentials, branchName, git);
        updateLocalUpstream(git, branchName);
      }
    } catch (Exception e) {
      log.error("Error during ping test setup for import {}", appImport.getId(), e);
      var errorLog = ApplicationImportLog.error(e.getMessage());
      stateService.updateState(appImport.getId(), TEST_PING_ENDPOINT_FAILED, List.of(errorLog));
    }
  }

  private Optional<String> getJavaMainClassPackage(String orgId, String importId) {
    try {
      var langAnalysisFile =
          applicationImportService.downloadAppImportFile(
              orgId, importId, ANALYSIS_RESULT, APP_LANG_ANALYSIS_RESULT_FILENAME);

      var data = objectMapper.readValue(langAnalysisFile, AppLangAnalyzerData.class);
      var mainClassPath = data.mainClassPath().toString();

      var packagePath =
          mainClassPath
              .replace(File.separatorChar, '/')
              .replace(MAIN_JAVA_SRC, "")
              .replace(JAVA_FILE_EXTENSION, "")
              .replace('/', '.');

      if (!packagePath.contains(".")) {
        log.info("No package found in: {}", packagePath);
        return Optional.empty();
      }

      return Optional.of(packagePath.substring(0, packagePath.lastIndexOf(".")));

    } catch (IOException e) {
      log.error("Error extracting package", e);
      return Optional.empty();
    }
  }

  private void addPingITTest(Path projectRoot, String packageName) throws IOException {
    var pingITTemplate = bucketComponent.download(S3_PING_IT_TEMPLATE);
    var templateContent = Files.readString(pingITTemplate.toPath());

    var finalContent = templateContent.replace(PACKAGE_PLACEHOLDER, packageName);

    var packagePath = packageName.replace(".", File.separator);
    var testDir = projectRoot.resolve("src/test/java").resolve(packagePath);
    Files.createDirectories(testDir);

    var pingITFile = testDir.resolve("PingIT.java");
    Files.writeString(pingITFile, finalContent);

    log.info("PingIT.java created at {}", pingITFile);
  }

  private void configurePingCI(Path projectRoot, String orgId, String importId, Set<EnvVar> envVars)
      throws IOException {
    var ciTemplate = bucketComponent.download(S3_PING_CI_TEMPLATE);
    var templateContent = Files.readString(ciTemplate.toPath());

    var envVarsString = getWorkflowEnvVarsString(envVars);
    var finalContent =
        templateContent
            .replace(ORG_ID_PLACEHOLDER, orgId)
            .replace(IMPORT_ID_PLACEHOLDER, importId)
            .replace(ENV_VARS_PLACEHOLDER, envVarsString);

    var workflowsDir = projectRoot.resolve(GITHUB_WORKFLOWS_DIR);
    Files.createDirectories(workflowsDir);

    var ciFile = workflowsDir.resolve(PING_CI_FILENAME);
    Files.writeString(ciFile, finalContent);

    log.info("Ping CI workflow created at {}", ciFile);
  }

  private static String getWorkflowEnvVarsString(Set<EnvVar> envVars) {
    var envLines =
        envVars.stream()
            .filter(e -> e.value() != null && !e.value().isBlank())
            .map(envVar -> toWorkflowEnvVarString(envVar.name().toUpperCase()))
            .collect(Collectors.joining("\n      "));

    if (envLines.isBlank()) {
      return "";
    }

    return "env:\n      " + envLines;
  }

  private static GhSecret toGhSecret(EnvVar envVar) {
    return new GhSecret(toGhSecretName(envVar.name()), envVar.value());
  }

  private void configureRepositorySecrets(
      Set<EnvVar> envVars, String ownerLogin, String repoName, String token) {
    var repoSecrets =
        envVars.stream()
            .filter(e -> e.value() != null && !e.value().isBlank())
            .map(PostTransformationPingTestRequestedService::toGhSecret)
            .collect(toUnmodifiableSet());
    githubService.crupdateSecrets(repoSecrets, ownerLogin, repoName, token);
  }

  private static void rmExistingWorkflows(Git git, Path projectDir) throws GitAPIException {
    var workflowsPath = projectDir.resolve(GITHUB_WORKFLOWS_DIR);

    if (Files.exists(workflowsPath) && Files.isDirectory(workflowsPath)) {
      log.info("Removed existing workflows directory: {}", workflowsPath);
      gitRm(git, List.of(Path.of(GITHUB_WORKFLOWS_DIR)));
    }
  }

  private static boolean shouldSkipPingTest(ApplicationImportStateStatus status) {
    return status == TEST_PING_ENDPOINT_IN_PROGRESS || status == TEST_PING_ENDPOINT_SUCCESSFUL;
  }
}
