package api.poja.io.service.event;

import static api.poja.io.file.ApplicationImportFileType.ANALYSIS_RESULT;
import static api.poja.io.file.ExtendedBucketComponent.APP_LANG_ANALYSIS_RESULT_FILENAME;
import static api.poja.io.file.ExtendedBucketComponent.FORMATTED_ZIPPED_CODE_SNAPSHOT;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.appInstall1;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.APP_INSTALLATION_1_ID;
import static api.poja.io.model.importer.TestMocks.domainEnvVarsWithTestValues;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_SUCCESSFUL;
import static api.poja.io.service.event.PostTransformationPingTestRequestedService.S3_PING_CI_TEMPLATE;
import static api.poja.io.service.event.PostTransformationPingTestRequestedService.S3_PING_IT_TEMPLATE;
import static java.nio.file.Files.createTempDirectory;
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.model.PostTransformationPingTestRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.importer.analyzer.lang.AppLangAnalyzerData;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.repository.model.ApplicationImportState;
import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;

class PostTransformationPingTestRequestedServiceTest {
  PostTransformationPingTestRequestedService subject;
  File mockRepo;
  final ObjectMapper objectMapper = new ObjectMapper();
  static final String IMPORT_PING_TEST_BRANCH_NAME = "import-" + APP_IMPORT_1_ID + "-ping-test";
  static final String PING_IT_FILE = "files/import/ping/PingIT.java.template";
  static final String PING_CI_FILE = "files/import/ping/ping-test-ci.yml";
  static final String LANG_ANALYSIS_FILE =
      "files/import/analyzer/lang/analysis_result/java-project-lang-analysis-result.json";

  @SneakyThrows
  void setup(ApplicationImportStateStatus stateStatus) {
    mockRepo = initLocalRepo();
    var bucketComponentMock = mock(ExtendedBucketComponent.class);
    var appImportMapperMock = mock(ApplicationImportMapper.class);
    var githubServiceMock = mock(GithubService.class);
    var appInstallationServiceMock = mock(AppInstallationService.class);
    var appImportServiceMock = mock(ApplicationImportService.class);
    var appImportStateServiceMock = mock(ApplicationImportStateService.class);
    var mockState = mock(ApplicationImportState.class);

    when(mockState.getProgressionStatus()).thenReturn(stateStatus);
    when(appImportStateServiceMock.getStatesByImportId(eq(APP_IMPORT_1_ID)))
        .thenReturn(List.of(mockState));
    when(bucketComponentMock.download(S3_PING_IT_TEMPLATE)).thenReturn(getFile(PING_IT_FILE));
    when(bucketComponentMock.download(S3_PING_CI_TEMPLATE)).thenReturn(getFile(PING_CI_FILE));
    when(appImportMapperMock.toUnknownApplication(
            eq(pendingAppImport()), eq(FORMATTED_ZIPPED_CODE_SNAPSHOT)))
        .thenReturn(unknownApplication(mockRepo));
    when(appImportServiceMock.downloadAppImportFile(
            eq(ORG_1_ID),
            eq(APP_IMPORT_1_ID),
            eq(ANALYSIS_RESULT),
            eq(APP_LANG_ANALYSIS_RESULT_FILENAME)))
        .thenReturn(getFile(LANG_ANALYSIS_FILE));

    doNothing().when(githubServiceMock).crupdateSecrets(anySet(), any(), any(), any());
    when(githubServiceMock.getInstallationToken(eq(APP_INSTALLATION_1_ID), any()))
        .thenReturn("mock_token");
    when(appInstallationServiceMock.getById(eq(GH_APP_INSTALL_1_ID))).thenReturn(appInstall1());
    when(appImportServiceMock.findById(eq(APP_IMPORT_1_ID)))
        .thenReturn(Optional.of(pendingAppImport()));

    subject =
        new PostTransformationPingTestRequestedService(
            bucketComponentMock,
            appImportServiceMock,
            appImportStateServiceMock,
            githubServiceMock,
            appInstallationServiceMock,
            appImportMapperMock,
            objectMapper);
  }

