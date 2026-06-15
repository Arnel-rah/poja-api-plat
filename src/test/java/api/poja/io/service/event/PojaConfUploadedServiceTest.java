package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.CODE_GENERATION_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.Environment.StateEnum.UNKNOWN;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.file.ExtendedBucketComponent.getOrgBucketKey;
import static api.poja.io.file.ExtendedBucketComponent.getUserBucketKey;
import static api.poja.io.file.FileType.POJA_CONF;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestUtils.getFile;
import static api.poja.io.model.PojaVersion.POJA_6;
import static java.nio.file.Files.createTempDirectory;
import static java.util.UUID.randomUUID;
import static org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM;
import static org.eclipse.jgit.api.ResetCommand.ResetType.HARD;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.PojaConfUploaded;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.PojaVersion;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.jpa.EnvDeploymentConfRepository;
import api.poja.io.repository.jpa.EnvironmentDeploymentRepository;
import api.poja.io.repository.jpa.EnvironmentRepository;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.api.pojaSam.PojaSamApi;
import api.poja.io.service.workflows.DeploymentStateService;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class PojaConfUploadedServiceTest extends MockedThirdParties {
  // branch type to test
  public static final EnvironmentType ENV_TYPE = PREPROD;
  @Autowired PojaConfUploadedService subject;
  @MockBean ExtendedBucketComponent extendedBucketMock;
  @MockBean PojaSamApi pojaSamApiMock;
  @Autowired EnvironmentDeploymentRepository environmentDeploymentRepository;
  @Autowired DeploymentStateService deploymentStateService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired EnvironmentRepository environmentRepository;
  @Autowired EnvDeploymentConfRepository envDeploymentConfRepository;

  @Test
  void poja6() throws IOException, GitAPIException {
    EventWithRemoteRepo eventWithRemoteRepo =
        setupPojaConfUploaded(POJA_6, "mock_token", true, null);
    PojaConfUploaded confUploadedEvent = eventWithRemoteRepo.event();
    var beforePushPath = createTempDirectory("github_clone");
    Git.cloneRepository()
        .setDirectory(beforePushPath.toFile())
        .setURI(eventWithRemoteRepo.repoUri())
        .setNoCheckout(true)
        .call();

    assertDoesNotThrow(() -> subject.accept(confUploadedEvent));
    try (var git = Git.open(beforePushPath.toFile())) {
      var branchList = git.branchList().call();
      var hasNoProdYet =
          branchList.stream()
              .noneMatch(branch -> branch.getName().equals(PROD.toString().toLowerCase()));
      git.fetch().setRemote("origin").setRefSpecs(new RefSpec("+refs/heads/*:refs/heads/*")).call();
      var newBranchList = git.branchList().call();
      var hasProdAfterFetch =
          newBranchList.stream()
              .noneMatch(branch -> branch.getName().equals(PROD.toString().toLowerCase()));
      git.reset()
          .setRef(ENV_TYPE.toString().toLowerCase())
          .setMode(HARD)
          .call(); // avoid CheckoutConflictException
      git.checkout()
          .setUpstreamMode(SET_UPSTREAM)
          .setName(ENV_TYPE.toString().toLowerCase())
          .call();

      assertTrue(hasNoProdYet);
      assertTrue(hasProdAfterFetch);
      assertTrue(hasNewCommit(git, confUploadedEvent.getAppEnvDeplId()));
    }
  }

  @Test
  @Disabled
  void test_with_real_repo() throws IOException, GitAPIException {
    // if you also want to test with real sam-api, you need to change EnvConf poja-sam-api url and
    // uncomment the mocking phase in this::setup
    EventWithRemoteRepo eventWithRemoteRepo =
        setupPojaConfUploaded(POJA_6, "YOUR_TOKEN", false, "YOUR_GITHUB_HTTPS_URL");
    PojaConfUploaded confUploadedEvent = eventWithRemoteRepo.event();
    var beforePushPath = createTempDirectory("github_clone");
    Git.cloneRepository()
        .setDirectory(beforePushPath.toFile())
        .setURI(eventWithRemoteRepo.repoUri())
        .setNoCheckout(true)
        .call();

    assertDoesNotThrow(() -> subject.accept(confUploadedEvent));
    try (var git = Git.open(beforePushPath.toFile())) {
      git.fetch().setRemote("origin").setRefSpecs(new RefSpec("+refs/heads/*:refs/heads/*")).call();
      git.reset()
          .setRef(ENV_TYPE.toString().toLowerCase())
          .setMode(HARD)
          .call(); // avoid CheckoutConflictException
      git.checkout()
          .setUpstreamMode(SET_UPSTREAM)
          .setName(ENV_TYPE.toString().toLowerCase())
          .call();

      assertTrue(hasNewCommit(git, confUploadedEvent.getAppEnvDeplId()));
    }
  }

  static boolean hasNewCommit(Git git, String message) throws GitAPIException, IOException {
    var log = git.log().all().call();
    for (RevCommit revCommit : log) {
      if (revCommit.getFullMessage().contains(message)) {
        return true;
      }
    }
    return false;
  }

  private @NotNull EventWithRemoteRepo setupPojaConfUploaded(
      PojaVersion pojaVersion,
      String githubClonePushToken,
      boolean mockRepo,
      String realGithubRepoUri) {
    EventWithRemoteRepo eventWithRepo =
        getConfUploadedEvent(pojaVersion, mockRepo, realGithubRepoUri);
    PojaConfUploaded confUploadedEvent = eventWithRepo.event();
    setup(pojaVersion, confUploadedEvent, githubClonePushToken);
    return eventWithRepo;
  }

  private void setup(
      PojaVersion pojaVersion, PojaConfUploaded confUploadedEvent, String githubClonePushToken) {
    String orgBucketKey =
        getOrgBucketKey(
            confUploadedEvent.getOrgId(),
            confUploadedEvent.getAppId(),
            confUploadedEvent.getEnvironmentId(),
            POJA_CONF,
            confUploadedEvent.getFilename());
    String userBucketKey =
        getUserBucketKey(
            JOE_DOE_ID,
            confUploadedEvent.getAppId(),
            confUploadedEvent.getEnvironmentId(),
            POJA_CONF,
            confUploadedEvent.getFilename());
    when(extendedBucketMock.doesExist(eq(userBucketKey))).thenReturn(false);
    when(extendedBucketMock.doesExist(eq(orgBucketKey))).thenReturn(true);
    when(extendedBucketMock.download(eq(orgBucketKey)))
        .thenReturn(
            getFile(
                switch (pojaVersion) {
                  case POJA_1 -> "files/poja_1.yml";
                  case POJA_2 -> "files/poja_2.yml";
                  case POJA_3 -> "files/poja_3.yml";
                  case POJA_4 -> "files/poja_4.yml";
                  case POJA_5 -> "files/poja_5.yml";
                  case POJA_6 -> "files/poja_6.yml";
                  case POJA_7 -> "files/poja_7.yml";
                  case POJA_8 -> "files/poja_8.yml";
                  case POJA_9 -> "files/poja_9.yml";
                }));
    var cdComputeFile =
        switch (pojaVersion) {
          case POJA_5 -> "poja-templates/cd-compute-1.3.3.yml";
          case POJA_6 -> "poja-templates/cd-compute-2.0.1.yml";
          default -> "poja-templates/cd-compute.yml";
        };
    when(extendedBucketMock.download(eq(cdComputeFile)))
        .thenReturn(getFile("files/cd-compute.yml"));
    when(githubComponentMock.getAppInstallationToken(eq(12344L), any()))
        .thenReturn(githubClonePushToken);
    when(pojaSamApiMock.genCode(eq(pojaVersion), any()))
        .thenReturn(getFile("files/poja-base-prod.zip"));
  }

  @SneakyThrows
  private EventWithRemoteRepo getConfUploadedEvent(
      PojaVersion pojaVersion, boolean mockRepo, String realRepoGithubUri) {
    String appId = randomUUID().toString();
    var app =
        applicationRepository.save(
            Application.builder()
                .id(appId)
                .name(appId)
                .orgId(JOE_DOE_MAIN_ORG_ID)
                .installationId(GH_APP_INSTALL_1_ID)
                .build());
    String envId = randomUUID().toString();
    var environment =
        environmentRepository.save(
            Environment.builder()
                .id(envId)
                .applicationId(appId)
                .environmentType(ENV_TYPE)
                .state(UNKNOWN)
                .build());
    var environmentId = environment.getId();
    String uri = mockRepo ? initLocalRepo() : realRepoGithubUri;
    applicationRepository.save(app.toBuilder().githubRepositoryUrl(uri).build());
    var depl =
        environmentDeploymentRepository.save(
            AppEnvironmentDeployment.builder()
                .appId(appId)
                .env(environment)
                .envDeplConfId("env_1_depl_files_1_id")
                .ghRepoName("mock")
                .ghRepoOwnerName("mock")
                .ghCommitMessage("chore: mock")
                .ghCommitSha("shashasha")
                .build());
    deploymentStateService.save(depl.getAppId(), depl.getId(), CODE_GENERATION_IN_PROGRESS);
    var deplConf =
        envDeploymentConfRepository.save(
            EnvDeploymentConf.builder()
                .id(randomUUID().toString())
                .envId(environmentId)
                .computePermissionStackFileKey("compute-permission.yml")
                .buildTemplateFile("template.yml")
                .creationDatetime(Instant.now())
                .pojaConfFileKey("filename")
                .build());

    // todo: Test the 'sourceBranch' param
    return new EventWithRemoteRepo(
        PojaConfUploaded.builder()
            .envDeplConfId(deplConf.getId())
            .pojaVersion(pojaVersion)
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .filename(randomUUID().toString())
            .appId(appId)
            .environmentId(environmentId)
            .appEnvDeplId(depl.getId())
            .build(),
        uri);
  }

  private record EventWithRemoteRepo(PojaConfUploaded event, String repoUri) {}

  private static String initLocalRepo() throws IOException, GitAPIException, URISyntaxException {
    Path tempDir = createTempDirectory("jgit-remote-");

    File repoDir = tempDir.toFile();
    Git working = Git.init().setDirectory(repoDir).call();
    File hello = new File(repoDir, "hello.txt");
    Files.writeString(hello.toPath(), "Hello world");
    working.add().addFilepattern("hello.txt").call();
    working.commit().setMessage("Initial commit").call();
    working.remoteAdd().setName("origin").setUri(new URIish(repoDir.toURI().toString())).call();
    working.push().setRemote("origin").setPushAll().call();

    return repoDir.toURI().toString();
  }
}
