package api.poja.io.service.pojaConfHandler;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_PUSH_FAILED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_PUSH_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_PUSH_SUCCESS;
import static api.poja.io.file.ExtendedBucketComponent.getOrgBucketKey;
import static api.poja.io.file.FileType.DEPLOYMENT_FILE;
import static api.poja.io.model.PojaVersion.POJA_8;
import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.cloneRepository;
import static api.poja.io.service.git.GitUtils.configureGitRepositoryGpg;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static api.poja.io.service.git.GitUtils.formatShortBranchName;
import static api.poja.io.service.git.GitUtils.gitRm;
import static api.poja.io.service.git.GitUtils.pushAndCheckResult;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static java.lang.Boolean.FALSE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.event.model.PojaConfUploaded;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileFinder;
import api.poja.io.file.FileUnzipper;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.AppEnvironmentDeploymentService;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvDeploymentConfService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.api.pojaSam.PojaSamApi;
import api.poja.io.service.appEnvConfigurer.AppEnvConfigurerService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.github.model.GhWorkflowRunRequestBody;
import api.poja.io.service.workflows.DeploymentStateService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

@Slf4j
@AllArgsConstructor
public abstract sealed class AbstractPojaConfUploadedHandler implements PojaConfUploadedHandler
    permits PojaConf1UploadedHandler,
        PojaConf2UploadedHandler,
        PojaConf3UploadedHandler,
        PojaConf4UploadedHandler,
        PojaConf5UploadedHandler,
        PojaConf6UploadedHandler,
        PojaConf7UploadedHandler,
        PojaConf8UploadedHandler,
        PojaConf9UploadedHandler {
  protected final PojaDeploymentCloudEnv pojaDeploymentCloudEnv;

  protected final AppEnvConfigurerService appEnvConfigurerService;
  protected final FileUnzipper unzipper;
  protected final DeploymentStateService deploymentStateService;
  protected final ApplicationService appService;
  protected final EnvironmentService envService;
  protected final AppInstallationService appInstallService;
  protected final ExtendedBucketComponent bucketComponent;
  protected final PojaSamApi pojaSamApi;
  protected final GithubService githubService;
  protected final EnvDeploymentConfService envDeploymentConfService;
  protected final AppEnvironmentDeploymentService appEnvironmentDeploymentService;
  protected final FileFinder finder;
  protected final PojaConfFileMapper pojaConfFileMapper;

  protected static final String CD_COMPUTE_WORKFLOW_ID = "cd-compute.yml";
  protected static final String ENV_PLACEHODLER = "<?env>";
  protected static final String MAIN_JAVA_SOURCE_PATH = "src/main/java/";
  protected static final String GITHUB_WORKFLOWS_CD_SCHEDULER_YML =
      ".github/workflows/cd-scheduler.yml";
  protected static final String BUILD_TEMPLATE_FILENAME_YML = "template.yml";
  protected static final String CF_STACKS_CD_COMPUTE_PERMISSION_YML_PATH =
      "cf-stacks/compute-permission-stack.yml";
  protected static final String CF_STACKS_EVENT_STACK_YML_PATH = "cf-stacks/event-stack.yml";
  protected static final String CF_STACKS_STORAGE_BUCKET_STACK_YML_PATH =
      "cf-stacks/storage-bucket-stack.yml";
  protected static final String CF_STACKS_STORAGE_SQLITE_STACK_YML_PATH =
      "cf-stacks/storage-efs-stack.yml";
  protected static final String CD_SCHEDULER_KEY_STACK_YML_PATH = "cf-stacks/scheduler-stack.yml";
  private static final List<String> POJA_FILES =
      List.of(
          "cf-stacks/",
          "poja-custom-java-env-vars.txt",
          "poja-custom-java-test-env-vars.txt",
          "poja-custom-java-gradle-test-args.txt",
          "poja-custom-java-repositories.txt",
          "poja-custom-java-deps.txt",
          BUILD_TEMPLATE_FILENAME_YML,
          "poja.yml",
          ".github/workflows/cd-scheduler.yml",
          ".github/workflows/cd-compute-permission.yml",
          ".github/workflows/cd-event.yml",
          ".github/workflows/cd-storage-bucket.yml",
          ".github/workflows/cd-storage-database.yml",
          ".github/workflows/health-check-email.yml",
          ".github/workflows/health-check-infra.yml",
          ".github/workflows/health-check-poja.yml");

  protected abstract PojaVersion supportedPojaVersion();

  @Override
  public boolean supports(PojaVersion pojaVersion) {
    return supportedPojaVersion().equals(pojaVersion);
  }

  protected void onBeforeClone(
      PojaConf pojaConf,
      Application app,
      Environment env,
      String appInstallationToken,
      AppInstallation appInstallation) {
    log.info("PojaConfUploadedHandler:onBeforeClone");
  }

  protected void onAfterClone(
      Git git,
      CredentialsProvider ghCredentialsProvider,
      Application app,
      Environment env,
      File toUnzip,
      Path cloneDirPath,
      String appEnvDeploymentId,
      PojaConf pojaConf) {
    log.info("PojaConfUploadedHandler:onAfterClone");
    configureCdCompute(cloneDirPath, pojaConf);
    addExecutePermissionToFormat(cloneDirPath);
  }

  @Override
  public final void accept(PojaConfUploaded pojaConfUploaded) {
    PojaVersion pojaVersion = pojaConfUploaded.getPojaVersion();
    if (!this.supports(pojaVersion)) {
      log.error(
          "expected Poja version {} does not match poja version {}",
          this.supportedPojaVersion(),
          pojaVersion);
      return;
    }
    var pojaConf =
        appEnvConfigurerService.readConfigAsDomain(
            pojaConfUploaded.getOrgId(),
            pojaConfUploaded.getAppId(),
            pojaConfUploaded.getEnvironmentId(),
            pojaConfUploaded.getFilename());

    String appId = pojaConfUploaded.getAppId();
    String envId = pojaConfUploaded.getEnvironmentId();
    var savedAppEnvDeploymentId = pojaConfUploaded.getAppEnvDeplId();
    var app = appService.getById(appId);
    var env = envService.getById(envId);
    var appInstallation = appInstallService.getById(app.getInstallationId());
    var appInstallationToken =
        getInstallationToken(appInstallation, pojaConfUploaded.maxConsumerDuration());
    var cloneDirPath = createTempDir("github_clone");
    CredentialsProvider ghCredentialsProvider =
        new UsernamePasswordCredentialsProvider("x-access-token", appInstallationToken);
    var generatedCode =
        generateCodeFromPojaConf(pojaConf, pojaConfUploaded.getOrgId(), appId, envId);

    deploymentStateService.save(app.getId(), savedAppEnvDeploymentId, CODE_PUSH_IN_PROGRESS);

    onBeforeClone(pojaConf, app, env, appInstallationToken, appInstallation);

    // todo: refactor exception handling
    try {
      var git =
          clone(
              ghCredentialsProvider,
              app,
              env,
              generatedCode,
              cloneDirPath,
              pojaConfUploaded.getSourceBranch());

      onAfterClone(
          git,
          ghCredentialsProvider,
          app,
          env,
          generatedCode,
          cloneDirPath,
          savedAppEnvDeploymentId,
          pojaConf);

      var branchName = formatShortBranchName(env);
      boolean doesBranchExist = doesBranchExist(ghCredentialsProvider, git, branchName);

      boolean hasRepoBeenModified =
          cleanAndCommitChanges(
              ghCredentialsProvider,
              cloneDirPath,
              app,
              savedAppEnvDeploymentId,
              git,
              branchName,
              doesBranchExist);

      var savedConfId =
          uploadAndSaveDeploymentFiles(generatedCode, pojaConfUploaded, savedAppEnvDeploymentId);

      if (!hasRepoBeenModified) {
        log.info(
            "no changes to push to git repo, but still triggering cd-compute workflow for"
                + " deployment id: {}, with config id: {}",
            savedAppEnvDeploymentId,
            savedConfId);
        githubService.runWorkflowDispatch(
            appInstallation.getOwnerGithubLogin(),
            app.getGithubRepositoryName(),
            appInstallationToken,
            CD_COMPUTE_WORKFLOW_ID,
            new GhWorkflowRunRequestBody(
                formatShortBranchName(env),
                Map.of("env_conf_id", savedConfId, "deployment_id", savedAppEnvDeploymentId)));
        /*
         Even if there are no changes to push, we still mark the deployment code push as successful.
         Otherwise, EnvironmentBuildService#updateGithubWorkflowState may trigger another
         deployment when the workflow run (triggered above) is processed, treating this one
         as a previous deployment, ignoring it, and creating a new one.

         A dedicated state would be a cleaner solution.
        */
        deploymentStateService.save(app.getId(), savedAppEnvDeploymentId, CODE_PUSH_SUCCESS);
      }

      log.info(
          "Successfully processed PojaConf upload for deployment id: {}", savedAppEnvDeploymentId);
    } catch (Exception e) {
      deploymentStateService.save(app.getId(), savedAppEnvDeploymentId, CODE_PUSH_FAILED);
      log.error(
          "Failed to process PojaConf upload for deployment id: {}", savedAppEnvDeploymentId, e);
    }
  }

  /**
   * Clones application ghRepo code to cloneDirPath
   *
   * @param ghCredentialsProvider
   * @param app
   * @param env
   * @param toUnzip
   * @param sourceBranch
   * @param cloneDirPath
   */
  private Git clone(
      CredentialsProvider ghCredentialsProvider,
      Application app,
      Environment env,
      File toUnzip,
      Path cloneDirPath,
      @Nullable String sourceBranch)
      throws GitAPIException {
    String branchName = formatShortBranchName(env);
    try (Git git =
        cloneRepository(ghCredentialsProvider, app.getGithubRepositoryUrl(), cloneDirPath)) {
      configureGitRepositoryGpg(git);

      var branchExists = doesBranchExist(ghCredentialsProvider, git, branchName);
      var sourceBranchExists =
          sourceBranch != null && doesBranchExist(ghCredentialsProvider, git, sourceBranch);
      checkoutBranch(branchExists, git, branchName, sourceBranchExists ? sourceBranch : null);

      log.info("successfully cloned in {}", cloneDirPath.toAbsolutePath());
      unzip(asZipFile(toUnzip), cloneDirPath);

      return git;
    }
  }

  private boolean cleanAndCommitChanges(
      CredentialsProvider ghCredentialsProvider,
      Path cloneDirPath,
      Application app,
      String appEnvDeploymentId,
      Git git,
      String branchName,
      boolean doesBranchExist)
      throws GitAPIException {
    var hasUncommittedChanges = rmPojaFilesAndAddChanges(git, hasApiSpec(cloneDirPath));
    if (hasUncommittedChanges) {
      boolean isCommitEmpty = false;
      unsignedCommitAsBot(
          git, "poja: deployment ID: " + appEnvDeploymentId, ghCredentialsProvider, isCommitEmpty);
      pushAndCheckResult(ghCredentialsProvider, branchName, git);
      deploymentStateService.save(app.getId(), appEnvDeploymentId, CODE_PUSH_SUCCESS);
      return true;
    }
    if (!doesBranchExist) {
      // empty commit because branch creation counts as a repo change, and it helps us create a new
      // deployment for the newly created branch
      boolean isCommitEmpty = true;
      unsignedCommitAsBot(
          git, "poja: deployment ID: " + appEnvDeploymentId, ghCredentialsProvider, isCommitEmpty);
      pushAndCheckResult(ghCredentialsProvider, branchName, git);
      deploymentStateService.save(app.getId(), appEnvDeploymentId, CODE_PUSH_SUCCESS);
      return true;
    }
    return false;
  }

  public abstract void configureCdCompute(Path clonedDirPath, PojaConf pojaConf);

  public static void addExecutePermissionToFormat(Path cloneDirPath) {
    cloneDirPath.resolve("format.sh").toFile().setExecutable(true);
  }

  /**
   * uploads deployment files and saves config
   *
   * @return the saved config
   */
  private String uploadAndSaveDeploymentFiles(
      File toUnzip, PojaConfUploaded pojaConfUploaded, String appEnvDeploymentId) {
    var unzippedCode = createTempDir("unzipped");
    unzip(asZipFile(toUnzip), unzippedCode);
    var tempDirPath = createTempDir("deployment_files");
    EnvDeploymentConf savedConf =
        envDeploymentConfService.save(
            getEnvDeploymentConf(pojaConfUploaded, unzippedCode, tempDirPath));
    String confId = savedConf.getId();
    appEnvironmentDeploymentService.updateEnvDeploymentConf(appEnvDeploymentId, confId);
    return confId;
  }

  private EnvDeploymentConf getEnvDeploymentConf(
      PojaConfUploaded pojaConfUploaded, Path unzippedCode, Path tempDirPath) {
    UUID random = randomUUID();
    var optionalBuildTemplateFilename =
        copyToIfExists(
            unzippedCode, BUILD_TEMPLATE_FILENAME_YML, tempDirPath, "template" + random + ".yml");
    var optionalComputePermissionStackFilename =
        copyToIfExists(
            unzippedCode,
            CF_STACKS_CD_COMPUTE_PERMISSION_YML_PATH,
            tempDirPath,
            "compute-permission" + random + ".yml");
    var optionalEventStackFilename =
        copyToIfExists(
            unzippedCode,
            CF_STACKS_EVENT_STACK_YML_PATH,
            tempDirPath,
            "event-stack" + random + ".yml");
    var optionalStorageBucketStackFilename =
        copyToIfExists(
            unzippedCode,
            CF_STACKS_STORAGE_BUCKET_STACK_YML_PATH,
            tempDirPath,
            "storage-bucket-stack" + random + ".yml");
    var optionalStorageSqliteStackFilename =
        copyToIfExists(
            unzippedCode,
            CF_STACKS_STORAGE_SQLITE_STACK_YML_PATH,
            tempDirPath,
            "storage-efs-stack" + random + ".yml");
    var optionalSchedulerStackFilename =
        copyToIfExists(
            unzippedCode,
            CD_SCHEDULER_KEY_STACK_YML_PATH,
            tempDirPath,
            "scheduler" + random + ".yml");

    var environmentId = pojaConfUploaded.getEnvironmentId();
    bucketComponent.upload(
        tempDirPath.toFile(),
        getOrgBucketKey(
            pojaConfUploaded.getOrgId(),
            pojaConfUploaded.getAppId(),
            environmentId,
            DEPLOYMENT_FILE));
    return EnvDeploymentConf.builder()
        .id(pojaConfUploaded.getEnvDeplConfId())
        .envId(environmentId)
        .computePermissionStackFileKey(optionalComputePermissionStackFilename.orElseThrow())
        .storageBucketStackFileKey(optionalStorageBucketStackFilename.orElse(null))
        .eventStackFileKey(optionalEventStackFilename.orElse(null))
        .storageDatabaseSqliteStackFileKey(optionalStorageSqliteStackFilename.orElse(null))
        .eventSchedulerStackFileKey(optionalSchedulerStackFilename.orElse(null))
        .buildTemplateFile(optionalBuildTemplateFilename.orElseThrow())
        .creationDatetime(Instant.now())
        .pojaConfFileKey(pojaConfUploaded.getFilename())
        .build();
  }

  @SneakyThrows
  protected void rmIntegrationGeneratedFiles(
      Git git, Path cloneDirPath, PojaConf1.Integration integration) {
    if (FALSE.equals(integration.withCodeql())) {
      gitRm(git, List.of(Path.of(".github/workflows/codeql.yml")));
    }
    if (FALSE.equals(integration.withSentry())) {
      var sentryConfs = finder.apply(cloneDirPath, Set.of("SentryConf.java"));
      if (!sentryConfs.isEmpty()) {
        gitRm(git, List.of(cloneDirPath.relativize(sentryConfs.getFirst())));
      }
    }
  }

  protected Path getPackageSourcePath(Path cloneDirPath, String packageName) {
    return cloneDirPath.resolve(
        Path.of(MAIN_JAVA_SOURCE_PATH + packageName.replaceAll("\\.", "/")));
  }

  /**
   * adds changes to git
   *
   * @param git
   * @param hasApiSpec
   * @return true if changes were made, false otherwise, a change is a modified(added, deleted, or
   *     altered) file other than those we don't track
   */
  public static boolean rmPojaFilesAndAddChanges(Git git, boolean hasApiSpec) {
    try {
      if (hasApiSpec) {
        git.checkout().addPath("doc/api.yml").call();
      }
      git.add().addFilepattern(".").call();
      gitRm(git, POJA_FILES.stream().map(Path::of).toList());
      Status status = git.status().call();
      return !status.isClean();
    } catch (GitAPIException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param pojaConf
   * @param orgId
   * @param appId
   * @param environmentId
   * @return zip file but we do not use ZipFile type so we can reuse this
   */
  private File generateCodeFromPojaConf(
      PojaConf pojaConf, String orgId, String appId, String environmentId) {
    log.info("Read pojaConfFile for orgId: {}, app: {}, env: {}", orgId, appId, environmentId);
    var updatedPojaConf = applyCloudOverridesForGeneration(pojaConf);
    var pojaConfFile = pojaConfFileMapper.writeToTempFile(updatedPojaConf);
    return pojaSamApi.genCode(pojaConf.getVersion(), pojaConfFile);
  }

  private static PojaConf applyCloudOverridesForGeneration(PojaConf pojaConf) {
    if (POJA_8.equals(pojaConf.getVersion()) && pojaConf instanceof PojaConf8 pojaConf8) {
      var scheduledTasks = pojaConf8.scheduledTasks();
      if (scheduledTasks == null || scheduledTasks.isEmpty()) return pojaConf;
      var cloudTasks = scheduledTasks.stream().map(PojaConf8.ScheduledTask::useCloudName).toList();
      return pojaConf8.toBuilder().scheduledTasks(cloudTasks).build();
    }

    if (POJA_9.equals(pojaConf.getVersion()) && pojaConf instanceof PojaConf9 pojaConf9) {
      var scheduledTasks = pojaConf9.scheduledTasks();
      if (scheduledTasks == null || scheduledTasks.isEmpty()) return pojaConf;
      var cloudTasks = scheduledTasks.stream().map(PojaConf9.ScheduledTask::useCloudName).toList();
      return pojaConf9.toBuilder().scheduledTasks(cloudTasks).build();
    }

    return pojaConf;
  }

  @SneakyThrows
  private static Path createTempDir(String prefix) {
    return Files.createTempDirectory(prefix);
  }

  private String getInstallationToken(AppInstallation appInstallation, Duration tokenDuration) {
    return githubService.getInstallationToken(appInstallation.getGhId(), tokenDuration);
  }

  private static Optional<String> copyToIfExists(
      Path source, String originalFilePath, Path destination, String newFilename) {
    var originalFile = source.resolve(originalFilePath);
    var copySucceeded = copyFile(originalFile, destination, newFilename);
    if (!copySucceeded) {
      return empty();
    }
    return Optional.of(newFilename);
  }

  /**
   * copies a file from source to target with the new filename. if copy fails, it throws
   * RuntimeException if file does not exist, it returns false if file exists, it returns true the
   * boolean return value is used to handle non-existing file copies e.g: storage-bucket-stack does
   * not exist if with_file_storage is false in the code_gen conf, hence the need to check whether
   * the file exists or not on copy result
   */
  private static boolean copyFile(Path source, Path target, String newFilename) {
    if (source.toFile().exists()) {
      Path newFilenameResolved = target.resolve(newFilename);
      log.info("Copying {} to {}", source, newFilenameResolved);
      try {
        Files.move(source, newFilenameResolved, REPLACE_EXISTING);
      } catch (IOException e) {
        log.info("failed to copy");
        throw new RuntimeException(e);
      }
      return true;
    }
    log.info("file does not exist {}", source.toAbsolutePath());
    return false;
  }

  @SneakyThrows
  private static ZipFile asZipFile(File toUnzip) {
    return new ZipFile(toUnzip);
  }

  private void unzip(ZipFile downloaded, Path destination) {
    unzipper.apply(downloaded, destination);
  }

  protected boolean hasApiSpec(Path directory) {
    return directory.resolve("doc/api.yml").toFile().exists();
  }
}
