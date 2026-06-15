package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.Environment.StateEnum.UNKNOWN;
import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.ACTIVE;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.pojaAppProdEnvironment;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsNotFoundException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.EnvironmentApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.CrupdateEnvironmentsRequestBody;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationEnvironmentIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
  }

  @Test
  void list_environments_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    EnvironmentApi api = new EnvironmentApi(joeDoeClient);

    var actual = api.getApplicationEnvironments(JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID);
    var actualData =
        requireNonNull(actual.getData()).stream()
            .map(
                d ->
                    d.activeConfId(null)
                        .currentConfId(null)
                        .appliedConfId(null)
                        .activeDeploymentUri(null))
            .toList();

    assertTrue(actualData.contains(pojaAppProdEnvironment()));
  }

  @Test
  void crupdate_environments_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    EnvironmentApi api = new EnvironmentApi(joeDoeClient);
    Environment toCreate = toCreateEnv();

    var createApplicationResponse =
        api.crupdateApplicationEnvironments(
            JOE_DOE_MAIN_ORG_ID,
            POJA_APPLICATION_ID,
            new CrupdateEnvironmentsRequestBody().data(List.of(toCrupdateEnvironment(toCreate))));
    var updatedPayload =
        requireNonNull(createApplicationResponse.getData()).getFirst().archived(true);
    api.crupdateApplicationEnvironments(
        JOE_DOE_MAIN_ORG_ID,
        POJA_APPLICATION_ID,
        new CrupdateEnvironmentsRequestBody().data(List.of(toCrupdateEnvironment(updatedPayload))));
    var envById =
        api.getApplicationEnvironmentById(
            JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, toCreate.getId());

    assertEquals(toCreate, envById);
  }

  @Test
  void crupdate_environment_ko() {
    ApiClient joeDoeClient = anApiClient();
    EnvironmentApi api = new EnvironmentApi(joeDoeClient);
    Environment toCreate =
        new Environment().id(randomUUID().toString()).environmentType(PROD).state(UNKNOWN);

    assertThrowsBadRequestException(
        () ->
            api.crupdateApplicationEnvironments(
                JOE_DOE_MAIN_ORG_ID,
                POJA_APPLICATION_ID,
                new CrupdateEnvironmentsRequestBody()
                    .data(List.of(toCrupdateEnvironment(toCreate)))),
        "Only one PROD environment can be created.");
  }

  @Test
  void get_environment_by_id_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    EnvironmentApi api = new EnvironmentApi(joeDoeClient);

    Environment actual =
        api.getApplicationEnvironmentById(
            JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, POJA_APPLICATION_ENVIRONMENT_ID);

    assertEquals(
        pojaAppProdEnvironment(),
        actual
            .activeConfId(null)
            .currentConfId(null)
            .appliedConfId(null)
            .activeDeploymentUri(null));
  }

  @Test
  void get_environment_by_id_ko() {
    ApiClient joeDoeClient = anApiClient();
    EnvironmentApi api = new EnvironmentApi(joeDoeClient);

    assertThrowsNotFoundException(
        () -> api.getApplicationEnvironmentById(JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, "dummy"),
        "Environment identified by id dummy for application "
            + POJA_APPLICATION_ID
            + " of org "
            + JOE_DOE_MAIN_ORG_ID
            + " not found");
  }

  private static Environment toCreateEnv() {
    return new Environment()
        .id("poja_preprod_application_environment_id")
        .environmentType(PREPROD)
        .status(ACTIVE)
        .state(UNKNOWN);
  }

  private static CrupdateEnvironment toCrupdateEnvironment(Environment environment) {
    return new CrupdateEnvironment()
        .id(environment.getId())
        .environmentType(environment.getEnvironmentType())
        .archived(environment.getArchived());
  }
}
