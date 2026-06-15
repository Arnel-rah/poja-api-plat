package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.ApplicationImportStateEnum.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobName.PRE_TRANSFORMATION_TEST;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobStatus.FAILURE;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobStatus.SUCCESS;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_6_ID;
import static api.poja.io.integration.conf.utils.TestMocks.APP_IMPORT_7_ID;
import static api.poja.io.integration.conf.utils.TestMocks.A_GITHUB_APP_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_REPO_ID;
import static api.poja.io.integration.conf.utils.TestMocks.applicationImport1States;
import static api.poja.io.integration.conf.utils.TestMocks.applicationImport6;
import static api.poja.io.integration.conf.utils.TestMocks.applicationImport6States;
import static api.poja.io.integration.conf.utils.TestMocks.org1_applicationImports;
import static api.poja.io.integration.conf.utils.TestUtils.anApiClient;
import static api.poja.io.integration.conf.utils.TestUtils.anAppImportApiClient;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsApiException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.math.BigDecimal.ONE;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.ImporterApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.ApplicationImportState;
import api.poja.io.endpoint.rest.model.UpdateApplicationImportStatesRequestBody;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ApplicationImportIT extends MockedThirdParties {

  private ApiClient aGithubActionApiClient() {
    return anAppImportApiClient(A_GITHUB_APP_TOKEN, port);
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
    when(githubComponentMock.getRepositoryIdByAppToken(A_GITHUB_APP_TOKEN))
        .thenReturn(Optional.of(POJA_APPLICATION_REPO_ID));
  }

  @SneakyThrows
  @Test
  void applicationImport_canBe_listed_per_orgId() {
    var apiClient = anApiClient(JOE_DOE_TOKEN, port);
    var api = new ImporterApi(apiClient);

    var actual = api.getOrganizationApplicationImports(ORG_1_ID, 1, 5).getData();

    assertThat(actual)
        .usingRecursiveComparison()
        .ignoringCollectionOrder()
        .isEqualTo(org1_applicationImports());
  }

  @SneakyThrows
  @Test
  void applicationImport_canBe_retrieved() {
    var apiClient = anApiClient(JOE_DOE_TOKEN, port);
    var api = new ImporterApi(apiClient);

    var actual = api.getApplicationImportById(ORG_1_ID, APP_IMPORT_6_ID);

    assertEquals(applicationImport6(), actual);
  }

  @SneakyThrows
  @Test
  void applicationImportState_canBe_listed() {
    var apiClient = anApiClient(JOE_DOE_TOKEN, port);
    var api = new ImporterApi(apiClient);

    var actual1 = api.getApplicationImportStates(ORG_1_ID, APP_IMPORT_1_ID).getData();
    var actual6 = api.getApplicationImportStates(ORG_1_ID, APP_IMPORT_6_ID).getData();

    assertEquals(
        applicationImport1States().stream()
            .sorted(comparing(ApplicationImportState::getTimestamp).reversed())
            .toList(),
        actual1);
    assertEquals(
        applicationImport6States().stream()
            .sorted(comparing(ApplicationImportState::getTimestamp).reversed())
            .toList(),
        actual6);
  }

  @Test
  void update_state_ok() throws ApiException {
    var aGithubActionApiClient = aGithubActionApiClient();
    var appImportStateApi = new ImporterApi(aGithubActionApiClient);
    var reqBody =
        new UpdateApplicationImportStatesRequestBody()
            .jobName(PRE_TRANSFORMATION_TEST)
            .repoName("dummy")
            .repoOwner("dummy")
            .jobStatus(SUCCESS)
            .runId("dummy")
            .attemptNb(ONE);

    var latestState =
        requireNonNull(
                appImportStateApi
                    .updateApplicationImportStates(ORG_1_ID, APP_IMPORT_7_ID, reqBody)
                    .getData())
            .getFirst();

    assertEquals(
        PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL, latestState.getProgressionStatus());
  }

  // TODO: Reimplement and enable when getStates endpoint is implemented
  @Disabled
  @Test
  void update_state_ko() {
    var aGithubActionApiClient = aGithubActionApiClient();
    var appImportStateApi = new ImporterApi(aGithubActionApiClient);
    var reqBody =
        new UpdateApplicationImportStatesRequestBody()
            .jobName(PRE_TRANSFORMATION_TEST)
            .repoName("dummy")
            .repoOwner("dummy")
            .jobStatus(FAILURE)
            .runId("dummy")
            .attemptNb(ONE);

    assertThrowsApiException(
        () -> appImportStateApi.updateApplicationImportStates(ORG_1_ID, APP_IMPORT_7_ID, reqBody),
        "{"
            + "\"type\":\"500 INTERNAL_SERVER_ERROR\","
            + "\"message\":\"Pre transformation test run failed for import = "
            + APP_IMPORT_7_ID
            + "\"}");
  }
}
