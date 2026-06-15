package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.PROD_COMPUTE_FRONTAL_FUNCTION;
import static api.poja.io.integration.conf.utils.TestMocks.PROD_COMPUTE_WORKER_1_FUNCTION;
import static api.poja.io.integration.conf.utils.TestMocks.PROD_COMPUTE_WORKER_2_FUNCTION;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.StackApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.FunctionMonitoringResource;
import api.poja.io.endpoint.rest.model.GroupedMonitoringResources;
import api.poja.io.endpoint.rest.model.WorkerMonitoringResources;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
public class MonitoringResourceIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  private static GroupedMonitoringResources groupedFunctionMonitoringResource() {
    Instant creationDatetime = Instant.parse("2024-07-18T10:15:29.00Z");
    Instant creationDatetime1 = Instant.parse("2024-07-18T10:15:30.00Z");
    Instant creationDatetime2 = Instant.parse("2024-07-18T10:15:28.00Z");
    return new GroupedMonitoringResources()
        .frontalFunctionMonitoringResources(
            List.of(
                new FunctionMonitoringResource()
                    .name(PROD_COMPUTE_FRONTAL_FUNCTION)
                    .monitoringUri(
                        URI.create(
                            "https://dummy-region.console.aws.amazon.com/lambda/home?region=dummy-region#/functions/prod-compute-frontal-function"))
                    .creationDatetime(creationDatetime1),
                new FunctionMonitoringResource()
                    .name("prod-compute-frontal-function-3")
                    .monitoringUri(null)
                    .creationDatetime(creationDatetime2)))
        .workerFunctionMonitoringResources(
            List.of(
                new WorkerMonitoringResources()
                    .workerIndex(0)
                    .monitoringResources(
                        List.of(
                            new FunctionMonitoringResource()
                                .name(PROD_COMPUTE_WORKER_1_FUNCTION)
                                .monitoringUri(
                                    URI.create(
                                        "https://dummy-region.console.aws.amazon.com/lambda/home?region=dummy-region#/functions/prod-compute-worker-1-function"))
                                .creationDatetime(creationDatetime1))),
                new WorkerMonitoringResources()
                    .workerIndex(1)
                    .monitoringResources(
                        List.of(
                            new FunctionMonitoringResource()
                                .monitoringUri(
                                    URI.create(
                                        "https://dummy-region.console.aws.amazon.com/lambda/home?region=dummy-region#/functions/prod-compute-worker-2-function"))
                                .name(PROD_COMPUTE_WORKER_2_FUNCTION)
                                .creationDatetime(creationDatetime1),
                            new FunctionMonitoringResource()
                                .monitoringUri(null)
                                .name("prod-compute-worker-2-function-3")
                                .creationDatetime(creationDatetime),
                            new FunctionMonitoringResource()
                                .monitoringUri(null)
                                .name("prod-compute-worker-2-function-2")
                                .creationDatetime(creationDatetime2)))));
  }

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
  }

  @Test
  void get_environment_compute_stack_resources() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    StackApi api = new StackApi(joeDoeClient);

    var data =
        api.getLogResources(
            JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID, POJA_APPLICATION_ENVIRONMENT_ID);

    assertEquals(groupedFunctionMonitoringResource(), data);
  }
}
