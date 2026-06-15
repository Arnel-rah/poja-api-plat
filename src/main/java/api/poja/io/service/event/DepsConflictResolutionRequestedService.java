package api.poja.io.service.event;

import static api.poja.io.file.TempFile.createTempDir;
import static api.poja.io.model.gradle.GradleDist.VERSION_8_5;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.BUILD_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleBuildExtractor.defaultGradleBuildExtractor;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLVED;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.DepsConflictResolutionRequested;
import api.poja.io.endpoint.event.model.PojaConfGenFromGradleRequested;
import api.poja.io.file.FileWriter;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleProject;
import api.poja.io.model.importer.deps.DepsConflictResolver;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.service.ApplicationImportLogService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.gradle.GradleDistDownloader;
import api.poja.io.sys.OpenFilesChecker;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class DepsConflictResolutionRequestedService
    implements Consumer<DepsConflictResolutionRequested> {
  private final FileWriter fileWriter;
  private final ApplicationImportService importService;
  private final ApplicationImportLogService importLogService;
  private final ApplicationImportStateService importStateService;
  private final GradleDistDownloader gradleDistDownloader;
  private final DepsConflictResolver depsConflictResolver;
  private final EventProducer<PojaConfGenFromGradleRequested> eventProducer;

  @Override
  public void accept(DepsConflictResolutionRequested event) {
    var orgId = event.getOrgId();
    var importId = event.getImportId();

    log.info("Build tool conflict resolution requested for ApplicationImport.id={}", importId);

    var importOpt = importService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error(
          "ApplicationImport.id={} not found. Skipping build file conflict resolution", importId);
      return;
    }
    var applicationImport = importOpt.get();
    var pojaVersion = applicationImport.getPojaVersionEnum();
    log.info("Using POJA version {} for importId={}", pojaVersion.toHumanReadableValue(), importId);

    log.info("GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS for importId={}", importId);
    importStateService.updateState(importId, GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS);

    var unzippedGradleBuild = createTempDir("unzipped-gradle");
    importService.downloadAndUnzipGradleBuildPostConversion(orgId, importId, unzippedGradleBuild);

    var openFilesChecker = new OpenFilesChecker();
    var gradleDist = gradleDistDownloader.apply(VERSION_8_5);
    try (var gradleProject = new GradleProject(unzippedGradleBuild, gradleDist)) {
      var root = gradleProject.directory().toPath();
      var gradleExtractionResult = defaultGradleBuildExtractor().extract(gradleProject);

      if (!gradleExtractionResult.isSuccess()) {
        log.error("Gradle build extraction failed for importId={}", importId);
        importStateService.updateState(
            importId,
            GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED,
            gradleExtractionResult.errors().stream()
                .map(error -> ApplicationImportLog.error(error.toString()))
                .toList());
        return;
      }

      resolveConflictsAndUpload(orgId, importId, root, gradleExtractionResult.value(), pojaVersion);
    }
    log.info("[DEBUG] end of operation, checking open files...");
    openFilesChecker.checkOpenFiles();
    openFilesChecker.stop();
  }

  private void resolveConflictsAndUpload(
      String orgId, String importId, Path root, GradleBuild gradleBuild, PojaVersion pojaVersion) {
    var userDeps = gradleBuild.dependencies();

    try {
      var result = depsConflictResolver.apply(userDeps, pojaVersion);
      if (result.failed()) {
        log.error("GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED for importId={}", importId);
        importStateService.updateState(
            importId, GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED, result.logs());
        return;
      }
      var state = importStateService.getStatesByImportId(importId).getFirst();
      importLogService.saveAll(result.logs(), state.getId());

      var resolvedGradleBuild =
          gradleBuild.toBuilder().dependencies(result.data().dependencies()).build();

      updateBuildGradle(root, resolvedGradleBuild);
      importService.uploadPostConflictResolutionZippedBuildToolFiles(orgId, importId, root);
      importStateService.updateState(importId, GRADLE_BUILD_FILE_CONFLICTS_RESOLVED);
      log.info("GRADLE_BUILD_FILE_CONFLICTS_RESOLVED for importId={}", importId);

      firePojaConfGenFromGbfEvent(importId, orgId);

    } catch (Exception e) {
      log.error("GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED for importId={}", importId, e);
      var errorMessage =
          ApplicationImportLog.error(
              "Unexpected error during dependencies conflict resolution: " + e);
      importStateService.updateState(
          importId, GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED, List.of(errorMessage));
    }
  }

  private void updateBuildGradle(Path root, GradleBuild gradleBuild) {
    fileWriter.write(gradleBuild.formatDeclaration().getBytes(), root.toFile(), BUILD_GRADLE);
  }

  private void firePojaConfGenFromGbfEvent(String importId, String orgId) {
    var event = new PojaConfGenFromGradleRequested(importId, orgId);
    eventProducer.accept(List.of(event));
  }
}
