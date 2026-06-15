package api.poja.io.service.event;

import static api.poja.io.file.ExtendedBucketComponent.CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.file.ExtendedBucketComponent.INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_GENERATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATION_IN_PROGRESS;
import static api.poja.io.service.event.BuildToolConversionRequestedServiceTest.GH_TOKEN;
import static api.poja.io.service.event.BuildToolConversionRequestedServiceTest.appInstallation_1;
import static api.poja.io.service.event.PojaConfUploadedServiceTest.hasNewCommit;
import static api.poja.io.service.git.GitUtils.gitRm;
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportPojaConfUploaded;
import api.poja.io.endpoint.event.model.CodeFormattingRequested;
import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.api.pojaSam.PojaSamApi;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import api.poja.io.service.git.GitUtils;
import api.poja.io.service.github.GithubService;
import api.poja.io.service.pojaConfHandler.AbstractPojaConfUploadedHandler;
import api.poja.io.service.pojaConfHandler.PojaConfUploadedHandler;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppImportPojaConfUploadedServiceTest {
  static final String HELLO_WORLD_FILENAME = "hello.txt";

  final FileWriter fileWriter = new FileWriter(new ExtensionGuesser());
  final FileUnzipper unzipper = new FileUnzipper(fileWriter);

  AppImportPojaConfUploadedService subject;
  ApplicationImportService importServiceMock;
  ApplicationImportStateService importStateServiceMock;
  PojaConfFileMapper pojaConfMapperMock;
  PojaSamApi pojaSamApiMock;
  AppInstallationService appInstallationServiceMock;
  GithubService githubServiceMock;
  PojaConfUploadedHandler pojaConfUploadedHandlerMock;
  EventProducer<CodeFormattingRequested> eventProducerMock;

  @BeforeEach
  void setup() {
    importServiceMock = mock();
    importStateServiceMock = mock();
    pojaConfMapperMock = mock();
    pojaSamApiMock = mock();
    appInstallationServiceMock = mock();
    githubServiceMock = mock();
    pojaConfUploadedHandlerMock = mock();
    eventProducerMock = mock();

    when(importServiceMock.findById(eq(APP_IMPORT_1_ID)))
        .thenReturn(Optional.of(pendingAppImport()));
    when(importServiceMock.downloadAppImportPojaConf(eq(ORG_1_ID), eq(APP_IMPORT_1_ID)))
        .thenReturn(null);
    when(pojaConfMapperMock.readAsDomain(any())).thenReturn(domainPojaConf());
    when(pojaSamApiMock.genCode(any(), any())).thenReturn(getFile("files/poja-base-prod.zip"));
    when(appInstallationServiceMock.getById(any())).thenReturn(appInstallation_1());
    when(githubServiceMock.getInstallationToken(anyLong(), any())).thenReturn(GH_TOKEN);
    doNothing().when(pojaConfUploadedHandlerMock).configureCdCompute(any(), any());
    doNothing()
        .when(importServiceMock)
        .uploadZippedCodeSnapshot(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(INTEGRATED_GENERATED_CODE_ZIPPED_CODE_SNAPSHOT),
            any());
    doAnswer(
            i -> {
              Path target = i.getArgument(3);
              initLocalRepo(target);
              return null;
            })
        .when(importServiceMock)
        .downloadAndUnzipCodeSnapshot(
            eq(ORG_1_ID), eq(APP_IMPORT_1_ID), eq(CONVERTED_TO_GRADLE_ZIPPED_CODE_SNAPSHOT), any());
  }

  @SneakyThrows
  @Test
  void generatedCodeIntegratedSuccessfully_shouldSave_successState() {
    try (var gitMock = mockStatic(GitUtils.class, CALLS_REAL_METHODS)) {
      var gitRepoAt0 = new Git[1];
      gitMock
          .when(() -> GitUtils.pushAndCheckResult(any(), any(), any()))
          .then(
              invocation -> {
                Git git = invocation.getArgument(2);
                gitRepoAt0[0] = git;
                return invocation.callRealMethod();
              });

      subject =
          new AppImportPojaConfUploadedService(
              importServiceMock,
              importStateServiceMock,
              appInstallationServiceMock,
              githubServiceMock,
              pojaConfMapperMock,
              pojaSamApiMock,
              unzipper,
              eventProducerMock);

      var event = new AppImportPojaConfUploaded(ORG_1_ID, APP_IMPORT_1_ID);

      assertDoesNotThrow(() -> subject.accept(event));

      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(CODE_GENERATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(GENERATED_CODE_INTEGRATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(GENERATED_CODE_INTEGRATED));
      verify(eventProducerMock)
          .accept(eq(List.of(new CodeFormattingRequested(ORG_1_ID, APP_IMPORT_1_ID))));

      var working = gitRepoAt0[0];

      assertTrue(hasBranch(working, pendingAppImport().ghMainBranchName()));
      assertTrue(hasNewCommit(working, "generated code integration"));

      var ciPath =
          working.getRepository().getWorkTree().toPath().resolve(".github/workflows/ci.yml");
      var cdPath =
          working
              .getRepository()
              .getWorkTree()
              .toPath()
              .resolve(".github/workflows/cd-compute.yml");

      assertFalse(Files.exists(ciPath));
      assertFalse(Files.exists(cdPath));
    }
  }

  @Test
  void unexpectedFileDeletion_shouldSave_failedState() {
    try (var pojaUploadedStaticMock =
        mockStatic(AbstractPojaConfUploadedHandler.class, CALLS_REAL_METHODS)) {
      pojaUploadedStaticMock
          .when(() -> AbstractPojaConfUploadedHandler.rmPojaFilesAndAddChanges(any(), anyBoolean()))
          .then(
              invocation -> {
                Git git = invocation.getArgument(0);
                gitRm(git, List.of(Path.of(HELLO_WORLD_FILENAME)));
                return invocation.callRealMethod();
              });

      subject =
          new AppImportPojaConfUploadedService(
              importServiceMock,
              importStateServiceMock,
              appInstallationServiceMock,
              githubServiceMock,
              pojaConfMapperMock,
              pojaSamApiMock,
              unzipper,
              eventProducerMock);

      var event = new AppImportPojaConfUploaded(ORG_1_ID, APP_IMPORT_1_ID);

      assertDoesNotThrow(() -> subject.accept(event));

      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(CODE_GENERATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(GENERATED_CODE_INTEGRATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(
              eq(APP_IMPORT_1_ID),
              eq(GENERATED_CODE_INTEGRATION_FAILED),
              argThat(
                  logs ->
                      logs.size() == 1
                          && logs.getFirst()
                              .getMessage()
                              .equals(
                                  "Unexpected deletion of following files: \n"
                                      + HELLO_WORLD_FILENAME
                                      + "\n")
                          && logs.getFirst().getType().equals(ERROR)));

      verifyNoMoreInteractions(eventProducerMock);
    }
  }

  @Test
  void unexpectedFileChanges_shouldSave_failedState() {
    try (var pojaUploadedStaticMock =
        mockStatic(AbstractPojaConfUploadedHandler.class, CALLS_REAL_METHODS)) {
      pojaUploadedStaticMock
          .when(() -> AbstractPojaConfUploadedHandler.rmPojaFilesAndAddChanges(any(), anyBoolean()))
          .then(
              invocation -> {
                Git git = invocation.getArgument(0);
                var dir = git.getRepository().getWorkTree();
                fileWriter.write("...".getBytes(), dir, HELLO_WORLD_FILENAME);
                return invocation.callRealMethod();
              });

      subject =
          new AppImportPojaConfUploadedService(
              importServiceMock,
              importStateServiceMock,
              appInstallationServiceMock,
              githubServiceMock,
              pojaConfMapperMock,
              pojaSamApiMock,
              unzipper,
              eventProducerMock);

      var event = new AppImportPojaConfUploaded(ORG_1_ID, APP_IMPORT_1_ID);

      assertDoesNotThrow(() -> subject.accept(event));

      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(CODE_GENERATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(eq(APP_IMPORT_1_ID), eq(GENERATED_CODE_INTEGRATION_IN_PROGRESS));
      verify(importStateServiceMock)
          .updateState(
              eq(APP_IMPORT_1_ID),
              eq(GENERATED_CODE_INTEGRATION_FAILED),
              argThat(
                  logs ->
                      logs.size() == 1
                          && logs.getFirst()
                              .getMessage()
                              .equals(
                                  "Unexpected change of following files: \n"
                                      + HELLO_WORLD_FILENAME
                                      + "\n")
                          && logs.getFirst().getType().equals(ERROR)));

      verifyNoMoreInteractions(eventProducerMock);
    }
  }

  static boolean hasBranch(Git git, String branchName) throws GitAPIException {
    var branches = git.branchList().setListMode(ALL).call();

    for (var ref : branches) {
      if (ref.getName().contains(branchName)) {
        return true;
      }
    }
    return false;
  }

  static PojaConf domainPojaConf() {
    return PojaConf7.builder().build();
  }

  static void initLocalRepo(Path target) throws IOException, GitAPIException, URISyntaxException {
    File repoDir = target.toFile();
    Git working = Git.init().setDirectory(repoDir).call();
    File hello = new File(repoDir, HELLO_WORLD_FILENAME);
    Files.writeString(hello.toPath(), "Hello world");
    working.add().addFilepattern(HELLO_WORLD_FILENAME).call();
    working.commit().setMessage("Initial commit").call();
    working.remoteAdd().setName("origin").setUri(new URIish(repoDir.toURI().toString())).call();
    working.push().setRemote("origin").setPushAll().call();
  }
}
