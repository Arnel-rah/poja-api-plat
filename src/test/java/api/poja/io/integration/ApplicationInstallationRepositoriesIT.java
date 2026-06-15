package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_2_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.net.URI.create;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.GithubAppInstallationApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.GithubRepositoryListItem;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class ApplicationInstallationRepositoriesIT extends MockedThirdParties {
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
  void get_all_repos_ok() throws ApiException {
    var joeDoeClient = anApiClient();
    var api = new GithubAppInstallationApi(joeDoeClient);
    var data =
        api.getOrgInstallationRepositories(
                JOE_DOE_MAIN_ORG_ID,
                GH_APP_INSTALL_1_ID,
                new PageFromOne(1).getValue(),
                new BoundedPageSize(30).getValue())
            .getData();

    assert data != null;
    assertTrue(data.contains(joeDoeGhRepo()));
  }

  @Test
  void get_joe_doe_repos_ko() {
    var joeDoeClient = anApiClient();
    var api = new GithubAppInstallationApi(joeDoeClient);
    assertThrowsForbiddenException(
        () ->
            api.getOrgInstallationRepositories(
                JOE_DOE_MAIN_ORG_ID,
                GH_APP_INSTALL_2_ID,
                new PageFromOne(1).getValue(),
                new BoundedPageSize(30).getValue()),
        "Access Denied");
  }

  GithubRepositoryListItem joeDoeGhRepo() {
    return new GithubRepositoryListItem()
        .id("10")
        .name("joe_doe_repo_mock")
        .description("description")
        .isPrivate(true)
        .defaultBranch("default")
        .htmlUrl(create("https://repoUrl.com/repo"))
        .installationId(GH_APP_INSTALL_1_ID)
        .isEmpty(false);
  }
}
