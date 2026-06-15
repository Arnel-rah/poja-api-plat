package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.ExtendedBucketComponent.ENV_VARS_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.INITIAL_ZIPPED_CODE_FILENAME;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.model.importer.TestMocks.GRADLE_BUILD_FROM_MVN;
import static api.poja.io.model.importer.TestMocks.GRADLE_SETTINGS_FROM_MVN;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.BUILD_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.GRADLE_FILES;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.MAVEN_FILES;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.POM_XML;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.SETTINGS_GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.GRADLE;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool.MAVEN;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CONVERSION_TO_GRADLE_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CONVERSION_TO_GRADLE_SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.EndpointConf;
import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.BuildToolConversionRequested;
import api.poja.io.endpoint.event.model.DepsConflictResolutionRequested;
import api.poja.io.endpoint.rest.mapper.EnvVarMapper;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildTool;
import api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer.BuildToolData;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.model.importer.transformer.mvn.MavenToGradleConverter;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.repository.model.ApplicationImportState;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportLogService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class BuildToolConversionRequestedServiceTest {

  static final String GH_TOKEN = "gh_token";

  final FileWriter fileWriter = new FileWriter(new ExtensionGuesser());

  EventProducer<DepsConflictResolutionRequested> eventProducerMock;
  ApplicationImportLogService importLogServiceMock;
  ApplicationImportStateService importStateServiceMock;
  ApplicationImportService importServiceMock;
  AppInstallationService appInstallationServiceMock;
  ExtendedBucketComponent bucketComponentMock;
  GithubService githubServiceMock;
  MavenToGradleConverter mavenToGradleConverterSpy;
  ApplicationImportMapper importMapperSpy;
  BuildToolConversionRequestedService subject;

  @BeforeEach
  void setUp() {
    eventProducerMock = mock();
    importLogServiceMock = mock();
    importStateServiceMock = mock();
    importServiceMock = mock();
    bucketComponentMock = mock();
    appInstallationServiceMock = mock();
    githubServiceMock = mock();
    mavenToGradleConverterSpy = spy(new MavenToGradleConverter());

    importMapperSpy =
        spy(
            new ApplicationImportMapper(
                mock(),
                bucketComponentMock,
                new FileUnzipper(fileWriter),
                new EnvVarMapper(new EndpointConf().objectMapper())));
    subject =
        new BuildToolConversionRequestedService(
            fileWriter,
            mavenToGradleConverterSpy,
            importServiceMock,
            importLogServiceMock,
            importStateServiceMock,
            importMapperSpy,
            eventProducerMock,
            githubServiceMock,
            appInstallationServiceMock);

    doAnswer(
            invocation -> {
              var application = (UnknownApplication) invocation.callRealMethod();
              File dir = application.file();
              try (var git = Git.open(dir)) {
                git.remoteAdd().setName("origin").setUri(new URIish(dir.toURI().toString())).call();
              } catch (RepositoryNotFoundException e) {
                // do nothing
              }
              return application;
            })
        .when(importMapperSpy)
        .toUnknownApplication(any());

    when(importServiceMock.findById(APP_IMPORT_1_ID)).thenReturn(Optional.of(pendingAppImport()));
    when(importStateServiceMock.getStatesByImportId(APP_IMPORT_1_ID))
        .thenReturn(List.of(ApplicationImportState.builder().id("in_progress_1_id").build()));

    when(appInstallationServiceMock.getById(any())).thenReturn(appInstallation_1());
    when(githubServiceMock.getInstallationToken(anyLong(), any())).thenReturn(GH_TOKEN);
  }

  @Test
  void nonExistent_appImport_shouldBe_skipped() {
    var importId = "NonExistentAppImportId";
    var event = new BuildToolConversionRequested(ORG_1_ID, importId);

    subject.accept(event);

    verify(importServiceMock, times(1)).findById(importId);
    verifyNoMoreInteractions(
        importLogServiceMock, importStateServiceMock, importServiceMock, bucketComponentMock);
  }

  @SneakyThrows
  @Test
  void alreadyGradleProject_shouldBe_skipped() {
    when(bucketComponentMock.download(endsWith(INITIAL_ZIPPED_CODE_FILENAME)))
        .thenAnswer(
            (invocation) -> getResource("files/import/bt-conversion/gradle-project.zip").getFile());
    when(bucketComponentMock.download(endsWith(ENV_VARS_FILENAME)))
        .thenReturn(getResource("files/env-vars.json").getFile());
    when(importServiceMock.downloadBuildToolAnalysisResultData(ORG_1_ID, APP_IMPORT_1_ID))
        .thenReturn(new BuildToolData(GRADLE, Arrays.stream(GRADLE_FILES).map(Path::of).toList()));

    var event = new BuildToolConversionRequested(ORG_1_ID, APP_IMPORT_1_ID);

    subject.accept(event);

    verify(importServiceMock, times(1))
        .downloadBuildToolAnalysisResultData(ORG_1_ID, APP_IMPORT_1_ID);
    verify(importServiceMock, times(1)).findById(APP_IMPORT_1_ID);
    verify(bucketComponentMock, times(1)).download(endsWith(INITIAL_ZIPPED_CODE_FILENAME));
    verify(bucketComponentMock, times(1)).download(endsWith(ENV_VARS_FILENAME));
    verify(eventProducerMock, times(1))
        .accept(List.of(new DepsConflictResolutionRequested(ORG_1_ID, APP_IMPORT_1_ID)));
    verify(importServiceMock)
        .uploadPostConvZippedBuildToolFiles(eq(ORG_1_ID), eq(APP_IMPORT_1_ID), any(Path.class));
    verify(importServiceMock)
        .uploadZippedCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));
    verifyNoMoreInteractions(
        importLogServiceMock,
        importStateServiceMock,
        importServiceMock,
        bucketComponentMock,
        eventProducerMock);
  }

  @SneakyThrows
  @ParameterizedTest
  @ValueSource(strings = {"NONE", "MULTIPLE", "INCOMPLETE"})
  void noValidTool_shouldBe_skipped(String buildTool) {
    when(bucketComponentMock.download(endsWith(INITIAL_ZIPPED_CODE_FILENAME)))
        .thenAnswer(
            (invocation) -> getResource("files/import/bt-conversion/plain-project.zip").getFile());
    when(bucketComponentMock.download(endsWith(ENV_VARS_FILENAME)))
        .thenReturn(getResource("files/env-vars.json").getFile());
    when(importServiceMock.downloadBuildToolAnalysisResultData(ORG_1_ID, APP_IMPORT_1_ID))
        .thenReturn(new BuildToolData(BuildTool.fromValue(buildTool), List.of()));

    var event = new BuildToolConversionRequested(ORG_1_ID, APP_IMPORT_1_ID);

    subject.accept(event);

    verify(importServiceMock, times(1))
        .downloadBuildToolAnalysisResultData(ORG_1_ID, APP_IMPORT_1_ID);
    verify(importServiceMock, times(1)).findById(APP_IMPORT_1_ID);
    verify(bucketComponentMock, times(1)).download(endsWith(INITIAL_ZIPPED_CODE_FILENAME));
    verify(bucketComponentMock, times(1)).download(endsWith(ENV_VARS_FILENAME));

    verifyNoMoreInteractions(
        importLogServiceMock,
        importStateServiceMock,
        importServiceMock,
        bucketComponentMock,
        eventProducerMock);
  }

  @SneakyThrows
  @Test
  void mvnProject_shouldBe_converted() {
    when(bucketComponentMock.download(endsWith(INITIAL_ZIPPED_CODE_FILENAME)))
        .thenAnswer(
            (invocation) -> getResource("files/import/bt-conversion/mvn-project.zip").getFile());
    when(bucketComponentMock.download(endsWith(ENV_VARS_FILENAME)))
        .thenReturn(getResource("files/env-vars.json").getFile());
    when(importServiceMock.downloadBuildToolAnalysisResultData(ORG_1_ID, APP_IMPORT_1_ID))
        .thenReturn(new BuildToolData(MAVEN, Arrays.stream(MAVEN_FILES).map(Path::of).toList()));

    // alt to atomic ref to intercept a val inside a lambda fn
    var rootPathAt0 = new Path[1];
    var filesMock = mockStatic(Files.class, CALLS_REAL_METHODS);
    filesMock
        .when(() -> Files.createTempDirectory(eq("unzipped-code")))
        .thenAnswer(
            invocation -> {
              Path rootPath = Files.createTempDirectory("unzipped-code-ril");
              rootPathAt0[0] = rootPath;
              return rootPath;
            });

    var event = new BuildToolConversionRequested(ORG_1_ID, APP_IMPORT_1_ID);

    subject.accept(event);

    verify(importServiceMock, times(1))
        .downloadBuildToolAnalysisResultData(ORG_1_ID, APP_IMPORT_1_ID);
    verify(importServiceMock, times(1)).findById(APP_IMPORT_1_ID);
    verify(importStateServiceMock, times(1))
        .updateState(
            eq(APP_IMPORT_1_ID),
            eq(CONVERSION_TO_GRADLE_IN_PROGRESS),
            argThat(
                logs ->
                    1 == logs.size()
                        && "Build tool conversion".equals(logs.getFirst().getMessage())));
    verify(importServiceMock)
        .uploadPostConvZippedBuildToolFiles(ORG_1_ID, APP_IMPORT_1_ID, rootPathAt0[0]);
    verify(importServiceMock)
        .uploadZippedCodeSnapshot(
            ORG_1_ID, APP_IMPORT_1_ID, CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT, rootPathAt0[0]);
    verify(importStateServiceMock, times(1))
        .updateState(eq(APP_IMPORT_1_ID), eq(CONVERSION_TO_GRADLE_SUCCESSFUL));

    verify(importStateServiceMock, times(1)).getStatesByImportId(APP_IMPORT_1_ID);
    verify(importLogServiceMock, times(1)).saveAll(any(), eq("in_progress_1_id"));

    verify(bucketComponentMock, times(1)).download(endsWith(INITIAL_ZIPPED_CODE_FILENAME));
    verify(bucketComponentMock, times(1)).download(endsWith(ENV_VARS_FILENAME));

    verify(eventProducerMock, times(1))
        .accept(List.of(new DepsConflictResolutionRequested(ORG_1_ID, APP_IMPORT_1_ID)));

    verifyNoMoreInteractions(
        importLogServiceMock,
        importStateServiceMock,
        importServiceMock,
        bucketComponentMock,
        eventProducerMock);

    Path root = rootPathAt0[0];
    assertFalse(Files.exists(root.resolve(POM_XML)));

    var genBuild = root.resolve(BUILD_GRADLE);
    var genSettings = root.resolve(SETTINGS_GRADLE);

    assertTrue(Files.exists(genBuild));
    assertTrue(Files.exists(genSettings));
    assertEquals(GRADLE_BUILD_FROM_MVN.toString(), Files.readString(genBuild));
    assertEquals(GRADLE_SETTINGS_FROM_MVN.toString(), Files.readString(genSettings));
    assertTrue(Arrays.stream(MAVEN_FILES).map(root::resolve).allMatch(Files::notExists));
  }

  static AppInstallation appInstallation_1() {
    return AppInstallation.builder()
        .id("app_installation_id")
        .orgId(ORG_1_ID)
        .userId("user_id")
        .ghId(1234)
        .ownerGithubLogin("user")
        .build();
  }
}
