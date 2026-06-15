package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.HTTP_API;
import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf3;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf6;
import static api.poja.io.integration.conf.utils.TestMocks.joePojaApplication1;
import static api.poja.io.integration.conf.utils.TestMocks.pojaAppEnvironments;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsApiException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.EnvironmentApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.Application;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.repository.jpa.EnvironmentRepository;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class ApplicationEnvironmentConfigIT extends MockedThirdParties {
  @MockBean ExtendedBucketComponent extendedBucketComponentMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  @Autowired EnvironmentRepository environmentRepository;

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
    setUpExtendedBucketComponentMock(extendedBucketComponentMock);
  }

  void setUpExtendedBucketComponentMock(ExtendedBucketComponent extendedBucketComponent)
      throws IOException {
    when(extendedBucketComponent.doesExist(any())).thenReturn(true);
    when(extendedBucketComponent.download(any()))
        .thenReturn(new ClassPathResource("files/poja_3.yml").getFile());
  }

  @Test
  void configure_environment_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new EnvironmentApi(apiClient);
    var currentApplication = joePojaApplication1();
    var currentEnv = pojaAppEnvironments().getFirst();
    String id = randomUUID().toString();
    assertThrowsBadRequestException(
        () ->
            applyConf(
                api,
                currentApplication,
                currentEnv,
                id,
                new EnvConf().conf(new OneOfPojaConf(getValidPojaConf6(HTTP_API))).id(id)),
        "compute.frontal_function_invocation_method = HTTP_API is a premium feature.");

    EnvConf newPayload =
        new EnvConf().conf(new OneOfPojaConf(getValidPojaConf6(LAMBDA_URL))).id(id);
    var actual = applyConf(api, currentApplication, currentEnv, id, newPayload);

    var newId = randomUUID().toString();
    assertThrowsBadRequestException(
        () ->
            applyConf(
                api,
                currentApplication,
                currentEnv,
                newId,
                new EnvConf().conf(new OneOfPojaConf(getValidPojaConf6(HTTP_API))).id(newId)),
        "compute.frontal_function_invocation_method = HTTP_API is a premium feature.");
    // TODO: test premium
    assertEquals(newPayload, actual);
  }

  private EnvConf applyConf(
      EnvironmentApi api,
      Application currentApplication,
      Environment currentEnv,
      String id,
      EnvConf newPayload)
      throws ApiException {
    var res =
        api.saveApplicationEnvConfig(
            JOE_DOE_MAIN_ORG_ID, currentApplication.getId(), currentEnv.getId(), id, newPayload);
    environmentRepository.save(
        environmentRepository.findById(currentEnv.getId()).get().toBuilder()
            .appliedConfId(newPayload.getId())
            .build());
    return res;
  }

  @Test
  void read_env_config_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new EnvironmentApi(apiClient);
    var currentApplication = joePojaApplication1();
    var currentEnv = pojaAppEnvironments().getFirst();
    var expected =
        new EnvConf()
            .conf(new OneOfPojaConf(getValidPojaConf3(HTTP_API)))
            .id("env_1_depl_files_1_id");

    var actual =
        api.getApplicationEnvironmentConfigById(
            JOE_DOE_MAIN_ORG_ID,
            currentApplication.getId(),
            currentEnv.getId(),
            "env_1_depl_files_1_id");

    assertEquals(expected, actual);
  }

  @Test
  void read_empty_from_db_env_config_ko() {
    var apiClient = anApiClient();
    var api = new EnvironmentApi(apiClient);

    assertThrowsApiException(
        () ->
            api.getApplicationEnvironmentConfigById(
                JOE_DOE_MAIN_ORG_ID,
                "other_poja_application_id",
                "other_poja_application_environment_2_id",
                "env_1_depl_files_3_id"),
        "{\"type\":\"500 INTERNAL_SERVER_ERROR\",\"message\":\"config not found in DB for org.Id ="
            + " org-JoeDoe-id app.Id = other_poja_application_id environment.Id ="
            + " other_poja_application_environment_2_id\"}");
  }

  @Test
  void read_empty_from_s3_env_config_ko() {
    reset(extendedBucketComponentMock);
    when(extendedBucketComponentMock.doesExist(any())).thenReturn(false);
    var apiClient = anApiClient();
    var api = new EnvironmentApi(apiClient);
    var currentApplication = joePojaApplication1();
    var currentEnv = pojaAppEnvironments().getFirst();

    assertThrowsApiException(
        () ->
            api.getApplicationEnvironmentConfigById(
                JOE_DOE_MAIN_ORG_ID,
                currentApplication.getId(),
                currentEnv.getId(),
                "env_1_depl_files_2_id"),
        "{\"type\":\"500 INTERNAL_SERVER_ERROR\",\"message\":\"config not found in S3 for org.Id ="
            + " org-JoeDoe-id app.Id = poja_application_id environment.Id ="
            + " poja_application_environment_id\"}");
  }
}
