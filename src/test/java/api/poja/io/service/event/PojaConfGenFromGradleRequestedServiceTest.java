package api.poja.io.service.event;

import static api.poja.io.file.ApplicationImportFileType.ANALYSIS_RESULT;
import static api.poja.io.file.ApplicationImportFileType.ENV_VARS_FILE;
import static api.poja.io.file.ExtendedBucketComponent.APP_IMPORT_POJA_CONF_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.APP_LANG_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.ENV_VARS_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.getGradleDistBucketKey;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_6_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.model.PojaVersion.POJA_6;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.gradle.GradleDist.VERSION_8_5;
import static api.poja.io.model.importer.TestMocks.EMAIL_CONF;
import static api.poja.io.model.importer.TestMocks.networkingConf;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATION_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportPojaConfUploaded;
import api.poja.io.endpoint.event.model.PojaConfGenFromGradleRequested;
import api.poja.io.endpoint.rest.mapper.EnvVarMapper;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFExtractDepsError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleBuildExtractor;
import api.poja.io.model.importer.model.FallibleResult;
import api.poja.io.model.importer.poja.GBFToPojaConf;
import api.poja.io.model.pojaConf.factory.PojaConfFactory;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.gradle.GradleDistDownloader;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

// TODO: refactor to be independent from poja conf 6
class PojaConfGenFromGradleRequestedServiceTest {
  final FileUnzipper unzipper = new FileUnzipper(new FileWriter(new ExtensionGuesser()));
  final ObjectMapper objectMapper = new ObjectMapper();

  PojaConfGenFromGradleRequestedService subject;
  ApplicationImportService importServiceMock;
  ApplicationImportStateService importStateServiceMock;
  EventProducer<AppImportPojaConfUploaded> eventProducerMock;
  ExtendedBucketComponent bucketComponentMock;
  PojaConfFileMapper pojaConfFileMapperMock;

  @SneakyThrows
  @BeforeEach
  void setup() {
    importServiceMock = mock();
    importStateServiceMock = mock();
    eventProducerMock = mock();
    bucketComponentMock = mock();
    pojaConfFileMapperMock = mock();

    when(importServiceMock.findById(eq(APP_IMPORT_1_ID)))
        .thenReturn(Optional.of(pendingAppImport()));
    when(importServiceMock.findById(eq(APP_IMPORT_6_ID)))
        .thenAnswer(
            invocation -> {
              var appImport = pendingAppImport();
              appImport.setPojaVersion(POJA_6.toHumanReadableValue());
              return Optional.of(appImport);
            });
    doAnswer(
            invocation -> {
              var target = (Path) invocation.getArgument(2);
              var gradleProject = getFile("files/import/bt-conversion/gradle-project.zip");
              unzipper.apply(new ZipFile(gradleProject), target);
              return null;
            })
        .when(importServiceMock)
        .downloadAndUnzipGradleBuildPostConflictResolution(any(), any(), any());

    when(bucketComponentMock.download(getGradleDistBucketKey(VERSION_8_5)))
        .thenReturn(getResource("files/gradle-dist/gradle-8.5-bin.zip").getFile());
  }

  @Test
  void successfulGen_shouldSave_successState() {
    var networkingServiceMock = mock(NetworkingService.class);

    var envVarMapper = new EnvVarMapper(new ObjectMapper());
    var gbfToPojaConf = new GBFToPojaConf(new PojaConfFactory(networkingServiceMock, EMAIL_CONF));

    when(networkingServiceMock.getNetworkingConfig()).thenReturn(networkingConf());
    when(importServiceMock.downloadAppImportFile(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), eq(ENV_VARS_FILE), eq(ENV_VARS_FILENAME)))
        .thenReturn(getFile("files/env-vars.json"));
    when(pojaConfFileMapperMock.writeToTempFile(any())).thenReturn(null);
    when(importServiceMock.downloadAppImportFile(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(ANALYSIS_RESULT),
            eq(APP_LANG_ANALYSIS_RESULT_FILENAME)))
        .thenReturn(
            getFile(
                "files/import/analyzer/lang/analysis_result/java-project-lang-analysis-result.json"));
    when(bucketComponentMock.upload(any(), any())).thenReturn(null);

    subject =
        new PojaConfGenFromGradleRequestedService(
            importServiceMock,
            importStateServiceMock,
            gbfToPojaConf,
            pojaConfFileMapperMock,
            envVarMapper,
            bucketComponentMock,
            objectMapper,
            eventProducerMock,
            new GradleDistDownloader(bucketComponentMock, unzipper));

    var event = new PojaConfGenFromGradleRequested(APP_IMPORT_1_ID, ORG_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(importStateServiceMock)
        .updateState(eq(APP_IMPORT_1_ID), eq(POJA_CONF_GENERATION_IN_PROGRESS));
    verify(importStateServiceMock).updateState(eq(APP_IMPORT_1_ID), eq(POJA_CONF_GENERATED));
    verify(bucketComponentMock).upload(any(), endsWith(APP_IMPORT_POJA_CONF_FILENAME));
    verify(eventProducerMock)
        .accept(List.of(new AppImportPojaConfUploaded(ORG_1_ID, APP_IMPORT_1_ID)));
  }

