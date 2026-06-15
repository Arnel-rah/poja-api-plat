package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.FORMATTED_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.ExtendedBucketComponent.INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.TempFile.createTempDir;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_SUCCESSFUL;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static api.poja.io.service.git.GitUtils.pushAndCheckResult;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static api.poja.io.service.git.GitUtils.updateLocalUpstream;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.CodeFormattingRequested;
import api.poja.io.endpoint.event.model.PostTransformationTestRunRequested;
import api.poja.io.model.importer.format.CodeFormatter;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class CodeFormattingRequestedService implements Consumer<CodeFormattingRequested> {
  private final CodeFormatter formatter;
  private final ApplicationImportService importService;
  private final ApplicationImportStateService stateService;
  private final GithubService githubService;
  private final AppInstallationService appInstallationService;
  private final EventProducer<PostTransformationTestRunRequested> eventProducer;

  @Override
  public void accept(CodeFormattingRequested event) {
    var orgId = event.getOrgId();
    var importId = event.getImportId();

    log.info("Code formatting requested for ApplicationImport.id={}", importId);

    var importOpt = importService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found. Skipping code formatting", importId);
      return;
    }
    var applicationImport = importOpt.get();

    stateService.updateState(importId, CODE_FORMATTING_IN_PROGRESS);

    try {
      var tempDir = createTempDir("code-formatting-" + importId);
      importService.downloadAndUnzipCodeSnapshot(
          orgId, importId, INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT, tempDir);

      try (var git = Git.open(tempDir.toFile())) {
        var credentials = getCredentialsProvider(applicationImport, event.maxConsumerDuration());
        var branchName = applicationImport.ghMainBranchName();
        var defaultBranch = applicationImport.getGithubRepositoryDefaultBranch();
        var branchExists = doesBranchExist(credentials, git, branchName);

        checkoutBranch(branchExists, git, branchName, defaultBranch);

        var result = formatter.apply(tempDir);

        if (result.failed()) {
          log.error("Code formatting failed for ApplicationImport.id={}", importId);
          stateService.updateState(importId, CODE_FORMATTING_FAILED, result.logs());
          return;
        }

        git.add().addFilepattern(".").call();
        var status = git.status().call();
        var formattedFiles = status.getChanged();

        unsignedCommitAsBot(
            git,
            applicationImport.ghCommitMsgPrefix() + " code formatting",
            credentials,
            status.isClean());

        pushAndCheckResult(credentials, branchName, git);
        updateLocalUpstream(git, branchName);

        importService.uploadZippedCodeSnapshot(
            orgId, importId, FORMATTED_ZIPPED_CODE_SNAPSHOT, tempDir);

        if (!formattedFiles.isEmpty()) {
          var logMessage = new StringBuilder();
          logMessage.append("Successfully formatted the following files: \n");
          formattedFiles.forEach(f -> logMessage.append(f).append("\n"));
          stateService.updateState(
              importId,
              CODE_FORMATTING_SUCCESSFUL,
              List.of(ApplicationImportLog.info(logMessage.toString())));
        } else {
          stateService.updateState(importId, CODE_FORMATTING_SUCCESSFUL);
        }

        firePostTransformationTestRun(importId);
      }
    } catch (Exception e) {
      var errorMessage = "Unable to format code for ApplicationImport.id=" + importId;
      log.error(errorMessage, e);
      stateService.updateState(
          importId, CODE_FORMATTING_FAILED, List.of(ApplicationImportLog.error(errorMessage)));
    }
  }

  private void firePostTransformationTestRun(String importId) {
    var event = new PostTransformationTestRunRequested(importId);
    eventProducer.accept(List.of(event));
  }

  private UsernamePasswordCredentialsProvider getCredentialsProvider(
      ApplicationImport applicationImport, Duration duration) {
    var appInstallation = appInstallationService.getById(applicationImport.getAppInstallationId());
    var ghToken = githubService.getInstallationToken(appInstallation.getGhId(), duration);
    return new UsernamePasswordCredentialsProvider("x-access-token", ghToken);
  }
}
