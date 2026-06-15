package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.GithubAppInstallationRepositorySelection.ALL;
import static api.poja.io.endpoint.rest.model.GithubAppInstallationRepositorySelection.SELECTED;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestUtils.APP_INSTALLATION_1_ID;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.GithubAppInstallationApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.CreateGithubAppInstallation;
import api.poja.io.endpoint.rest.model.CrupdateGithubAppInstallationsRequestBody;
import api.poja.io.endpoint.rest.model.GithubAppInstallation;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class ApplicationInstallationIT extends MockedThirdParties {
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
  void crupdate_applications_installations_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    GithubAppInstallationApi api = new GithubAppInstallationApi(joeDoeClient);
    CreateGithubAppInstallation data =
        new CreateGithubAppInstallation()
            .id(randomUUID().toString())
            .ghInstallationId(APP_INSTALLATION_1_ID);
    CreateGithubAppInstallation copy =
        new CreateGithubAppInstallation()
            .id(randomUUID().toString())
            .ghInstallationId(APP_INSTALLATION_1_ID);

    var dataResponse =
        api.crupdateGithubAppInstallations(
            JOE_DOE_MAIN_ORG_ID,
            new CrupdateGithubAppInstallationsRequestBody().data(List.of(data)));
    var copyResponse =
        api.crupdateGithubAppInstallations(
            JOE_DOE_MAIN_ORG_ID,
            new CrupdateGithubAppInstallationsRequestBody().data(List.of(copy)));

    List<GithubAppInstallation> dataResponseData = requireNonNull(dataResponse.getData());
    GithubAppInstallation dataResponseDataFirst = dataResponseData.getFirst();
    List<GithubAppInstallation> copyResponseData = requireNonNull(copyResponse.getData());
    GithubAppInstallation copyResponseDataFirst = copyResponseData.getFirst();

    assertEquals(data.getId(), dataResponseDataFirst.getId());
    assertEquals(data.getGhInstallationId(), dataResponseDataFirst.getGhInstallationId());
    assertEquals(dataResponseDataFirst, copyResponseDataFirst);
  }

  @Test
  void get_all_application_installations_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    GithubAppInstallationApi api = new GithubAppInstallationApi(joeDoeClient);

    var getUserInstallationsResponse = requireNonNull(api.getUserInstallations(JOE_DOE_ID));
    var actual = requireNonNull(getUserInstallationsResponse.getData());

    assertTrue(actual.contains(appInstallation1()));
    assertFalse(actual.contains(appInstallation2()));
  }

  private static GithubAppInstallation appInstallation1() {
    return new GithubAppInstallation()
        .id(GH_APP_INSTALL_1_ID)
        .type("User")
        .ghAvatarUrl("http://testimage.com")
        .owner("joedoelogin1")
        .repositorySelection(ALL)
        .ghInstallationId(12344L);
  }

  private static GithubAppInstallation appInstallation2() {
    return new GithubAppInstallation()
        .id("gh_app_install_2_id")
        .type("Organization")
        .ghAvatarUrl("http://testimage.com")
        .owner("janedoelogin")
        .repositorySelection(SELECTED)
        .ghInstallationId(12346L);
  }
}
