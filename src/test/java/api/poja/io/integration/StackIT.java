package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.Environment.StateEnum.HEALTHY;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.endpoint.rest.model.StackType.COMPUTE_PERMISSION;
import static api.poja.io.endpoint.rest.model.StackType.EVENT;
import static api.poja.io.endpoint.rest.model.StackType.STORAGE_BUCKET;
import static api.poja.io.integration.conf.utils.TestMocks.BUCKET_STACK_ID;
import static api.poja.io.integration.conf.utils.TestMocks.BUCKET_STACK_NAME;
import static api.poja.io.integration.conf.utils.TestMocks.COMPUTE_PERM_STACK_ID;
import static api.poja.io.integration.conf.utils.TestMocks.COMPUTE_PERM_STACK_NAME;
import static api.poja.io.integration.conf.utils.TestMocks.EVENT_STACK_ID;
import static api.poja.io.integration.conf.utils.TestMocks.EVENT_STACK_NAME;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.permStackEvents;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpExtendedBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.time.Instant.MAX;
import static java.time.Instant.MIN;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.StackApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.ApplicationBase;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.endpoint.rest.model.GithubRepository;
import api.poja.io.endpoint.rest.model.Stack;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
public class StackIT extends MockedThirdParties {
  @MockBean ExtendedBucketComponent extendedBucketComponent;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  private static ApplicationBase otherApplication() {
    return new ApplicationBase()
        .id(OTHER_POJA_APPLICATION_ID)
        .name("other-poja-app")
        .archived(false)
        .githubRepository(
            new GithubRepository()
                .name("other_poja_application")
                .isPrivate(false)
                .description("a regular poja app")
                .id("gh_repository_4_id")
                .htmlUrl(URI.create("http://github.com/user/repo"))
                .installationId(GH_APP_INSTALL_1_ID))
        .userId(JOE_DOE_ID);
  }

  private static Environment otherEnvironment() {
    return new Environment()
        .id(OTHER_POJA_APPLICATION_ENVIRONMENT_ID)
        .environmentType(PROD)
        .activeDeploymentUri(URI.create("https://example.com/deploy1"))
        .archived(false)
        .state(HEALTHY);
  }

  private Stack bucketStack() {
    return new Stack()
        .id(BUCKET_STACK_ID)
        .name(BUCKET_STACK_NAME)
        .stackType(STORAGE_BUCKET)
        .cfStackId("bucket_stack_aws_id")
        .applicationId(otherApplication().getId())
        .environmentId(otherEnvironment().getId())
        .creationDatetime(Instant.parse("2023-06-18T10:15:30.00Z"))
        .updateDatetime(Instant.parse("2023-07-18T10:15:30.00Z"));
  }

  private Stack computePermStack() {
    return new Stack()
        .id(COMPUTE_PERM_STACK_ID)
        .name(COMPUTE_PERM_STACK_NAME)
        .stackType(COMPUTE_PERMISSION)
        .cfStackId("compute_perm_stack_aws_id")
        .applicationId(otherApplication().getId())
        .environmentId(otherEnvironment().getId())
        .creationDatetime(Instant.parse("2023-06-18T10:15:30.00Z"))
        .updateDatetime(Instant.parse("2023-07-18T10:15:30.00Z"));
  }

  private Stack eventStack() {
    return new Stack()
        .id(EVENT_STACK_ID)
        .name(EVENT_STACK_NAME)
        .stackType(EVENT)
        .cfStackId("event_stack_aws_id")
        .applicationId(otherApplication().getId())
        .environmentId(otherEnvironment().getId())
        .creationDatetime(Instant.parse("2023-06-18T10:15:30.00Z"))
        .updateDatetime(Instant.parse("2023-07-18T10:15:30.00Z"));
  }

