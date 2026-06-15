package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.StackApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.ComputeStackResource;
import api.poja.io.endpoint.rest.model.EnvironmentResourceByDeployment;
import api.poja.io.endpoint.rest.model.EventQueue;
import api.poja.io.endpoint.rest.model.EventQueueResourceGroup;
import api.poja.io.endpoint.rest.model.EventStackResource;
import api.poja.io.endpoint.rest.model.StorageBucketStackResource;
import api.poja.io.endpoint.rest.model.WorkerFunction;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class EnvironmentResourcesGroupedByDeploymentIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
  }

  @Test
  void read_environments_resources_by_env_ok() throws ApiException {
    var joeDoeClient = anApiClient();
    var api = new StackApi(joeDoeClient);
    var data =
        api.getEnvResourcesGroupedByDeployment(
                JOE_DOE_MAIN_ORG_ID,
                OTHER_POJA_APPLICATION_ID,
                OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
                new PageFromOne(1).getValue(),
                new BoundedPageSize(10).getValue())
            .getData();
    assert data != null;
    var sorted =
        data.stream()
            .sorted(
                Comparator.comparing(EnvironmentResourceByDeployment::getDeploymentDatetime)
                    .reversed())
            .toList();

    assertEquals(2, data.size());
    assertEquals(sorted, data);
    log.info("data {}", data);
    assertFalse(data.contains(otherPojaApplicationEnvResource1()));
    assertTrue(data.contains(otherPojaApplicationEnvResource2()));
    assertTrue(data.contains(otherPojaApplicationEnvResource3()));
  }

  EnvironmentResourceByDeployment otherPojaApplicationEnvResource1() {
    return new EnvironmentResourceByDeployment()
        .applicationEnvironmentDeploymentId("deployment_1_id")
        .commitMessage("Initial deployment")
        .commitSha("abc123def456")
        .deploymentDatetime(Instant.parse("2024-08-01T10:15:00Z"))
        .environmentId(OTHER_POJA_APPLICATION_ENVIRONMENT_ID)
        .environmentType(PROD)
        .computeStackResource(otherPojaApplicationComputeStack1())
        .eventStackResource(otherPojaApplicationEventStack1())
        .storageBucketStackResource(otherPojaApplicationStorageBucketStack1());
  }

  EnvironmentResourceByDeployment otherPojaApplicationEnvResource2() {
    return new EnvironmentResourceByDeployment()
        .applicationEnvironmentDeploymentId("deployment_12_id")
        .commitMessage("Seventh deployment")
        .commitSha("stu901vwx234")
        .deploymentDatetime(Instant.parse("2024-08-07T12:15:00Z"))
        .environmentId(OTHER_POJA_APPLICATION_ENVIRONMENT_ID)
        .environmentType(PROD)
        .computeStackResource(otherPojaApplicationComputeStack2())
        .eventStackResource(otherPojaApplicationEventStack2())
        .storageBucketStackResource(otherPojaApplicationStorageBucketStack2());
  }

  EnvironmentResourceByDeployment otherPojaApplicationEnvResource3() {
    return new EnvironmentResourceByDeployment()
        .applicationEnvironmentDeploymentId("deployment_7_id")
        .commitMessage("Eighth deployment")
        .commitSha("vwx234yzb567")
        .deploymentDatetime(Instant.parse("2024-08-08T12:30:00Z"))
        .environmentId(OTHER_POJA_APPLICATION_ENVIRONMENT_ID)
        .environmentType(PROD)
        .computeStackResource(otherPojaApplicationComputeStack3())
        .eventStackResource(otherPojaApplicationEventStack3())
        .storageBucketStackResource(otherPojaApplicationStorageBucketStack3());
  }

  ComputeStackResource otherPojaApplicationComputeStack1() {
    return new ComputeStackResource()
        .id("poja_application_compute_2_resources_id")
        .frontalFunctionName("preprod-compute-frontal-function")
        .workerFunctions(
            List.of(
                new WorkerFunction().name("preprod-compute-worker-1-function"),
                new WorkerFunction().name("preprod-compute-worker-2-function")))
        .functionDashboardUrlPrefix(
            URI.create(
                "https://dummy-region.console.aws.amazon.com/lambda/home?region=dummy-region#/functions/"))
        .creationDatetime(Instant.parse("2023-07-18T10:15:30.00Z"));
  }

  ComputeStackResource otherPojaApplicationComputeStack2() {
    return new ComputeStackResource()
        .id("poja_application_compute_3_resources_id")
        .frontalFunctionName("preprod-compute-frontal-function")
        .workerFunctions(
            List.of(
                new WorkerFunction().name("preprod-compute-worker-1-function"),
                new WorkerFunction().name("preprod-compute-worker-2-function-2")))
        .functionDashboardUrlPrefix(
            URI.create(
                "https://dummy-region.console.aws.amazon.com/lambda/home?region=dummy-region#/functions/"))
        .creationDatetime(Instant.parse("2024-08-07T12:15:00Z"));
  }

  ComputeStackResource otherPojaApplicationComputeStack3() {
    return new ComputeStackResource()
        .id("poja_application_compute_4_resources_id")
        .frontalFunctionName("preprod-compute-frontal-function")
        .workerFunctions(
            List.of(
                new WorkerFunction().name("preprod-compute-worker-1-function-2"),
                new WorkerFunction().name("preprod-compute-worker-2-function")))
        .functionDashboardUrlPrefix(
            URI.create(
                "https://dummy-region.console.aws.amazon.com/lambda/home?region=dummy-region#/functions/"))
        .creationDatetime(Instant.parse("2024-08-08T12:30:00Z"));
  }

  EventStackResource otherPojaApplicationEventStack1() {
    return new EventStackResource()
        .id("other_poja_application_event_1_resource_id")
        .queues(
            List.of(
                new EventQueueResourceGroup()
                    .queue(
                        new EventQueue()
                            .name("mailboxQueue1")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FmailboxQueue1")))
                    .dlQueue(
                        new EventQueue()
                            .name("deadQueue1")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FdeadQueue1")))));
  }

  EventStackResource otherPojaApplicationEventStack2() {
    return new EventStackResource()
        .id("other_poja_application_event_2_resource_id")
        .queues(
            List.of(
                new EventQueueResourceGroup()
                    .queue(
                        new EventQueue()
                            .name("mailboxQueue1")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FmailboxQueue1")))
                    .dlQueue(
                        new EventQueue()
                            .name("deadQueue1")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FdeadQueue1"))),
                new EventQueueResourceGroup()
                    .queue(
                        new EventQueue()
                            .name("mailboxQueue2")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FmailboxQueue2")))
                    .dlQueue(
                        new EventQueue()
                            .name("deadQueue2")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FdeadQueue2")))));
  }

  EventStackResource otherPojaApplicationEventStack3() {
    return new EventStackResource()
        .id("other_poja_application_event_3_resource_id")
        .queues(
            List.of(
                new EventQueueResourceGroup()
                    .queue(
                        new EventQueue()
                            .name("mailboxQueue1")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FmailboxQueue1")))
                    .dlQueue(
                        new EventQueue()
                            .name("deadQueue1")
                            .resourceUri(
                                URI.create(
                                    "https://dummy-region.console.aws.amazon.com/sqs/v3/home#/queues/https%3A%2F%2Fsqs.dummy-region.amazonaws.com%2F01%2FdeadQueue1")))));
  }

  StorageBucketStackResource otherPojaApplicationStorageBucketStack1() {
    return new StorageBucketStackResource()
        .id("other_poja_application_storage_bucket_1_resource_id")
        .bucketName("other_poja_application_bucket")
        .bucketUri(
            URI.create(
                "https://dummy-region.console.aws.amazon.com/s3/buckets/other_poja_application_bucket"));
  }

  StorageBucketStackResource otherPojaApplicationStorageBucketStack2() {
    return new StorageBucketStackResource()
        .id("other_poja_application_storage_bucket_2_resource_id")
        .bucketName("other_poja_application_bucket")
        .bucketUri(
            URI.create(
                "https://dummy-region.console.aws.amazon.com/s3/buckets/other_poja_application_bucket"));
  }

  StorageBucketStackResource otherPojaApplicationStorageBucketStack3() {
    return new StorageBucketStackResource()
        .id("other_poja_application_storage_bucket_3_resource_id")
        .bucketName("other_poja_application_bucket")
        .bucketUri(
            URI.create(
                "https://dummy-region.console.aws.amazon.com/s3/buckets/other_poja_application_bucket"));
  }
}