  @Test
  void failedGradleExtraction_shouldSave_failureState() {
    var gbfToPojaConfMock = mock(GBFToPojaConf.class);
    var envVarMapperMock = mock(EnvVarMapper.class);

    subject =
        new PojaConfGenFromGradleRequestedService(
            importServiceMock,
            importStateServiceMock,
            gbfToPojaConfMock,
            pojaConfFileMapperMock,
            envVarMapperMock,
            bucketComponentMock,
            objectMapper,
            eventProducerMock,
            new GradleDistDownloader(bucketComponentMock, unzipper));

    var event = new PojaConfGenFromGradleRequested(APP_IMPORT_1_ID, ORG_1_ID);

    try (MockedStatic<GradleBuildExtractor> mockExtractorClass =
        mockStatic(GradleBuildExtractor.class)) {
      var gbExtractorMock = mock(GradleBuildExtractor.class);

      when(gbExtractorMock.extract(any()))
          .thenReturn(
              new FallibleResult<>(null, List.of(), List.of(new GBFExtractDepsError(null))));

      mockExtractorClass
          .when(GradleBuildExtractor::defaultGradleBuildExtractor)
          .thenReturn(gbExtractorMock);

      assertDoesNotThrow(() -> subject.accept(event));

      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(POJA_CONF_GENERATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(
              eq(APP_IMPORT_1_ID),
              eq(POJA_CONF_GENERATION_FAILED),
              argThat(
                  logs ->
                      1 == logs.size()
                          && "Gradle build extraction failed. Poja conf could not be generated"
                              .equals(logs.getFirst().getMessage())));
    }
  }

  @Test
  void invalidMainClassIdentifier_shouldSave_failureState() {
    var envVarMapperMock = mock(EnvVarMapper.class);
    var networkingServiceMock = mock(NetworkingService.class);

    var gbfToPojaConf = new GBFToPojaConf(new PojaConfFactory(networkingServiceMock, EMAIL_CONF));

    when(importServiceMock.downloadAppImportFile(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(ANALYSIS_RESULT),
            eq(APP_LANG_ANALYSIS_RESULT_FILENAME)))
        .thenReturn(
            getFile(
                "files/import/analyzer/lang/analysis_result/main-with-annotations-result.json"));

    subject =
        new PojaConfGenFromGradleRequestedService(
            importServiceMock,
            importStateServiceMock,
            gbfToPojaConf,
            pojaConfFileMapperMock,
            envVarMapperMock,
            bucketComponentMock,
            objectMapper,
            eventProducerMock,
            new GradleDistDownloader(bucketComponentMock, unzipper));

    var event = new PojaConfGenFromGradleRequested(APP_IMPORT_1_ID, ORG_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(importStateServiceMock)
        .updateState(eq(APP_IMPORT_1_ID), eq(POJA_CONF_GENERATION_IN_PROGRESS));
    verify(importStateServiceMock)
        .updateState(
            eq(APP_IMPORT_1_ID),
            eq(POJA_CONF_GENERATION_FAILED),
            argThat(
                logs ->
                    1 == logs.size()
                        && "Invalid java main class identifier. Poja conf could not be generated"
                            .equals(logs.getFirst().getMessage())));
  }

  @Test
  void unsupported_pojaConfVersionAppImport_should_fail() {
    var envVarMapperMock = mock(EnvVarMapper.class);
    var networkingServiceMock = mock(NetworkingService.class);

    var gbfToPojaConf = new GBFToPojaConf(new PojaConfFactory(networkingServiceMock, EMAIL_CONF));

    subject =
        new PojaConfGenFromGradleRequestedService(
            importServiceMock,
            importStateServiceMock,
            gbfToPojaConf,
            pojaConfFileMapperMock,
            envVarMapperMock,
            bucketComponentMock,
            objectMapper,
            eventProducerMock,
            new GradleDistDownloader(bucketComponentMock, unzipper));

    var event = new PojaConfGenFromGradleRequested(APP_IMPORT_6_ID, ORG_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));
    verify(importStateServiceMock)
        .updateState(eq(APP_IMPORT_6_ID), eq(POJA_CONF_GENERATION_IN_PROGRESS));
    verify(importStateServiceMock)
        .updateState(
            eq(APP_IMPORT_6_ID),
            eq(POJA_CONF_GENERATION_FAILED),
            argThat(
                logs ->
                    1 == logs.size()
                        && (String.format(
                                "Unsupported PojaVersion, required >= %s, found %s",
                                POJA_7.toHumanReadableValue(), POJA_6.toHumanReadableValue()))
                            .equals(logs.getFirst().getMessage())));
  }
}
