package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum.COMPLETED;
import static api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum.FAILED;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_2_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_2_NAME;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_5_ID;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import api.poja.io.aws.iam.IamComponent;
import api.poja.io.aws.iam.model.ConsoleUserCredentials;
import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.OrganizationUpserted;
import api.poja.io.endpoint.rest.api.OrgApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.organization.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.iam.model.IamException;

class OrganizationUpsertedServiceTest extends MockedThirdParties {
  @Autowired private OrganizationUpsertedService subject;
  @MockBean private IamComponent iamComponent;
  @Autowired private OrganizationService organizationService;
  private static final String CONSOLE_USERNAME = "dummyUsername";
  private static final String CONSOLE_PASSWORD = "dummyPassword";
  private static final String ACCOUNT_ID = "1234";

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  void setupSuccessfulIamCreation() {
    when(iamComponent.createIam(any()))
        .thenReturn(new ConsoleUserCredentials(CONSOLE_USERNAME, CONSOLE_PASSWORD, ACCOUNT_ID));
  }

  void setupFailedIamCreation() {
    when(iamComponent.createIam(any()))
        .thenThrow(IamException.builder().message("An error occurred during IAM creation").build());
  }

  @BeforeEach
  void setup() {
    setUpGithub(githubComponentMock);
  }

  @Test
  void user_main_org_setup_ok() throws ApiException {
    setupSuccessfulIamCreation();
    var janeDoeId = anApiClient(JANE_DOE_TOKEN);
    var api = new OrgApi(janeDoeId);
    var organizationToUpsert =
        Organization.builder().id(ORG_2_ID).name(ORG_2_NAME).ownerId(JANE_DOE_ID).build();
    var event = new OrganizationUpserted(organizationToUpsert);

    subject.accept(event);
    var upsertedOrg = organizationService.getById(organizationToUpsert.getId());
    var latestState =
        requireNonNull(api.getOrganizationSetupStates(upsertedOrg.getId()).getData()).getFirst();

    assertEquals(CONSOLE_USERNAME, upsertedOrg.getConsoleUsername());
    assertEquals(CONSOLE_PASSWORD, upsertedOrg.getConsolePassword());
    assertEquals(ACCOUNT_ID, upsertedOrg.getConsoleAccountId());
    assertEquals(COMPLETED, latestState.getProgressionStatus());
  }

  @Test
  void user_main_org_setup_ko() throws ApiException {
    setupFailedIamCreation();
    var janeDoeId = anApiClient(JANE_DOE_TOKEN);
    var api = new OrgApi(janeDoeId);
    var organizationToUpsert =
        Organization.builder().id(ORG_5_ID).name("org_5_name").ownerId(JANE_DOE_ID).build();
    var event = new OrganizationUpserted(organizationToUpsert);

    subject.accept(event);
    var latestState = requireNonNull(api.getOrganizationSetupStates(ORG_5_ID).getData()).getFirst();

    assertEquals(FAILED, latestState.getProgressionStatus());
  }
}
