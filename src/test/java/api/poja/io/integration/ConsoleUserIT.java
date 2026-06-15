package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.OrgApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.ConsoleUser;
import api.poja.io.integration.conf.utils.TestUtils;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class ConsoleUserIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  @BeforeEach
  void setUp() {
    setUpGithub(githubComponentMock);
  }

  @Test
  void read_console_user_ok() throws ApiException {
    var client = anApiClient();
    var api = new OrgApi(client);

    var read = api.getConsoleUser(JOE_DOE_MAIN_ORG_ID);

    assertEquals(expectedJoeDoeMainOrgConsoleUser(), read);
  }

  ConsoleUser expectedJoeDoeMainOrgConsoleUser() {
    return new ConsoleUser()
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .username("org-JoeDoe")
        .password("org-JoeDoe-password")
        .consoleLoginUrl(URI.create("https://101.signin.aws.amazon.com/console"))
        .accountId("101");
  }
}
