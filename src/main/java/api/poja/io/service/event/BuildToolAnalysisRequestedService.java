package api.poja.io.service.event;

import static api.poja.io.model.importer.model.ApplicationImportLog.error;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_SUCCESSFUL;
import static java.nio.file.Files.createTempDirectory;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.BuildToolAnalysisRequested;
import api.poja.io.endpoint.event.model.PreTransformationTestRunRequested;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolAnalyzer;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolData;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class BuildToolAnalysisRequestedService implements Consumer<BuildToolAnalysisRequested> {
  private final ApplicationImportService applicationImportService;
  private final ApplicationImportStateService stateService;
  private final BuildToolAnalyzer analyzer;
  private final EventProducer<PreTransformationTestRunRequested> eventProducer;

  @Override
  @SneakyThrows
  public void accept(BuildToolAnalysisRequested event) {
    var importId = event.getImportId();
    var orgId = event.getOrgId();
    var appImport = applicationImportService.getById(importId);
    log.info("BUILD_TOOL_VERIFICATION_IN_PROGRESS for importId={}", importId);
    stateService.updateState(importId, BUILD_TOOL_VERIFICATION_IN_PROGRESS);

    try {
      log.info("Downloading & Unzipping code for importId={}", importId);
      var unzippedCode = createTempDirectory("unzipped-code");
      applicationImportService.downloadAndUnzipCode(orgId, importId, unzippedCode);
      var unknownApp = new UnknownApplication(unzippedCode.toFile(), Set.of());
      var result = analyzer.analyze(unknownApp);
      if (result.failed()) {
        log.error(" BUILD_TOOL_VERIFICATION_FAILED for importId={}", importId);
        stateService.updateState(importId, BUILD_TOOL_VERIFICATION_FAILED, result.logs());
        return;
      }

      BuildToolData resultData = result.data();

      applicationImportService.uploadInitialZippedBuildToolFiles(
          orgId, importId, resultData.rawBuildToolFilePaths());
      stateService.updateState(importId, BUILD_TOOL_VERIFICATION_SUCCESSFUL);
      log.info("BUILD_TOOL_VERIFICATION_SUCCESSFUL for importId={}", importId);

      log.info("Uploading Build tool analysis result for importId={}", importId);
      applicationImportService.uploadBuildToolAnalysisResultData(orgId, importId, resultData);

      firePreTransformationTestRunEvent(appImport.getId());

    } catch (Exception e) {
      log.error("BUILD_TOOL_VERIFICATION_FAILED for importId={}", importId, e);
      stateService.updateState(
          importId,
          BUILD_TOOL_VERIFICATION_FAILED,
          List.of(error("Unexpected error during build tool analysis: " + e)));
    }
  }

  private void firePreTransformationTestRunEvent(String importId) {
    var event = new PreTransformationTestRunRequested(importId);
    eventProducer.accept(List.of(event));
  }
}
