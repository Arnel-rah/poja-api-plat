package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.file.ExtendedBucketComponent.TEMP_FILES_BUCKET_PREFIX;
import static api.poja.io.file.hash.FileHashAlgorithm.SHA256;
import static api.poja.io.integration.conf.utils.TestMocks.A_GITHUB_APP_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_REPO_ID;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.EnvDeployApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.hash.FileHash;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.repository.jpa.EnvBuildRequestRepository;
import api.poja.io.service.github.model.GhGetCommitResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class EnvironmentBuildIT extends MockedThirdParties {
  public static final String MOCK_BUILT_ZIP_PATH = "mock_built_zip.zip";
  public static final String MOCK_BUILT_ZIP_TEST_RESOURCE_PATH = "files/" + MOCK_BUILT_ZIP_PATH;

  private ApiClient anApiClient() {
    return TestUtils.aGithubAppApiClient(A_GITHUB_APP_TOKEN, port);
  }

  @MockBean ExtendedBucketComponent extendedBucketComponentMock;
  @Autowired EnvBuildRequestRepository envBuildRequestRepository;

  @BeforeEach
  void setup() {
    when(githubComponentMock.getRepositoryIdByAppToken(A_GITHUB_APP_TOKEN))
        .thenReturn(Optional.of(POJA_APPLICATION_REPO_ID));
    when(extendedBucketComponentMock.getPresignedPutObjectUri(any(), any()))
        .thenReturn(URI.create("https://localhost:8080"));
    when(githubComponentMock.getCommitInfo(any(), anyLong(), any(), any()))
        .thenReturn(
            new GhGetCommitResponse(
                "sha",
                new GhGetCommitResponse.GhCommit(
                    "message",
                    new GhGetCommitResponse.GhCommit.GhCommitter("name", "email"),
                    URI.create("http://localhost")),
                new GhGetCommitResponse.GhUser(
                    "login", "id", URI.create("http://localhost"), "USER")));
  }

  @Test
  void create_upload_uri_ok() throws ApiException {
    var apiClient = anApiClient();
    var api = new EnvDeployApi(apiClient);

    var createdUri = api.createFileUploadUri("mock", "mock", PROD);

    log.info("created uri {}", createdUri);
    assertNotNull(createdUri);
    assertNotNull(createdUri.getUri());
  }

  @Test
  void deploy_env_ko() {
    var apiClient = anApiClient();
    var api = new EnvDeployApi(apiClient);

    assertThrowsBadRequestException(
        () ->
            api.deployEnv(
                "mock",
                "mock",
                PROD,
                new BuiltEnvInfo()
                    .environmentType(PROD)
                    .formattedBucketKey("mock/mock.zip")
                    .id("build_1_id")),
        "EnvBuildRequest has already been sent");
  }

  @Test
  void deploy_env_ok() throws ApiException, IOException {
    when(extendedBucketComponentMock.getFileHash(any()))
        .thenReturn(
            new FileHash(
                SHA256, "cb7a62143f21e9b08cc378371fa34d994943f7f39f6134c03089c0eec50fff16"));
    String bucketKey = TEMP_FILES_BUCKET_PREFIX + MOCK_BUILT_ZIP_PATH;
    when(extendedBucketComponentMock.doesExist(bucketKey)).thenReturn(true);
    when(extendedBucketComponentMock.download(bucketKey))
        .thenReturn(new ClassPathResource(MOCK_BUILT_ZIP_TEST_RESOURCE_PATH).getFile());
    var apiClient = anApiClient();
    var api = new EnvDeployApi(apiClient);

    String id = randomUUID().toString();
    BuiltEnvInfo payload =
        new BuiltEnvInfo()
            .appEnvDeploymentId("deployment_14_id")
            .environmentType(PROD)
            .formattedBucketKey(bucketKey)
            .id(id);
    var actual = api.deployEnv("mock", "mock", PROD, payload);
    verify(eventProducerMock, times(1)).accept(anyList());

    assertTrue(envBuildRequestRepository.existsById(requireNonNull(payload.getId())));
    assertEquals(payload, actual);
  }
}
