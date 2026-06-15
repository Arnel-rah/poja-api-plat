package api.poja.io.service.event;

import static api.poja.io.file.ApplicationImportFileType.ANALYSIS_RESULT;
import static api.poja.io.file.ApplicationImportFileType.ENV_VARS_FILE;
import static api.poja.io.file.ApplicationImportFileType.POJA_CONF_FILE;
import static api.poja.io.file.ExtendedBucketComponent.APP_IMPORT_POJA_CONF_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.APP_LANG_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.ENV_VARS_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.getAppImportBucketKey;
import static api.poja.io.file.TempFile.createTempDir;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.gradle.GradleDist.VERSION_8_5;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleBuildExtractor.defaultGradleBuildExtractor;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATION_IN_PROGRESS;
import static api.poja.io.service.appEnvConfigurer.mapper.PojaConfMapper.suffixAppName;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportPojaConfUploaded;
import api.poja.io.endpoint.event.model.PojaConfGenFromGradleRequested;
import api.poja.io.endpoint.rest.mapper.EnvVarMapper;
import api.poja.io.endpoint.rest.model.EnvVar;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleProject;
import api.poja.io.model.importer.analyzer.lang.AppLangAnalyzerData;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.poja.GBFToPojaConf;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.gradle.GradleDistDownloader;
import api.poja.io.sys.OpenFilesChecker;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PojaConfGenFromGradleRequestedService
    implements Consumer<PojaConfGenFromGradleRequested> {
  private static final String MAIN_JAVA_SRC = "src/main/java/";
  private static final String JAVA_FILE_EXTENSION = ".java";

  private final ApplicationImportService applicationImportService;
  private final ApplicationImportStateService stateService;
  private final GBFToPojaConf gbfToPojaConf;
  private final PojaConfFileMapper pojaConfFileMapper;
  private final EnvVarMapper envVarMapper;
  private final ExtendedBucketComponent bucketComponent;
  private final ObjectMapper objectMapper;
  private final EventProducer<AppImportPojaConfUploaded> eventProducer;
  private final GradleDistDownloader gradleDistDownloader;

  @Override
  public void accept(PojaConfGenFromGradleRequested event) {
    var orgId = event.getOrgId();
    var importId = event.getAppImportId();

    log.info("PojaConfGenFromGradleRequested for import = {}", importId);

    var importOpt = applicationImportService.findById(importId);
    if (importOpt.isEmpty()) {
      log.error("ApplicationImport.id={} not found. Skipping poja conf generation", importId);
      return;
    }
    var appImport = importOpt.get();
    var userId = appImport.getUserId();

    log.info("Converting gradle build to poja conf for appImport {}", importId);
    stateService.updateState(importId, POJA_CONF_GENERATION_IN_PROGRESS);

    if (appImport.getPojaVersionEnum().compareTo(POJA_7) < 0) {
      log.error(
          "Unsupported PojaVersion for ApplicationImport#{}: required >= {}, found {}",
          importId,
          POJA_7.toHumanReadableValue(),
          appImport.getPojaVersion());
      saveStateWithErrors(
          "Unsupported PojaVersion, required >= %s, found %s"
              .formatted(POJA_7.toHumanReadableValue(), appImport.getPojaVersion()),
          importId);
      return;
    }

    var unzippedGradleBuild = createTempDir("unzipped-gradle");
    applicationImportService.downloadAndUnzipGradleBuildPostConflictResolution(
        orgId, importId, unzippedGradleBuild);

    var openFilesChecker = new OpenFilesChecker();
    openFilesChecker.start();
    var gradleDist = gradleDistDownloader.apply(VERSION_8_5);
    try (var gradleProject = new GradleProject(unzippedGradleBuild, gradleDist)) {
      var gradleExtractionResult = defaultGradleBuildExtractor().extract(gradleProject);
      if (gradleExtractionResult.isSuccess()) {
        assert gradleExtractionResult.value() != null;

        var javaMainClassIdentifier = getJavaMainClassIdentifier(orgId, importId);
        var envVarFile =
            applicationImportService.downloadAppImportFile(
                appImport.getOrgId(), appImport.getId(), ENV_VARS_FILE, ENV_VARS_FILENAME);
        var restEnvVars =
            envVarMapper.toDomain(envVarFile).stream().map(envVarMapper::toRest).toList();

        if (!isValidClassIdentifier(javaMainClassIdentifier)) {
          log.error(
              "Invalid java main class identifier for import={}. Poja conf could not be generated",
              importId);
          saveStateWithErrors(
              "Invalid java main class identifier. Poja conf could not be generated", importId);
          return;
        }

        var packageFullName =
            javaMainClassIdentifier.substring(0, javaMainClassIdentifier.lastIndexOf('.'));

        PojaConf pojaConf =
            gbfToPojaConf.toPojaConf(
                gradleExtractionResult.value(), appImport.getPojaVersionEnum());

        // todo: move to different class
        var configuredPojaConf =
            setAppImportProperties(
                pojaConf,
                suffixAppName(appImport.getAppName(), userId),
                javaMainClassIdentifier,
                packageFullName,
                restEnvVars);

        var pojaConfFile = pojaConfFileMapper.writeToTempFile(configuredPojaConf);

        uploadPojaFiles(pojaConfFile, orgId, importId);
        stateService.updateState(importId, POJA_CONF_GENERATED);
        fireAppImportConfUploaded(orgId, importId);
        return;
      }
      log.error(
          "Gradle build extraction failed for import= {}. Poja conf could not be generated",
          importId);
      saveStateWithErrors(
          "Gradle build extraction failed. Poja conf could not be generated", importId);
    } catch (IOException e) {
      log.error("e", e);
      saveStateWithErrors(
          "Java main class identifier extraction failed. Poja conf could not be generated",
          importId);
    }
    log.info("[DEBUG] end of operation, checking open files...");
    openFilesChecker.checkOpenFiles();
    openFilesChecker.stop();
  }

  private PojaConf setAppImportProperties(
      PojaConf pojaConf,
      String appName,
      String javaMainClass,
      String packageFullName,
      List<EnvVar> envVars) {
    return switch (pojaConf) {
      case PojaConf7 c ->
          c.toBuilder()
              .general(
                  c.general().toBuilder()
                      .appName(appName)
                      .javaMainClass(javaMainClass)
                      .packageFullName(packageFullName)
                      .customJavaEnvVars(envVars)
                      .build())
              .build();
      case PojaConf8 c ->
          c.toBuilder()
              .general(
                  c.general().toBuilder()
                      .appName(appName)
                      .javaMainClass(javaMainClass)
                      .packageFullName(packageFullName)
                      .customJavaEnvVars(envVars)
                      .build())
              .build();
      case PojaConf9 c ->
          c.toBuilder()
              .general(
                  c.general().toBuilder()
                      .appName(appName)
                      .javaMainClass(javaMainClass)
                      .packageFullName(packageFullName)
                      .customJavaEnvVars(envVars)
                      .build())
              .build();
      default -> throw new IllegalStateException("Unsupported PojaConf" + pojaConf.version());
    };
  }

  private void saveStateWithErrors(String message, String importId) {
    var errorLog = new ApplicationImportLog(ERROR, message);
    stateService.updateState(importId, POJA_CONF_GENERATION_FAILED, List.of(errorLog));
  }

  private void uploadPojaFiles(File toUpload, String orgId, String importId) {
    var bucketKey =
        getAppImportBucketKey(orgId, importId, POJA_CONF_FILE, APP_IMPORT_POJA_CONF_FILENAME);
    bucketComponent.upload(toUpload, bucketKey);
  }

  private void fireAppImportConfUploaded(String orgId, String importId) {
    var event = new AppImportPojaConfUploaded(orgId, importId);
    eventProducer.accept(List.of(event));
  }

  private String getJavaMainClassIdentifier(String orgId, String importId) throws IOException {
    var langAnalysisFile =
        applicationImportService.downloadAppImportFile(
            orgId, importId, ANALYSIS_RESULT, APP_LANG_ANALYSIS_RESULT_FILENAME);
    AppLangAnalyzerData data = objectMapper.readValue(langAnalysisFile, AppLangAnalyzerData.class);

    var mainClassPath = data.mainClassPath().toString();
    var transformedClassPath =
        mainClassPath.trim().replace(MAIN_JAVA_SRC, "").replace(JAVA_FILE_EXTENSION, "");
    String[] splittedClassPath = transformedClassPath.split("/");
    return String.join(".", splittedClassPath);
  }

  private static boolean isValidClassIdentifier(String classIdentifier) {
    return !(classIdentifier.split("\\.").length <= 1);
  }
}
