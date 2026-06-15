package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.BUILD_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.GRADLE_FILES;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.MAVEN_FILES;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.POM_XML;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.SETTINGS_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.MAVEN;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.*;
import static api.poja.io.service.git.GitUtils.checkoutBranch;
import static api.poja.io.service.git.GitUtils.doesBranchExist;
import static api.poja.io.service.git.GitUtils.gitAdd;
import static api.poja.io.service.git.GitUtils.gitRm;
import static api.poja.io.service.git.GitUtils.pushAndCheckResult;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static api.poja.io.service.git.GitUtils.updateLocalUpstream;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.BuildToolConversionRequested;
import api.poja.io.endpoint.event.model.DepsConflictResolutionRequested;
import api.poja.io.file.FileWriter;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleSettings;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.transformer.mvn.MavenToGradleConverter;
import api.poja.io.model.importer.transformer.mvn.MvnReadError;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportLogService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class BuildToolConversionRequestedService implements Consumer<BuildToolConversionRequested> {
  private final FileWriter fileWriter;
  private final MavenToGradleConverter mavenToGradleConverter;
  private final ApplicationImportService importService;
  private final ApplicationImportLogService importLogService;
  private final ApplicationImportStateService importStateService;
  private final ApplicationImportMapper importMapper;
  private final EventProducer<DepsConflictResolutionRequested> eventProducer;
  private final GithubService githubService;
  private final AppInstallationService appInstallationService;

  @Override
  public void accept(BuildToolConversionRequested event) {
    var orgId = event.getOrgId();
    var importId = event.getImportId();

    log.info("Build tool conversion requested for ApplicationImport.id={}", importId);

    var importOpt = importService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found. Skipping build tool conversion", importId);
      return;
    }
    var applicationImport = importOpt.get();

    var application = importMapper.toUnknownApplication(applicationImport);
    var root = application.file().toPath();

    BuildTool buildTool =
        importService.downloadBuildToolAnalysisResultData(orgId, importId).buildTool();

    if (GRADLE.equals(buildTool)) {
      log.info("No conversion needed for ApplicationImport.id={}", importId);
      importService.uploadPostConvZippedBuildToolFiles(orgId, importId, root);
      importService.uploadZippedCodeSnapshot(
          orgId, importId, CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT, root);
      fireDepsConflictResolutionEvent(orgId, importId);
      return;
    }

    log.info("build tool {}", buildTool);
    if (MAVEN.equals(buildTool)) {
      log.info("Maven detected for ApplicationImport.id={}, converting...", importId);
      mvnToGradle(root, applicationImport, event.maxConsumerDuration());
      return;
    }

    log.info(
        "Build tool analysis did not detect a valid tool for ApplicationImport.id={}, detected {},"
            + " skipping build tool conversion...",
        importId,
        buildTool);
  }

  private void mvnToGradle(
      Path root, ApplicationImport applicationImport, Duration maxConsumerDuration) {
    String orgId = applicationImport.getOrgId();
    String importId = applicationImport.getId();
    String appName = applicationImport.getAppName();

    importStateService.updateState(
        importId,
        CONVERSION_TO_GRADLE_IN_PROGRESS,
        List.of(ApplicationImportLog.info("Build tool conversion")));

    var pom = root.resolve(POM_XML);
    // assert Files.exists(pom)
    MavenToGradleConverter.Result result = mavenToGradleConverter.apply(pom.toFile());

    if (!result.isSuccess()) {
      var errorMessages = result.errors().stream().map(MvnReadError::toString).toList();
      log.error("Could not convert pom.xml to GradleBuild:\n{}", String.join("\n", errorMessages));

      var errors = errorMessages.stream().map(ApplicationImportLog::error).toList();
      importStateService.updateState(importId, CONVERSION_TO_GRADLE_FAILED, errors);
      return;
    }

    var value = result.value();
    if (value == null) {
      log.error("Unexpected null conversion result for ApplicationImport.id={}", importId);
      importStateService.updateState(importId, CONVERSION_TO_GRADLE_FAILED);
      return;
    }

    if (!value.unhandledTagPaths().isEmpty()) {
      // A state must exist since this block is unreachable without persisting at least one
      var state = importStateService.getStatesByImportId(importId).getFirst();
      String message =
          "unhandled XML paths in pom.xml:" + String.join("\n" + value.unhandledTagPaths());
      log.error("unhandled XML paths in pom.xml {}", value.unhandledTagPaths());
      var warning = ApplicationImportLog.warning(message);
      importLogService.saveAll(List.of(warning), state.getId());
    }

    var gradleBuild = value.gradleBuild();
    var gradleSettings = new GradleSettings(appName);

    try {
      cleanAndMigrateToGradle(
          root, applicationImport, gradleBuild, gradleSettings, maxConsumerDuration);
    } catch (Exception e) {
      var errorMsg = "Unable to migration from mvn to gradle for ApplicationImport.id=" + importId;
      log.error(errorMsg, e);
      importStateService.updateState(
          importId, CONVERSION_TO_GRADLE_FAILED, List.of(ApplicationImportLog.error(errorMsg)));
      return;
    }

    importService.uploadPostConvZippedBuildToolFiles(orgId, importId, root);
    importService.uploadZippedCodeSnapshot(
        orgId, importId, CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT, root);
    importStateService.updateState(importId, CONVERSION_TO_GRADLE_SUCCESSFUL);

    fireDepsConflictResolutionEvent(orgId, importId);
  }

  private void cleanAndMigrateToGradle(
      Path root,
      ApplicationImport applicationImport,
      GradleBuild gradleBuild,
      GradleSettings gradleSettings,
      Duration maxConsumerDuration) {
    UsernamePasswordCredentialsProvider credentials =
        getCredentialsProvider(applicationImport, maxConsumerDuration);
    var defaultBranch = applicationImport.getGithubRepositoryDefaultBranch();
    var branchName = applicationImport.ghMainBranchName();

    try (var git = Git.open(root.toFile())) {
      var branchExists = doesBranchExist(credentials, git, branchName);
      checkoutBranch(branchExists, git, branchName, defaultBranch);

      fileWriter.write(gradleBuild.formatDeclaration().getBytes(), root.toFile(), BUILD_GRADLE);
      fileWriter.write(
          gradleSettings.formatDeclaration().getBytes(), root.toFile(), SETTINGS_GRADLE);

      gitRm(git, Arrays.stream(MAVEN_FILES).map(Path::of).toList());
      gitAdd(git, Arrays.stream(GRADLE_FILES).map(Path::of).toList());

      unsignedCommitAsBot(
          git,
          applicationImport.ghCommitMsgPrefix() + " build tool conv",
          credentials,
          /*is empty*/ false);
      pushAndCheckResult(credentials, branchName, git);
      updateLocalUpstream(
          git, branchName); // note(!): keep local upstream to enable subsequent operations
    } catch (GitAPIException | IOException | ApiException e) {
      throw new RuntimeException(e);
    }
  }

  // todo: This method is currently duplicated here and there, consider moving it to a common
  //       utility class
  private UsernamePasswordCredentialsProvider getCredentialsProvider(
      ApplicationImport applicationImport, Duration duration) {
    var appInstallation = appInstallationService.getById(applicationImport.getAppInstallationId());
    var ghToken = githubService.getInstallationToken(appInstallation.getGhId(), duration);
    return new UsernamePasswordCredentialsProvider("x-access-token", ghToken);
  }

  private void fireDepsConflictResolutionEvent(String orgId, String importId) {
    var event = new DepsConflictResolutionRequested(orgId, importId);
    eventProducer.accept(List.of(event));
  }
}