  @BeforeEach
  void setup() throws IOException, URISyntaxException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
    setUpExtendedBucketComponent(extendedBucketComponent);
  }

  @Test
  void get_stack_list_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    StackApi api = new StackApi(joeDoeClient);

    var actual =
        api.getEnvironmentStacks(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            null,
            null,
            null);
    var actualData = Objects.requireNonNull(actual.getData());

    assertTrue(actualData.contains(eventStack()));
    assertTrue(actualData.contains(computePermStack()));
    assertTrue(actualData.contains(bucketStack()));
  }

  @Test
  void get_other_user_stack_list_ko() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    StackApi api = new StackApi(janeDoeClient);

    assertThrowsForbiddenException(
        () ->
            api.getEnvironmentStacks(
                JANE_DOE_MAIN_ORG_ID,
                POJA_APPLICATION_ID,
                POJA_APPLICATION_ENVIRONMENT_ID,
                null,
                null,
                null),
        "Access Denied");
  }

  @Test
  void get_stack_by_id_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    StackApi api = new StackApi(joeDoeClient);

    Stack actualEventStack =
        api.getStackById(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            EVENT_STACK_ID);
    Stack actualBucketStack =
        api.getStackById(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            "bucket_stack_1_id");

    assertEquals(eventStack(), actualEventStack);
    assertEquals(bucketStack(), actualBucketStack);
  }

  @Test
  void get_other_user_stack_by_id_ko() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    StackApi api = new StackApi(janeDoeClient);

    assertThrowsForbiddenException(
        () ->
            api.getStackById(
                JANE_DOE_MAIN_ORG_ID,
                POJA_APPLICATION_ID,
                POJA_APPLICATION_ENVIRONMENT_ID,
                COMPUTE_PERM_STACK_ID),
        "Access Denied");
  }

  @Test
  void get_stack_events_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    StackApi api = new StackApi(joeDoeClient);

    var actual =
        api.getStackEvents(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            COMPUTE_PERMISSION,
            null,
            null,
            Instant.parse("2024-07-26T05:08:30.029Z"),
            Instant.parse("2024-07-26T05:47:37.873Z"));
    var actualWithoutFrom =
        api.getStackEvents(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            COMPUTE_PERMISSION,
            null,
            null,
            null,
            Instant.parse("2024-07-26T05:47:37.873Z"));
    var actualWithMinTo =
        api.getStackEvents(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            COMPUTE_PERMISSION,
            null,
            null,
            null,
            MIN);
    var actualWithoutTo =
        api.getStackEvents(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            COMPUTE_PERMISSION,
            null,
            null,
            Instant.parse("2024-07-26T05:08:30.029Z"),
            null);
    var actualWithMaxFrom =
        api.getStackEvents(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            COMPUTE_PERMISSION,
            null,
            null,
            MAX,
            null);
    var actualData = requireNonNull(actual.getData());
    var actualWithoutFromData = requireNonNull(actualWithoutFrom.getData());
    var actualWithoutToData = requireNonNull(actualWithoutTo.getData());
    var actualWithMinToData = requireNonNull(actualWithMinTo.getData());
    var actualWithMaxFromData = requireNonNull(actualWithMaxFrom.getData());

    assertTrue(actualData.containsAll(permStackEvents()));
    assertEquals(Instant.parse("2024-07-26T05:08:30.029Z"), actual.getLatestStackEventTimestamp());
    assertTrue(actualWithoutFromData.containsAll(permStackEvents()));
    assertEquals(
        Instant.parse("2024-07-26T05:08:30.029Z"),
        actualWithoutFrom.getLatestStackEventTimestamp());
    assertTrue(actualWithoutToData.containsAll(permStackEvents()));
    assertNull(actualWithMinTo.getLatestStackEventTimestamp());
    assertTrue(actualWithMinToData.isEmpty());
    assertEquals(
        Instant.parse("2024-07-26T05:08:30.029Z"), actualWithoutTo.getLatestStackEventTimestamp());
    assertTrue(actualWithMaxFromData.isEmpty());
    assertNull(actualWithMaxFrom.getLatestStackEventTimestamp());
  }

  @Test
  void get_other_stack_event_ko() {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    StackApi api = new StackApi(janeDoeClient);

    assertThrowsForbiddenException(
        () ->
            api.getStackEvents(
                JOE_DOE_MAIN_ORG_ID,
                OTHER_POJA_APPLICATION_ID,
                OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
                COMPUTE_PERMISSION,
                null,
                null,
                null,
                null),
        "Access Denied");
  }
}
