package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.getGradleDistBucketKey;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.gradle.GradleDist.VERSION_8_5;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_RESOLVED_GRADLE_DEPENDENCIES;
import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleBuildExtractor.defaultGradleBuildExtractor;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLVED;
import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.DepsConflictResolutionRequested;
import api.poja.io.endpoint.event.model.PojaConfGenFromGradleRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.file.TempFile;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleProject;
import api.poja.io.model.importer.deps.DepsConflictResolver;
import api.poja.io.model.importer.deps.DepsConflictResolverResult;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.ConflictResolutionData;
import api.poja.io.repository.model.ApplicationImportState;
import api.poja.io.service.ApplicationImportLogService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.gradle.GradleDistDownloader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class DepsConflictResolutionRequestedServiceTest {
  final FileWriter fileWriter = new FileWriter(new ExtensionGuesser());
  final FileUnzipper fileUnzipper = new FileUnzipper(fileWriter);

  EventProducer<PojaConfGenFromGradleRequested> eventProducerMock;
  ApplicationImportService importServiceMock;
  ApplicationImportLogService importLogServiceMock;
  ApplicationImportStateService importStateServiceMock;
  ExtendedBucketComponent bucketComponentMock;
  DepsConflictResolver depsConflictResolverMock;
  DepsConflictResolutionRequestedService subject;

  static final Comparator<GradleDependency> DEP_COMPARATOR =
      comparing(d -> d.configuration() + ":" + d.group() + ":" + d.name() + ":" + d.version());

  @SneakyThrows
  @BeforeEach
  void setUp() {
    eventProducerMock = mock();
    importServiceMock = mock();
    importLogServiceMock = mock();
    importStateServiceMock = mock();
    bucketComponentMock = mock();
    depsConflictResolverMock = mock();

    subject =
        new DepsConflictResolutionRequestedService(
            fileWriter,
            importServiceMock,
            importLogServiceMock,
            importStateServiceMock,
            new GradleDistDownloader(bucketComponentMock, fileUnzipper),
            depsConflictResolverMock,
            eventProducerMock);

    when(bucketComponentMock.download(getGradleDistBucketKey(VERSION_8_5)))
        .thenReturn(getResource("files/gradle-dist/gradle-8.5-bin.zip").getFile());
    when(importServiceMock.findById(APP_IMPORT_1_ID)).thenReturn(Optional.of(pendingAppImport()));
  }

  @Test
  void nonExistent_appImport_shouldBe_skipped() {
    var importId = "NonExistentAppImportId";
    var event = new DepsConflictResolutionRequested(ORG_1_ID, importId);

    subject.accept(event);

    verify(importServiceMock).findById(importId);
    verifyNoMoreInteractions(importServiceMock, depsConflictResolverMock, eventProducerMock);
  }

  @SneakyThrows
  @Test
  void depsConflictResolution_fails() {
    var failedResult = mock(DepsConflictResolverResult.class);
    when(failedResult.failed()).thenReturn(true);
    when(failedResult.status()).thenReturn(FAILED);
    when(failedResult.data()).thenReturn(new ConflictResolutionData(List.of()));
    when(failedResult.logs()).thenReturn(List.of(ApplicationImportLog.error("Version conflict")));
    when(depsConflictResolverMock.apply(any(), eq(POJA_7))).thenReturn(failedResult);

    doAnswer(
            invocation -> {
              Path targetDir = invocation.getArgument(2);
              var zipFile =
                  new ZipFile(
                      getResource("files/import/bt-deps-conflict/gradle-project-with-conflict.zip")
                          .getFile());
              fileUnzipper.apply(zipFile, targetDir);
              return null;
            })
        .when(importServiceMock)
        .downloadAndUnzipGradleBuildPostConversion(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), any(Path.class));

    try (var filesMock = mockStatic(TempFile.class, CALLS_REAL_METHODS)) {
      filesMock
          .when(() -> TempFile.createTempDir(eq("unzipped-gradle")))
          .thenAnswer(invocation -> Files.createTempDirectory("unzipped-gradle-real"));

      var event = new DepsConflictResolutionRequested(ORG_1_ID, APP_IMPORT_1_ID);
      subject.accept(event);
    }

    verify(importServiceMock).findById(APP_IMPORT_1_ID);
    verify(importStateServiceMock)
        .updateState(APP_IMPORT_1_ID, GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS);
    verify(importServiceMock)
        .downloadAndUnzipGradleBuildPostConversion(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), any(Path.class));
    verify(depsConflictResolverMock).apply(any(), eq(POJA_7));
    verify(importStateServiceMock)
        .updateState(
            eq(APP_IMPORT_1_ID),
            eq(GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED),
            argThat(
                logs ->
                    1 == logs.size() && logs.getFirst().getMessage().contains("Version conflict")));

    verifyNoMoreInteractions(importServiceMock, depsConflictResolverMock, eventProducerMock);
  }

  @SneakyThrows
  @Test
  void depsConflictResolution_succeeds() {
    var resolvedDeps = List.of(REAL_WORLD_RESOLVED_GRADLE_DEPENDENCIES);
    var successResult = mock(DepsConflictResolverResult.class);
    when(successResult.status()).thenReturn(SUCCESS);
    when(successResult.data()).thenReturn(new ConflictResolutionData(resolvedDeps));
    when(depsConflictResolverMock.apply(any(), eq(POJA_7))).thenReturn(successResult);
    when(importStateServiceMock.getStatesByImportId(APP_IMPORT_1_ID))
        .thenReturn(List.of(ApplicationImportState.builder().id("in_progress_1_id").build()));

    var rootPathAt0 = new Path[1];

    doAnswer(
            invocation -> {
              Path targetDir = invocation.getArgument(2);
              var zipFile =
                  new ZipFile(
                      getResource("files/import/bt-deps-conflict/gradle-project.zip").getFile());
              fileUnzipper.apply(zipFile, targetDir);
              return null;
            })
        .when(importServiceMock)
        .downloadAndUnzipGradleBuildPostConversion(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), any(Path.class));

    try (var filesMock = mockStatic(TempFile.class, CALLS_REAL_METHODS)) {
      filesMock
          .when(() -> TempFile.createTempDir(eq("unzipped-gradle")))
          .thenAnswer(
              invocation -> {
                Path dir = Files.createTempDirectory("unzipped-gradle-real");
                rootPathAt0[0] = dir;
                return dir;
              });

      var event = new DepsConflictResolutionRequested(ORG_1_ID, APP_IMPORT_1_ID);
      subject.accept(event);
    }

    verify(importServiceMock).findById(APP_IMPORT_1_ID);
    verify(importStateServiceMock)
        .updateState(APP_IMPORT_1_ID, GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS);
    verify(importServiceMock)
        .downloadAndUnzipGradleBuildPostConversion(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), any(Path.class));
    verify(depsConflictResolverMock).apply(any(), eq(POJA_7));
    verify(importStateServiceMock).getStatesByImportId(APP_IMPORT_1_ID);
    verify(importLogServiceMock).saveAll(any(), eq("in_progress_1_id"));
    verify(importServiceMock)
        .uploadPostConflictResolutionZippedBuildToolFiles(
            ORG_1_ID, APP_IMPORT_1_ID, rootPathAt0[0]);
    verify(importStateServiceMock)
        .updateState(APP_IMPORT_1_ID, GRADLE_BUILD_FILE_CONFLICTS_RESOLVED);
    verify(eventProducerMock)
        .accept(List.of(new PojaConfGenFromGradleRequested(APP_IMPORT_1_ID, ORG_1_ID)));

    verifyNoMoreInteractions(
        importServiceMock,
        depsConflictResolverMock,
        eventProducerMock,
        importStateServiceMock,
        importLogServiceMock);

    var gradleProject = new GradleProject(rootPathAt0[0]);
    var actualDeps = defaultGradleBuildExtractor().extract(gradleProject).value().dependencies();

    var sortedResolvedDeps = new ArrayList<>(resolvedDeps);
    var sortedActualDeps = new ArrayList<>(actualDeps);
    sortedResolvedDeps.sort(DEP_COMPARATOR);
    sortedActualDeps.sort(DEP_COMPARATOR);

    assertEquals(sortedResolvedDeps, sortedActualDeps);
  }
}
