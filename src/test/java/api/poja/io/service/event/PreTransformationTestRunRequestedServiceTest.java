package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.appInstall1;
import static api.poja.io.integration.conf.utils.TestMocks.pendingAppImport;
import static api.poja.io.integration.conf.utils.TestUtils.APP_INSTALLATION_1_ID;
import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.model.importer.TestMocks.domainEnvVarsWithTestValues;
import static api.poja.io.service.event.PreTransformationTestRunRequestedService.S3_PRE_TRANSFORMATION_CI_DIR;
import static java.nio.file.Files.createTempDirectory;
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.model.PreTransformationTestRunRequested;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.importer.model.UnknownApplication;
import api.poja.io.repository.model.mapper.ApplicationImportMapper;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.github.GithubService;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.SneakyThrows;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;

class PreTransformationTestRunRequestedServiceTest {
  private PreTransformationTestRunRequestedService subject;
  private File mockRepo;
  private static final String IMPORT_PRE_TRANSFORMATION_BRANCH_NAME =
      "import-" + APP_IMPORT_1_ID + "-pre-transformation";

  @SneakyThrows
  void setup() {
    mockRepo = initLocalRepo();
    var bucketComponentMock = mock(ExtendedBucketComponent.class);
    var appImportMapperMock = mock(ApplicationImportMapper.class);
    var githubServiceMock = mock(GithubService.class);
    var appInstallationServiceMock = mock(AppInstallationService.class);
    var appImportServiceMock = mock(ApplicationImportService.class);
    var appImportStateServiceMock = mock(ApplicationImportStateService.class);

    when(bucketComponentMock.download(S3_PRE_TRANSFORMATION_CI_DIR))
        .thenReturn(getFile("files/import/pre-transformation-ci.yml"));
    when(appImportMapperMock.toUnknownApplication(eq(pendingAppImport())))
        .thenReturn(unknownApplication(mockRepo));
    doNothing().when(githubServiceMock).crupdateSecrets(anySet(), any(), any(), any());
    when(githubServiceMock.getInstallationToken(eq(APP_INSTALLATION_1_ID), any()))
        .thenReturn("mock_token");
    when(appInstallationServiceMock.getById(eq(GH_APP_INSTALL_1_ID))).thenReturn(appInstall1());
    when(appImportServiceMock.findById(eq(APP_IMPORT_1_ID)))
        .thenReturn(Optional.of(pendingAppImport()));

    subject =
        new PreTransformationTestRunRequestedService(
            bucketComponentMock,
            appImportMapperMock,
            githubServiceMock,
            appInstallationServiceMock,
            appImportServiceMock,
            appImportStateServiceMock);
  }

  @Test
  void accept_ok() throws IOException, GitAPIException {
    setup();
    var event = new PreTransformationTestRunRequested(APP_IMPORT_1_ID);

    assertDoesNotThrow(() -> subject.accept(event));

    try (var git = Git.open(mockRepo)) {
      boolean hasImportBranch =
          git.branchList().setListMode(REMOTE).call().stream()
              .anyMatch(ref -> ref.getName().endsWith(IMPORT_PRE_TRANSFORMATION_BRANCH_NAME));
      Path CIFilePath =
          git.getRepository()
              .getWorkTree()
              .toPath()
              .resolve(".github/workflows/pre-transformation-ci.yml");

      assertTrue(hasImportBranch);
      assertTrue(Files.exists(CIFilePath));
    }
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
}