  @Test
  void accept_ok() throws IOException, GitAPIException {
    setup(POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL);
    var event = new PostTransformationPingTestRequested(APP_IMPORT_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    try (var git = Git.open(mockRepo)) {
      boolean hasImportBranch =
          git.branchList().setListMode(REMOTE).call().stream()
              .anyMatch(ref -> ref.getName().endsWith(IMPORT_PING_TEST_BRANCH_NAME));

      Path pingITPath =
          git.getRepository()
              .getWorkTree()
              .toPath()
              .resolve("src/test/java/demo/example/PingIT.java");

      Path ciFilePath =
          git.getRepository().getWorkTree().toPath().resolve(".github/workflows/ping-test-ci.yml");

      assertTrue(hasImportBranch);
      assertTrue(Files.exists(pingITPath));
      assertTrue(Files.exists(ciFilePath));

      var pingITContent = Files.readString(pingITPath);
      assertTrue(pingITContent.contains("package demo.example;"));
      assertTrue(pingITContent.contains("class PingIT extends FacadeIT"));
    }
  }

  @Test
  void accept_skips_when_already_in_progress() throws IOException, GitAPIException {
    setup(TEST_PING_ENDPOINT_IN_PROGRESS);
    var event = new PostTransformationPingTestRequested(APP_IMPORT_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    try (var git = Git.open(mockRepo)) {
      boolean hasImportBranch =
          git.branchList().setListMode(REMOTE).call().stream()
              .anyMatch(ref -> ref.getName().endsWith(IMPORT_PING_TEST_BRANCH_NAME));
      assertFalse(hasImportBranch);
    }
  }

  @Test
  void accept_skips_when_already_successful() throws IOException, GitAPIException {
    setup(TEST_PING_ENDPOINT_SUCCESSFUL);
    var event = new PostTransformationPingTestRequested(APP_IMPORT_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    try (var git = Git.open(mockRepo)) {
      boolean hasImportBranch =
          git.branchList().setListMode(REMOTE).call().stream()
              .anyMatch(ref -> ref.getName().endsWith(IMPORT_PING_TEST_BRANCH_NAME));
      assertFalse(hasImportBranch);
    }
  }

  @Test
  void accept_fails_when_package_not_found()
      throws IOException, GitAPIException, URISyntaxException {
    mockRepo = initLocalRepo();
    var bucketComponentMock = mock(ExtendedBucketComponent.class);
    var appImportMapperMock = mock(ApplicationImportMapper.class);
    var githubServiceMock = mock(GithubService.class);
    var appInstallationServiceMock = mock(AppInstallationService.class);
    var appImportServiceMock = mock(ApplicationImportService.class);
    var appImportStateServiceMock = mock(ApplicationImportStateService.class);
    var objectMapperMock = mock(ObjectMapper.class);
    var mockState = mock(ApplicationImportState.class);

    when(mockState.getProgressionStatus())
        .thenReturn(POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL);
    when(appImportStateServiceMock.getStatesByImportId(eq(APP_IMPORT_1_ID)))
        .thenReturn(List.of(mockState));

    when(appImportMapperMock.toUnknownApplication(any(), any()))
        .thenReturn(unknownApplication(mockRepo));

    var mockAnalysisData = mock(AppLangAnalyzerData.class);
    when(mockAnalysisData.mainClassPath()).thenReturn(Path.of("src/main/java/InvalidClass.java"));
    when(objectMapperMock.readValue(any(File.class), eq(AppLangAnalyzerData.class)))
        .thenReturn(mockAnalysisData);

    when(appImportServiceMock.downloadAppImportFile(any(), any(), any(), any()))
        .thenReturn(getFile(LANG_ANALYSIS_FILE));
    when(appInstallationServiceMock.getById(any())).thenReturn(appInstall1());
    when(appImportServiceMock.findById(any())).thenReturn(Optional.of(pendingAppImport()));

    subject =
        new PostTransformationPingTestRequestedService(
            bucketComponentMock,
            appImportServiceMock,
            appImportStateServiceMock,
            githubServiceMock,
            appInstallationServiceMock,
            appImportMapperMock,
            objectMapperMock);

    var event = new PostTransformationPingTestRequested(APP_IMPORT_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    verify(appImportStateServiceMock)
        .updateState(eq(APP_IMPORT_1_ID), eq(TEST_PING_ENDPOINT_IN_PROGRESS));
    verify(appImportStateServiceMock, never())
        .updateState(eq(APP_IMPORT_1_ID), eq(TEST_PING_ENDPOINT_SUCCESSFUL));
  }

  private static File initLocalRepo() throws IOException, GitAPIException, URISyntaxException {
    Path tempDir = createTempDirectory("jgit-remote-");

    File repoDir = tempDir.toFile();
    Git working = Git.init().setDirectory(repoDir).call();
    File hello = new File(repoDir, "hello.txt");
    Files.writeString(hello.toPath(), "Hello world");
    working.add().addFilepattern("hello.txt").call();
    working.commit().setMessage("Initial commit").call();
    working.remoteAdd().setName("origin").setUri(new URIish(repoDir.toURI().toString())).call();
    working.push().setRemote("origin").setPushAll().call();

    return repoDir;
  }

  private static UnknownApplication unknownApplication(File code) {
    return new UnknownApplication(code, domainEnvVarsWithTestValues());
  }

  private static File getFile(String resourcePath) {
    return new File(
        PostTransformationPingTestRequestedServiceTest.class
            .getClassLoader()
            .getResource(resourcePath)
            .getFile());
  }
}
