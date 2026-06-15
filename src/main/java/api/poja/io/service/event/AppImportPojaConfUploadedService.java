package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.ExtendedBucketComponent.INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.TempFile.createTempDir;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_GENERATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATION_IN_PROGRESS;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static api.poja.io.service.git.GitUtils.gitRm;
import static api.poja.io.service.git.GitUtils.pushAndCheckResult;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static api.poja.io.service.git.GitUtils.updateLocalUpstream;
import static api.poja.io.service.pojaConfHandler.AbstractPojaConfUploadedHandler.addExecutePermissionToFormat;
import static api.poja.io.service.pojaConfHandler.AbstractPojaConfUploadedHandler.rmPojaFilesAndAddChanges;
import static java.util.stream.Collectors.toUnmodifiableSet;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportPojaConfUploaded;
import api.poja.io.endpoint.event.model.CodeFormattingRequested;
import api.poja.io.file.FileUnzipper;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.api.pojaSam.PojaSamApi;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.github.GithubService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AppImportPojaConfUploadedService implements Consumer<AppImportPojaConfUploaded> {
  private static final Set<String> POJA_FILES_TO_DELETE =
      Set.of(".github/workflows/cd-compute.yml", ".github/workflows/ci.yml");
  private static final Set<String> FILES_ALLOWED_FOR_CHANGES =
      Set.of(
          ".shell/checkHealth.sh",
          ".shell/publish_gen_to_maven_local.bat",
          ".shell/publish_gen_to_maven_local.sh",
          "build.gradle",
          "settings.gradle",
          ".gitignore",
          "gradlew",
          "gradlew.bat",
          "format.sh");
  private static final Set<String> JAVA_FILES_ALLOWED_FOR_CHANGES =
      Set.of("PingController.java", "LambdaHandler.java", "EmailConf.java", "FacadeIT.java");
  private static final Set<String> FILES_ALLOWED_FOR_DELETION =
      Set.of(
          "cf-stacks/compute-permission-stack.yml",
          "cf-stacks/domain-name-stack.yml",
          "cf-stacks/event-stack.yml",
          "cf-stacks/scheduler-stack.yml",
          "cf-stacks/storage-bucket-stack.yml",
          "poja-custom-java-env-vars.txt",
          "poja-custom-java-test-env-vars.txt",
          "poja-custom-java-gradle-test-args.txt",
          "poja-custom-java-repositories.txt",
          "poja-custom-java-deps.txt",
          "template.yml",
          "poja.yml",
          ".github/workflows/cd-compute.yml",
          ".github/workflows/ci.yml",
          ".github/workflows/cd-scheduler.yml",
          ".github/workflows/cd-compute-permission.yml",
          ".github/workflows/cd-event.yml",
          ".github/workflows/cd-storage-bucket.yml",
          ".github/workflows/cd-storage-database.yml",
          ".github/workflows/health-check-email.yml",
          ".github/workflows/health-check-infra.yml",
          ".github/workflows/health-check-poja.yml");

  private final ApplicationImportService importService;
  private final ApplicationImportStateService importStateService;
  private final AppInstallationService appInstallationService;
  private final GithubService githubService;
  private final PojaConfFileMapper pojaConfMapper;
  private final PojaSamApi pojaSamApi;
  private final FileUnzipper unzipper;
  private final EventProducer<CodeFormattingRequested> eventProducer;

  @Override
  public void accept(AppImportPojaConfUploaded event) {
    var orgId = event.getOrgId();
    var importId = event.getImportId();

    var importOpt = importService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found. Skipping code generation", importId);
      return;
    }
    var applicationImport = importOpt.get();

    var confFile = importService.downloadAppImportPojaConf(orgId, importId);
    var pojaConf = pojaConfMapper.readAsDomain(confFile);

    // TODO: to prevent retries if the initial updateState(IN_PROGRESS) fails, block further
    // attempts
    log.info("Generating code for appImport: {}", importId);
    importStateService.updateState(importId, CODE_GENERATION_IN_PROGRESS);

    var unzippedCode = createTempDir("unzipped-code");
    var generatedCode = pojaSamApi.genCode(pojaConf.getVersion(), confFile);

    importService.downloadAndUnzipCodeSnapshot(
        orgId, importId, CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT, unzippedCode);

    try (var git = Git.open(unzippedCode.toFile())) {
      log.info("Integrating generated code for appImport: {}", importId);
      importStateService.updateState(importId, GENERATED_CODE_INTEGRATION_IN_PROGRESS);

      var importBranchName = applicationImport.ghMainBranchName();
      var gitCredentials = configureGitCredentials(applicationImport, event.maxConsumerDuration());
      var doesBranchExist = doesBranchExist(gitCredentials, git, importBranchName);

      checkoutBranch(doesBranchExist, git, importBranchName);
      unzip(generatedCode, unzippedCode);

      var hasUncommittedChanges = configureAndAddChanges(unzippedCode, git);
      var gitStatus = git.status().call();

      var removedFiles = gitStatus.getRemoved();
      var forbiddenFileDeletions = getForbiddenFileDeletions(removedFiles);

      if (!forbiddenFileDeletions.isEmpty()) {
        log.error("Unexpected file deletion during code integration for appImport: {}", importId);

        var errorMessageBuilder = new StringBuilder();
        errorMessageBuilder.append("Unexpected deletion of following files: \n");
        forbiddenFileDeletions.forEach(f -> errorMessageBuilder.append(f).append("\n"));
        saveStateWithErrors(importId, errorMessageBuilder.toString());
        return;
      }

      // Using getChanged() instead of getModified() because changes have been previously added to
      // index
      var changedFiles = gitStatus.getChanged();
      var forbiddenFileChanges = getForbiddenFileChanges(changedFiles);

      if (!forbiddenFileChanges.isEmpty()) {
        log.error("Unexpected file changes during code integration for appImport: {}", importId);

        for (var entry : git.diff().setCached(true).call()) {
          if (forbiddenFileChanges.contains(entry.getNewPath())) {
            var diffOutput = new ByteArrayOutputStream();
            try (var diffFormatter = new DiffFormatter(diffOutput)) {
              diffFormatter.setRepository(git.getRepository());
              diffFormatter.format(entry);
              log.error(
                  "Diff for forbidden file {} during appImport {}:\n{}",
                  entry.getNewPath(),
                  importId,
                  diffOutput);
            }
          }
        }

        var errorMessageBuilder = new StringBuilder();
        errorMessageBuilder.append("Unexpected change of following files: \n");
        forbiddenFileChanges.forEach(f -> errorMessageBuilder.append(f).append("\n"));
        saveStateWithErrors(importId, errorMessageBuilder.toString());
        return;
      }

      unsignedCommitAsBot(
          git,
          applicationImport.ghCommitMsgPrefix() + " generated code integration",
          gitCredentials,
          hasUncommittedChanges);
      pushAndCheckResult(gitCredentials, importBranchName, git);
      updateLocalUpstream(git, importBranchName);

      importService.uploadZippedCodeSnapshot(
          orgId, importId, INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT, unzippedCode);
      importStateService.updateState(importId, GENERATED_CODE_INTEGRATED);
      fireCoreFormatEvent(orgId, importId);
    } catch (IOException | GitAPIException | ApiException e) {
      log.error("Generated code could be integrated for appImport: {}", importId, e);
      saveStateWithErrors(
          importId, "An error occurred during the integration of the generated code");
    }
  }

  private void saveStateWithErrors(String importId, String errorMessage) {
    var errorLog = ApplicationImportLog.error(errorMessage);
    importStateService.updateState(importId, GENERATED_CODE_INTEGRATION_FAILED, List.of(errorLog));
  }

  // TODO: refactor into a reusable method to avoid duplicates
  private UsernamePasswordCredentialsProvider configureGitCredentials(
      ApplicationImport applicationImport, Duration tokenDuration) {
    var appInstallation = appInstallationService.getById(applicationImport.getAppInstallationId());
    var ghToken = githubService.getInstallationToken(appInstallation.getGhId(), tokenDuration);
    return new UsernamePasswordCredentialsProvider("x-access-token", ghToken);
  }

  private boolean configureAndAddChanges(Path unzippedCode, Git git) throws GitAPIException {
    addExecutePermissionToFormat(unzippedCode);
    rmPojaFilesAndAddChanges(git, hasApiSpec(unzippedCode));
    gitRm(git, POJA_FILES_TO_DELETE.stream().map(Path::of).toList());
    var status = git.status().call();
    return !status.isClean();
  }

  private void unzip(File toUnzip, Path destination) throws IOException {
    unzipper.apply(new ZipFile(toUnzip), destination);
  }

  private static boolean hasApiSpec(Path directory) {
    return directory.resolve("doc/api.yml").toFile().exists();
  }

  private static Set<String> getForbiddenFileChanges(Set<String> modifiedFiles) {
    return modifiedFiles.stream()
        .filter(f -> !FILES_ALLOWED_FOR_CHANGES.contains(f))
        .filter(f -> JAVA_FILES_ALLOWED_FOR_CHANGES.stream().noneMatch(f::contains))
        .collect(toUnmodifiableSet());
  }

  private static Set<String> getForbiddenFileDeletions(Set<String> deletedFiles) {
    return deletedFiles.stream()
        .filter(f -> !FILES_ALLOWED_FOR_DELETION.contains(f))
        .collect(toUnmodifiableSet());
  }

  private void fireCoreFormatEvent(String orgId, String importId) {
    var event = new CodeFormattingRequested(orgId, importId);
    eventProducer.accept(List.of(event));
  }
}
