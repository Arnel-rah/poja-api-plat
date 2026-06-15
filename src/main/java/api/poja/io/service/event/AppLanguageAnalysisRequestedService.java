package api.poja.io.service.event;

import static api.poja.io.file.ApplicationImportFileType.ZIPPED_CODE;
import static api.poja.io.file.ExtendedBucketComponent.INITIAL_ZIPPED_CODE_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.getAppImportBucketKey;
import static api.poja.io.file.TempFile.createTempDir;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppLanguageAnalysisRequested;
import api.poja.io.endpoint.event.model.BuildToolAnalysisRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileUnzipper;
import api.poja.io.model.importer.analyzer.lang.AppLanguageAnalyzer;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AppLanguageAnalysisRequestedService implements Consumer<AppLanguageAnalysisRequested> {
  private final ApplicationImportService applicationImportService;
  private final ApplicationImportStateService stateService;
  private final AppLanguageAnalyzer appLanguageAnalyzer;
  private final ExtendedBucketComponent bucketComponent;
  private final FileUnzipper unzipper;
  private final EventProducer<BuildToolAnalysisRequested> eventProducer;

  @Override
  public void accept(AppLanguageAnalysisRequested event) {
    var importId = event.getImportId();
    var orgId = event.getOrgId();
    log.info("APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS for importId={}", importId);
    stateService.updateState(importId, APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS);

    try {
      log.info("Downloading & Unzipping code for importId={}", importId);
      var codeBucketKey =
          getAppImportBucketKey(orgId, importId, ZIPPED_CODE, INITIAL_ZIPPED_CODE_FILENAME);
      var unzippedCode = createTempDir("unzipped-code");
      var appImportCode = bucketComponent.download(codeBucketKey);

      unzip(asZipFile(appImportCode), unzippedCode);

      // Use empty Set because env vars are not needed for this operation
      var unknownApp = new UnknownApplication(unzippedCode.toFile(), Set.of());
      var result = appLanguageAnalyzer.analyze(unknownApp);

      if (result.failed()) {
        log.error("APPLICATION_LANGUAGE_VERIFICATION_FAILED for importId={}", importId);
        stateService.updateState(importId, APPLICATION_LANGUAGE_VERIFICATION_FAILED, result.logs());
        return;
      }

      log.info("APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL for importId={}", importId);
      applicationImportService.uploadAppImportLangAnalysisResult(orgId, importId, result.data());
      stateService.updateState(importId, APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL);
      fireBuildToolAnalysisEvent(orgId, importId);
    } catch (Exception e) {
      log.error("APPLICATION_LANGUAGE_VERIFICATION_FAILED for importId={}", importId, e);
      var errorMessage = ApplicationImportLog.error("Unexpected error during analysis: " + e);
      stateService.updateState(
          importId, APPLICATION_LANGUAGE_VERIFICATION_FAILED, List.of(errorMessage));
    }
  }

  private void fireBuildToolAnalysisEvent(String orgId, String importId) {
    var event = new BuildToolAnalysisRequested(importId, orgId);
    eventProducer.accept(List.of(event));
  }

  @SneakyThrows
  private static ZipFile asZipFile(File toUnzip) {
    return new ZipFile(toUnzip);
  }

  private void unzip(ZipFile downloaded, Path destination) {
    unzipper.apply(downloaded, destination);
  }
}
