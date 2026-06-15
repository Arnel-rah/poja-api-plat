package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.DENIS_RITCHIE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.HealthApi;
import api.poja.io.endpoint.rest.api.SecurityApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.integration.conf.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BetaTestEndpointIT extends MockedThirdParties {

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
  }

  @Test
  void non_beta_tester_whoami_ko() {
    ApiClient denisClient = anApiClient(DENIS_RITCHIE_TOKEN);
    SecurityApi api = new SecurityApi(denisClient);
    assertThrowsForbiddenException(api::whoami, "Access Denied");
  }

  @Test
  void non_beta_tester_beta_ping_ko() {
    ApiClient denisClient = anApiClient(DENIS_RITCHIE_TOKEN);
    HealthApi api = new HealthApi(denisClient);

    assertThrowsForbiddenException(api::betaPing, "Access Denied");
  }

  @Test
  void beta_tester_beta_ping_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    HealthApi api = new HealthApi(joeDoeClient);

    String actual = api.betaPing();

    assertEquals("beta-pong", actual);
  }
}
