package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.DEPLOYMENT_WORKFLOW_FAILED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.DEPLOYMENT_WORKFLOW_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_INITIATED;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.DeploymentStateEnum.TEMPLATE_FILE_CHECK_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static api.poja.io.endpoint.rest.model.GithubWorkflowStateEnum.FAILED;
import static api.poja.io.endpoint.rest.model.GithubWorkflowStateEnum.RUNNING;
import static api.poja.io.integration.conf.utils.TestMocks.A_GITHUB_APP_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_REPO_ID;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf1;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.ApplicationApi;
import api.poja.io.endpoint.rest.api.EnvDeployApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.AppEnvDeployment;
import api.poja.io.endpoint.rest.model.DeploymentState;
import api.poja.io.endpoint.rest.model.GithubMeta;
import api.poja.io.endpoint.rest.model.GithubMetaCommit;
import api.poja.io.endpoint.rest.model.GithubMetaRepo;
import api.poja.io.endpoint.rest.model.GithubUserMeta;
import api.poja.io.endpoint.rest.model.GithubWorkflowState;
import api.poja.io.endpoint.rest.model.GithubWorkflowStateEnum;
import api.poja.io.endpoint.rest.model.GithubWorkflowStateResponse;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class ApplicationEnvironmentDeploymentsIT extends MockedThirdParties {
  public static final String DEPLOYMENT_1_ID = "deployment_1_id";
  public static final String DEPLOYMENT_13_ID = "deployment_13_id";
  @MockBean ExtendedBucketComponent extendedBucketComponentMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  private ApiClient aGithubActionApiClient() {
    return TestUtils.anApiClient(A_GITHUB_APP_TOKEN, port);
  }

  @BeforeEach
  void setup() throws IOException {
    when(githubComponentMock.getRepositoryIdByAppToken(A_GITHUB_APP_TOKEN))
        .thenReturn(Optional.of(POJA_APPLICATION_REPO_ID));
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
    setUpExtendedBucketComponentMock(extendedBucketComponentMock);
  }

  void setUpExtendedBucketComponentMock(ExtendedBucketComponent extendedBucketComponent)
      throws IOException {
    when(extendedBucketComponent.doesExist(any())).thenReturn(true);
    when(extendedBucketComponent.download(any()))
        .thenReturn(new ClassPathResource("files/poja_1.yml").getFile());
  }

  @Test
  void read_deployments_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new ApplicationApi(apiClient);

    var prodDepls =
        api.getApplicationDeployments(
                JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, PROD, null, null, 1, 10)
            .getData();
    var preprodDepls =
        api.getApplicationDeployments(
                JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, PREPROD, null, null, 1, 10)
            .getData();
    // allDepls pageSize is set to max pageSize=500 in order to get all prod and preprod depls
    // although testData should not reach 500
    var allDepls =
        api.getApplicationDeployments(
                JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, null, null, null, 1, 500)
            .getData();
    var instantFilteredDepls =
        api.getApplicationDeployments(
                JOE_DOE_MAIN_ORG_ID,
                OTHER_POJA_APPLICATION_ID,
                null,
                Instant.parse("2024-08-01T00:00:00Z"),
                Instant.parse("2024-08-01T23:59:00Z"),
                1,
                10)
            .getData();

    assertTrue(requireNonNull(allDepls).containsAll(requireNonNull(prodDepls)));
    assertTrue(allDepls.containsAll(requireNonNull(preprodDepls)));
    assertTrue(preprodDepls.contains(preprodDepl()));
    assertFalse(preprodDepls.contains(prodDepl()));
    assertTrue(prodDepls.contains(prodDepl()));
    assertFalse(prodDepls.contains(preprodDepl()));
    assertTrue(requireNonNull(instantFilteredDepls).contains(prodDepl()));
    assertFalse(instantFilteredDepls.contains(preprodDepl()));
  }

  @Test
  void read_deployments_is_well_ordered_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new ApplicationApi(apiClient);

    var allDepls =
        api.getApplicationDeployments(
            JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, null, null, null, 1, 10);
    List<AppEnvDeployment> data = allDepls.getData();
    var sorted =
        data.stream()
            .sorted(Comparator.comparing(AppEnvDeployment::getCreationDatetime).reversed())
            .toList();

    assertEquals(sorted, data);
  }

  @Test
  void read_deployment_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new ApplicationApi(apiClient);

    var actual =
        api.getApplicationDeployment(
            JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, DEPLOYMENT_1_ID);

    assertEquals(prodDepl(), actual);
  }

  @Test
  void read_env_config_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new ApplicationApi(apiClient);
    var expected = new OneOfPojaConf(getValidPojaConf1());

    var actual =
        api.getApplicationDeploymentConfig(
            JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, DEPLOYMENT_1_ID);
    assertEquals(expected, actual);
  }

  @Test
  void read_deployment_states_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new ApplicationApi(apiClient);

    var actual =
        api.getApplicationDeploymentStates(
            JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, DEPLOYMENT_1_ID);
    assertEquals(deploymentState(), actual);
  }

  @Test
  void crupdate_github_workflow_state_ok() throws ApiException {
    var aGithubActionApiClient = aGithubActionApiClient();
    var anUserApiClient = anApiClient();
    var envDeployApi = new EnvDeployApi(aGithubActionApiClient);
    var applicationApi = new ApplicationApi(anUserApiClient);

    var inProgressState =
        envDeployApi.updateGithubWorkflowState(
            "gh_user_one", "gh_repo", githubWorkflowState(RUNNING, null));
    var failedState =
        envDeployApi.updateGithubWorkflowState(
            "gh_user_one", "gh_repo", githubWorkflowState(FAILED, DEPLOYMENT_13_ID));
    var updatedState =
        applicationApi.getApplicationDeploymentStates(
            JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, DEPLOYMENT_13_ID);

    assertEquals(githubWorkflowStateResponse(RUNNING), inProgressState);
    assertEquals(githubWorkflowStateResponse(FAILED), failedState);
    assertNotNull(updatedState);
    assertEquals(DEPLOYMENT_WORKFLOW_IN_PROGRESS, updatedState.getProgressionStatus());
    assertNotNull(updatedState.getNextState());
    assertEquals(DEPLOYMENT_WORKFLOW_FAILED, updatedState.getNextState().getProgressionStatus());
  }

  GithubWorkflowState githubWorkflowState(
      GithubWorkflowStateEnum stateEnum, String appEnvDeploymentId) {
    return new GithubWorkflowState()
        .commitMessage("jcloudify: deployment ID: " + DEPLOYMENT_13_ID)
        .commitSha("rwoi12k3")
        .commitAuthorName("jcloudify[bot]")
        .environmentType(PROD)
        .appEnvDeploymentId(appEnvDeploymentId)
        .status(stateEnum);
  }

  GithubWorkflowStateResponse githubWorkflowStateResponse(GithubWorkflowStateEnum state) {
    return new GithubWorkflowStateResponse()
        .commitMessage("jcloudify: deployment ID: " + DEPLOYMENT_13_ID)
        .commitSha("rwoi12k3")
        .commitAuthorName("jcloudify[bot]")
        .environmentType(PROD)
        .appEnvDeploymentId(DEPLOYMENT_13_ID)
        .status(state);
  }

  DeploymentState deploymentState() {
    return new DeploymentState()
        .id("other_poja_application_deployment_state_1_id")
        .timestamp(Instant.parse("2024-09-01T08:50:00Z"))
        .progressionStatus(TEMPLATE_FILE_CHECK_IN_PROGRESS)
        .executionType(ASYNCHRONOUS)
        .nextState(
            new DeploymentState()
                .id("other_poja_application_deployment_state_2_id")
                .timestamp(Instant.parse("2024-09-01T08:51:00Z"))
                .progressionStatus(INDEPENDENT_STACKS_DEPLOYMENT_INITIATED)
                .executionType(ASYNCHRONOUS)
                .nextState(
                    new DeploymentState()
                        .id("other_poja_application_deployment_state_3_id")
                        .timestamp(Instant.parse("2024-09-01T08:51:15Z"))
                        .progressionStatus(INDEPENDENT_STACKS_DEPLOYMENT_IN_PROGRESS)
                        .executionType(ASYNCHRONOUS)
                        .nextState(
                            new DeploymentState()
                                .id("other_poja_application_deployment_state_4_id")
                                .timestamp(Instant.parse("2024-09-01T08:51:35Z"))
                                .progressionStatus(INDEPENDENT_STACKS_DEPLOYED)
                                .executionType(ASYNCHRONOUS)
                                .nextState(
                                    new DeploymentState()
                                        .id("other_poja_application_deployment_state_5_id")
                                        .timestamp(Instant.parse("2024-09-01T08:52:00Z"))
                                        .progressionStatus(COMPUTE_STACK_DEPLOYMENT_IN_PROGRESS)
                                        .executionType(ASYNCHRONOUS)
                                        .nextState(null)))));
  }

  GithubUserMeta johnDoeMetaGhUser() {
    return new GithubUserMeta()
        .login("johndoe")
        .email("john.doe@example.com")
        .name("John Doe")
        .githubId("12345678")
        .avatarUrl(URI.create("https://avatars.githubusercontent.com/u/12345678"))
        .isJcBot(false);
  }

  GithubMeta johnDoeMetaGh() {
    GithubMetaRepo repo = new GithubMetaRepo().ownerName("poja-org").name("repo1");

    GithubMetaCommit commit =
        new GithubMetaCommit()
            .branch("prod")
            .committer(johnDoeMetaGhUser())
            .message("Initial deployment")
            .sha("abc123def456")
            .url(URI.create("https://github.com/poja-org/repo1/commit/abc123def456"));

    return new GithubMeta().commit(commit).repo(repo);
  }

  AppEnvDeployment prodDepl() {
    return new AppEnvDeployment()
        .id("deployment_1_id")
        .confId("env_1_depl_files_1_id")
        .githubMeta(johnDoeMetaGh())
        .applicationId("other_poja_application_id")
        .environmentId("other_poja_application_environment_id")
        .deployedUrl(URI.create("https://example.com/deploy1"))
        .creationDatetime(Instant.parse("2024-08-01T10:15:00Z"));
  }

  GithubUserMeta janeSmithMetaGhUser() {
    return new GithubUserMeta()
        .login("janesmith")
        .email("jane.smith@example.com")
        .name("Jane Smith")
        .githubId("87654321")
        .avatarUrl(URI.create("https://avatars.githubusercontent.com/u/87654321"))
        .isJcBot(false);
  }

  GithubMeta janeSmithMetaGh() {
    GithubMetaRepo repo = new GithubMetaRepo().ownerName("poja-org").name("repo2");

    GithubMetaCommit commit =
        new GithubMetaCommit()
            .branch("preprod")
            .committer(janeSmithMetaGhUser())
            .message("Bug fixes")
            .sha("789ghi012jkl")
            .url(URI.create("https://github.com/poja-org/repo2/commit/789ghi012jkl"));

    return new GithubMeta().commit(commit).repo(repo);
  }

  AppEnvDeployment preprodDepl() {
    return new AppEnvDeployment()
        .id("deployment_2_id")
        .confId("env_1_depl_files_2_id")
        .githubMeta(janeSmithMetaGh())
        .applicationId("other_poja_application_id")
        .environmentId("other_poja_application_environment_2_id")
        .deployedUrl(URI.create("https://example.com/deploy2"))
        .creationDatetime(Instant.parse("2024-08-02T14:30:00Z"));
  }

  GithubUserMeta samBrownMetaGhUser() {
    return new GithubUserMeta()
        .login("sambrown")
        .email("sam.brown@example.com")
        .name("Sam Brown")
        .githubId("98765432")
        .avatarUrl(URI.create("https://avatars.githubusercontent.com/u/98765432"))
        .isJcBot(false);
  }

  GithubMeta samBrownMetaGh() {
    GithubMetaRepo repo = new GithubMetaRepo().ownerName("poja-org").name("repo3");

    GithubMetaCommit commit =
        new GithubMetaCommit()
            .branch("prod")
            .committer(samBrownMetaGhUser())
            .message("Final deployment")
            .sha("mno345pqr678")
            .url(URI.create("https://github.com/poja-org/repo3/commit/mno345pqr678"));

    return new GithubMeta().commit(commit).repo(repo);
  }

  AppEnvDeployment archivedDepl() {
    return new AppEnvDeployment()
        .id("deployment_3_id")
        .githubMeta(samBrownMetaGh())
        .applicationId("other_poja_application_id")
        .environmentId("archived_other_poja_app_env_id")
        .deployedUrl(URI.create("https://example.com/deploy3"))
        .creationDatetime(Instant.parse("2024-08-02T09:00:00Z"));
  }
}
