package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.FORMATTED_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.ExtendedBucketComponent.INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_SUCCESSFUL;
import static api.poja.io.service.event.AppImportPojaConfUploadedServiceTest.hasBranch;
import static api.poja.io.service.event.PojaConfUploadedServiceTest.hasNewCommit;
import static api.poja.io.service.git.GitUtils.unsignedCommitAsBot;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.CodeFormattingRequested;
import api.poja.io.endpoint.event.model.PostTransformationTestRunRequested;
import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.file.TempFile;
import api.poja.io.model.importer.format.CodeFormatter;
import api.poja.io.model.importer.format.CodeFormattingResult;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.git.GitUtils;
import api.poja.io.service.github.GithubService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class CodeFormattingRequestedServiceTest {

  static final String GH_TOKEN = "gh_token";
  final FileWriter fileWriter = new FileWriter(new ExtensionGuesser());
  final FileUnzipper fileUnzipper = new FileUnzipper(fileWriter);

  CodeFormatter formatterMock;
  ApplicationImportService importServiceMock;
  ApplicationImportStateService stateServiceMock;
  GithubService githubServiceMock;
  AppInstallationService appInstallationServiceMock;
  EventProducer<PostTransformationTestRunRequested> eventProducerMock;
  CodeFormattingRequestedService subject;

  @BeforeEach
  void setUp() {
    importServiceMock = mock(ApplicationImportService.class);
    stateServiceMock = mock(ApplicationImportStateService.class);
    githubServiceMock = mock(GithubService.class);
    appInstallationServiceMock = mock(AppInstallationService.class);
    eventProducerMock = mock(EventProducer.class);

    when(importServiceMock.findById(APP_IMPORT_1_ID)).thenReturn(Optional.of(pendingAppImport()));
    when(appInstallationServiceMock.getById(any())).thenReturn(appInstallation());
    when(githubServiceMock.getInstallationToken(anyLong(), any())).thenReturn(GH_TOKEN);
  }

  @Test
  void nonExistent_appImport_shouldBe_skipped() {
    formatterMock = mock();
    var importId = "NonExistentAppImportId";
    var event = new CodeFormattingRequested(ORG_1_ID, importId);

    subject =
        new CodeFormattingRequestedService(
            formatterMock,
            importServiceMock,
            stateServiceMock,
            githubServiceMock,
            appInstallationServiceMock,
            eventProducerMock);

    subject.accept(event);

    verify(importServiceMock).findById(importId);
    verifyNoMoreInteractions(importServiceMock, formatterMock, eventProducerMock);
  }

  @SneakyThrows
  @Test
  void code_formatting_fails() {
    formatterMock = mock();
    var errors =
        List.of(
            ApplicationImportLog.error("Failed to format file Test.java: syntax error"),
            ApplicationImportLog.error("Failed to format file Main.java: parse error"));

    doAnswer(
            invocation -> {
              Path targetDir = invocation.getArgument(3);
              var zipFile =
                  new ZipFile(getResource("files/import/format/unformatted-code.zip").getFile());
              fileUnzipper.apply(zipFile, targetDir);
              try (var git = Git.open(targetDir.toFile())) {
                git.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(targetDir.toFile().toURI().toString()))
                    .call();
              }
              return null;
            })
        .when(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));

    when(formatterMock.apply(any(Path.class)))
        .thenAnswer(
            invocation -> {
              Path root = invocation.getArgument(0);
              return new CodeFormattingResult(root, errors);
            });

    try (var filesMock = mockStatic(TempFile.class, CALLS_REAL_METHODS)) {
      filesMock
          .when(() -> TempFile.createTempDir(argThat(s -> s.startsWith("code-formatting-"))))
          .thenAnswer(invocation -> Files.createTempDirectory("code-formatting-test"));

      subject =
          new CodeFormattingRequestedService(
              formatterMock,
              importServiceMock,
              stateServiceMock,
              githubServiceMock,
              appInstallationServiceMock,
              eventProducerMock);

      var event = new CodeFormattingRequested(ORG_1_ID, APP_IMPORT_1_ID);
      subject.accept(event);
    }

    verify(importServiceMock).findById(APP_IMPORT_1_ID);
    verify(stateServiceMock).updateState(APP_IMPORT_1_ID, CODE_FORMATTING_IN_PROGRESS);
    verify(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));
    verify(formatterMock).apply(any(Path.class));
    verify(stateServiceMock)
        .updateState(
            eq(APP_IMPORT_1_ID),
            eq(CODE_FORMATTING_FAILED),
            argThat(
                logs ->
                    logs.size() == 2
                        && logs.getFirst().getMessage().contains("Failed to format file Test.java")
                        && logs.get(1).getMessage().contains("Failed to format file Main.java")));

    verifyNoMoreInteractions(importServiceMock, stateServiceMock, formatterMock, eventProducerMock);
  }

  @SneakyThrows
  @Test
  void formatUnformattedCode_shouldSucceed() {
    var gitRepoAt = new Git[1];
    Set<String> formattedFiles = new HashSet<>();
    doAnswer(
            invocation -> {
              Path targetDir = invocation.getArgument(3);
              var zipFile =
                  new ZipFile(getResource("files/import/format/unformatted-code.zip").getFile());
              fileUnzipper.apply(zipFile, targetDir);
              try (var git = Git.open(targetDir.toFile())) {
                gitRepoAt[0] = git;
                git.remoteAdd()
                    .setName("origin")
                    .setUri(new URIish(targetDir.toFile().toURI().toString()))
                    .call();
              }
              return null;
            })
        .when(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));

    subject =
        new CodeFormattingRequestedService(
            new CodeFormatter(),
            importServiceMock,
            stateServiceMock,
            githubServiceMock,
            appInstallationServiceMock,
            eventProducerMock);

    try (var gitMock = mockStatic(GitUtils.class, CALLS_REAL_METHODS)) {
      gitMock
          .when(() -> unsignedCommitAsBot(any(), any(), any(), anyBoolean()))
          .then(
              i -> {
                Git git = i.getArgument(0);
                var status = git.status().call();
                formattedFiles.addAll(status.getChanged());
                i.callRealMethod();
                return null;
              });

      var event = new CodeFormattingRequested(ORG_1_ID, APP_IMPORT_1_ID);
      assertDoesNotThrow(() -> subject.accept(event));
    }

    verify(importServiceMock).findById(APP_IMPORT_1_ID);
    verify(stateServiceMock).updateState(APP_IMPORT_1_ID, CODE_FORMATTING_IN_PROGRESS);
    verify(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));
    verify(importServiceMock)
        .uploadZippedCodeSnapshot(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), eq(FORMATTED_ZIPPED_CODE_SNAPSHOT), any());
    verify(eventProducerMock)
        .accept(eq(List.of(new PostTransformationTestRunRequested(APP_IMPORT_1_ID))));
    verify(stateServiceMock)
        .updateState(eq(APP_IMPORT_1_ID), eq(CODE_FORMATTING_SUCCESSFUL), anyList());
    verifyNoMoreInteractions(importServiceMock, stateServiceMock, eventProducerMock);

    var working = gitRepoAt[0];

    assertTrue(hasNewCommit(working, "code formatting"));
    assertTrue(hasBranch(working, pendingAppImport().ghMainBranchName()));
    assertFalse(formattedFiles.isEmpty());
  }

  @SneakyThrows
  @Test
  void unexpected_exception_should_fail() {
    formatterMock = mock();
    doAnswer(
            invocation -> {
              throw new RuntimeException("Network error");
            })
        .when(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));

    try (var filesMock = mockStatic(TempFile.class, CALLS_REAL_METHODS)) {
      filesMock
          .when(() -> TempFile.createTempDir(argThat(s -> s.startsWith("code-formatting-"))))
          .thenAnswer(invocation -> Files.createTempDirectory("code-formatting-test"));

      subject =
          new CodeFormattingRequestedService(
              formatterMock,
              importServiceMock,
              stateServiceMock,
              githubServiceMock,
              appInstallationServiceMock,
              eventProducerMock);

      var event = new CodeFormattingRequested(ORG_1_ID, APP_IMPORT_1_ID);
      subject.accept(event);
    }

    verify(importServiceMock).findById(APP_IMPORT_1_ID);
    verify(stateServiceMock).updateState(APP_IMPORT_1_ID, CODE_FORMATTING_IN_PROGRESS);
    verify(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any(Path.class));
    verify(stateServiceMock)
        .updateState(
            eq(APP_IMPORT_1_ID),
            eq(CODE_FORMATTING_FAILED),
            argThat(
                logs ->
                    logs.size() == 1
                        && logs.getFirst().getMessage().contains("Unable to format code")));

    verifyNoMoreInteractions(importServiceMock, stateServiceMock, formatterMock, eventProducerMock);
  }

  static AppInstallation appInstallation() {
    return AppInstallation.builder()
        .id("app_installation_id")
        .orgId(ORG_1_ID)
        .userId("user_id")
        .ghId(1234L)
        .ownerGithubLogin("user")
        .build();
  }
}
