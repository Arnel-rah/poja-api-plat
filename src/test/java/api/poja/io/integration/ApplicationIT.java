package api.poja.io.integration;

import static api.poja.io.conf.EnvConf.BETA_USER_ALLOWED_APP_NB;
import static api.poja.io.endpoint.rest.model.Application.StatusEnum.UNKNOWN;
import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.integration.conf.utils.TestMocks.CRUPDATE_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ENV_CONF_ID;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_ID;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_3_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_GITHUB_REPOSITORY_WITHOUT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_NAME;
import static api.poja.io.integration.conf.utils.TestMocks.applicationToCreate;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf3;
import static api.poja.io.integration.conf.utils.TestMocks.janePojaApplication;
import static api.poja.io.integration.conf.utils.TestMocks.joeArchivedPojaApplication1;
import static api.poja.io.integration.conf.utils.TestMocks.joePojaApplication1;
import static api.poja.io.integration.conf.utils.TestMocks.joePojaApplication2;
import static api.poja.io.integration.conf.utils.TestMocks.premiumOfferDomain;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsApiException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsNotFoundException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpBucketComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_FAILED;
import static java.lang.Boolean.TRUE;
import static java.net.URI.create;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Pageable.unpaged;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.ApplicationApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.Application;
import api.poja.io.endpoint.rest.model.ApplicationBase;
import api.poja.io.endpoint.rest.model.CreateAndDeployAppRequestBody;
import api.poja.io.endpoint.rest.model.CreateAndDeployAppResponse;
import api.poja.io.endpoint.rest.model.CrupdateApplicationsRequestBody;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.GithubRepository;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.endpoint.rest.model.PagedUserApplicationsResponse;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.repository.jpa.ApplicationImportRepository;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.model.UserSubscription;
import api.poja.io.service.UserSubscriptionService;
import api.poja.io.service.validator.UserAppThresholdValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
// TODO check application.status value
class ApplicationIT extends MockedThirdParties {
  @MockBean UserSubscriptionService userSubscriptionService;
  @SpyBean UserAppThresholdValidator appThresholdValidatorMock;

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  private ApiClient janeDoeClient() {
    return TestUtils.anApiClient(JANE_DOE_TOKEN, port);
  }

  private ApiClient loremIpsumClient() {
    return TestUtils.anApiClient(LOREM_IPSUM_TOKEN, port);
  }

  void setupAppThresholdValidator() {
    doNothing().when(appThresholdValidatorMock).accept(any(), any());
  }

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);
    setUpBucketComponent(bucketComponentMock);
  }

  @Autowired ApplicationRepository applicationRepository;
  @Autowired ApplicationImportRepository importRepository;

  @Test
  void crupdate_applications_ok() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);
    ApplicationBase toCreate = applicationToCreate();

    var createApplicationResponse =
        api.crupdateApplications(
            JOE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(List.of(toCreate)));
    List<ApplicationBase> updatedPayload =
        List.of(
            toApplicationBase(
                requireNonNull(createApplicationResponse.getData())
                    .getFirst()
                    .name(randomUUID().toString())));

    var updateApplicationResponse =
        api.crupdateApplications(
            JOE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(updatedPayload));
    var updateApplicationResponseData =
        requireNonNull(updateApplicationResponse.getData()).stream()
            .map(ApplicationIT::ignoreIds)
            .map(ApplicationIT::ignoreRepositoryUrls)
            .toList();

    List<Application> expectedResponseData =
        updatedPayload.stream()
            .map(ApplicationIT::toApplication)
            .map(a -> a.status(UNKNOWN))
            .map(ApplicationIT::ignoreIds)
            .map(ApplicationIT::ignoreRepositoryUrls)
            .toList();
    assertTrue(updateApplicationResponseData.containsAll(expectedResponseData));
  }

  @Test
  void create_applications_ko() {
    setupAppThresholdValidator();

    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);
    ApplicationBase toCreate = randomAppWithoutRepoId();
    toCreate.name(requireNonNull(toCreate.getName()).toUpperCase());

    assertThrowsBadRequestException(
        () ->
            api.crupdateApplications(
                JOE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(List.of(toCreate))),
        "app_name must not have more than 11 characters and contain only lowercase letters, numbers"
            + " and hyphen (-).");
  }

  @Test
  void create_application_ok() throws ApiException {
    var joeDoeClient = joeDoeClient();
    var api = new ApplicationApi(joeDoeClient);
    var toCreate = applicationToCreate();
    var env =
        new CrupdateEnvironment()
            .id(CRUPDATE_ENVIRONMENT_ID)
            .environmentType(PREPROD)
            .archived(false);
    var envConf =
        new EnvConf().id(ENV_CONF_ID).conf(new OneOfPojaConf(getValidPojaConf3(LAMBDA_URL)));
    CreateAndDeployAppResponse createApplicationResponse =
        api.createAndDeployApp(
            JOE_DOE_MAIN_ORG_ID,
            new CreateAndDeployAppRequestBody()
                .application(toCreate)
                .environment(env)
                .envConf(envConf));

    Application actualResponseData = requireNonNull(createApplicationResponse.getApplication());

    assertEquals(toCreate.getName(), actualResponseData.getName());
    assertEquals(PREPROD, createApplicationResponse.getEnvironment().getEnvironmentType());
    assertNotEquals(TRUE, actualResponseData.getArchived());
    assertEquals(CRUPDATE_ENVIRONMENT_ID, createApplicationResponse.getEnvironment().getId());
  }

  @Test
  void create_two_applications_without_repo_id_ok() throws ApiException {
    setupAppThresholdValidator();

    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);
    ApplicationBase toCreate = randomAppWithoutRepoId();
    ApplicationBase toCreate2 = randomAppWithoutRepoId();

    List<ApplicationBase> creationPayload = List.of(toCreate, toCreate2);
    var createApplicationResponse =
        api.crupdateApplications(
            JOE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));
    List<Application> createApplicationResponseData =
        requireNonNull(createApplicationResponse.getData()).stream()
            .map(ApplicationIT::ignoreIds)
            .toList();

    List<Application> expectedResponseData =
        creationPayload.stream()
            .map(ApplicationIT::toApplication)
            .map(a -> a.status(UNKNOWN))
            .map(ApplicationIT::ignoreIds)
            .toList();
    assertEquals(
        expectedResponseData.getFirst().getId(), createApplicationResponseData.getFirst().getId());
    assertEquals(expectedResponseData.get(1).getId(), createApplicationResponseData.get(1).getId());
  }

  @Test
  void create_app_with_existing_repo_id_ko() throws ApiException {
    setupAppThresholdValidator();

    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);
    GithubRepository randomRepo =
        new GithubRepository()
            .id(randomUUID().toString())
            .name(randomUUID().toString())
            .isPrivate(false)
            .description("random repo")
            .installationId(GH_APP_INSTALL_1_ID)
            .htmlUrl(create("http://github.com/user/reporandom"));
    ApplicationBase toCreate = randomAppWithoutRepoId().githubRepository(randomRepo);
    ApplicationBase toCreate2 = randomAppWithoutRepoId().githubRepository(randomRepo);

    List<ApplicationBase> creationPayload = List.of(toCreate);
    api.crupdateApplications(
        JOE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));

    assertThrowsApiException(
        () ->
            api.crupdateApplications(
                JOE_DOE_MAIN_ORG_ID,
                new CrupdateApplicationsRequestBody().data(List.of(toCreate2))),
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\"Multiple import on single repository has"
            + " not been implemented yet. Github Repository named repoName="
            + toCreate2.getGithubRepository().getName()
            + " has already been imported by another user.\"}");
  }

  @Test
  void get_all_applications_ok() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);

    var orgIdFilteredPagedResponse =
        api.getApplications(
            JOE_DOE_MAIN_ORG_ID,
            null,
            new PageFromOne(1).getValue(),
            new BoundedPageSize(10).getValue());
    List<Application> orgIdFilteredPagedResponseData =
        requireNonNull(orgIdFilteredPagedResponse.getData());
    var nameFilteredPagedResponse =
        api.getApplications(
            JOE_DOE_MAIN_ORG_ID,
            "2",
            new PageFromOne(1).getValue(),
            new BoundedPageSize(10).getValue());
    List<Application> nameFilteredPagedResponseData =
        requireNonNull(nameFilteredPagedResponse.getData());

    assertTrue(orgIdFilteredPagedResponseData.contains(joePojaApplication1()));
    assertFalse(orgIdFilteredPagedResponseData.contains(janePojaApplication()));
    assertTrue(nameFilteredPagedResponseData.contains(joePojaApplication2()));
    assertFalse(nameFilteredPagedResponseData.contains(joePojaApplication1()));
    assertEquals(
        orgIdFilteredPagedResponseData,
        orgIdFilteredPagedResponseData.stream()
            .sorted(comparing(Application::getCreationDatetime).reversed())
            .toList());
  }

  @Test
  void get_application_by_id_ok() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);

    Application actual = api.getApplicationById(JOE_DOE_MAIN_ORG_ID, POJA_APPLICATION_ID);

    assertEquals(joePojaApplication1(), actual);
  }

  @Test
  void get_application_by_id_ko() {
    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);

    assertThrowsNotFoundException(
        () -> api.getApplicationById(JOE_DOE_MAIN_ORG_ID, "non_existent_application_id"),
        "Application identified by id=non_existent_application_id not found");
  }

  private static ApplicationBase toApplicationBase(Application application) {
    return new ApplicationBase()
        .id(application.getId())
        .name(application.getName())
        .archived(application.getArchived())
        .githubRepository(application.getGithubRepository())
        .orgId(application.getOrgId())
        .userId(application.getUserId())
        .applicationImport(application.getApplicationImport());
  }

  private static Application toApplication(ApplicationBase base) {
    return new Application()
        .id(base.getId())
        .name(base.getName())
        .archived(base.getArchived())
        .githubRepository(base.getGithubRepository())
        .userId(base.getUserId())
        .orgId(base.getOrgId())
        .applicationImport(base.getApplicationImport());
  }

  private static Application ignoreIds(Application application) {
    return application.id(null).creationDatetime(null);
  }

  private static Application ignoreRepositoryUrls(Application application) {
    return application.githubRepository(application.getGithubRepository().htmlUrl(null));
  }

  private static ApplicationBase randomAppWithoutRepoId() {
    String id = randomUUID().toString();
    return new ApplicationBase()
        .id(id)
        .name(id.length() > 11 ? id.substring(0, 11) : id)
        .userId(JOE_DOE_ID)
        .githubRepository(
            new GithubRepository()
                .id(null)
                .name("some_random_repo" + id)
                .description("some_random_description")
                .isPrivate(true)
                .installationId(GH_APP_INSTALL_1_ID))
        .archived(false)
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .applicationImport(null);
  }

  private static ApplicationBase randomJaneDoeAppWithoutRepoId() {
    String id = randomUUID().toString();
    return new ApplicationBase()
        .id(id)
        .name(id.length() > 11 ? id.substring(0, 11) : id)
        .userId(JANE_DOE_ID)
        .githubRepository(
            new GithubRepository()
                .id(null)
                .name("some_random_repo" + id)
                .description("some_random_description")
                .isPrivate(true)
                .installationId(GH_APP_INSTALL_1_ID))
        .archived(false)
        .orgId(JANE_DOE_MAIN_ORG_ID)
        .applicationImport(null);
  }

  private static ApplicationBase randomLoremIpsumAppWithoutRepoId() {
    var id = randomUUID().toString();
    return new ApplicationBase()
        .id(id)
        .name(id.length() > 11 ? id.substring(0, 11) : id)
        .userId(LOREM_IPSUM_ID)
        .githubRepository(
            new GithubRepository()
                .id(null)
                .name("some_random_repo" + id)
                .description("some_random_description")
                .isPrivate(true)
                .installationId(GH_APP_INSTALL_1_ID))
        .archived(false)
        .orgId(LOREM_IPSUM_MAIN_ORG_ID)
        .applicationImport(null);
  }

  @Disabled
  @Test
  void not_e2e_user_cannot_have_more_than_2_apps() {
    ApiClient janeDoeClient = janeDoeClient();
    ApplicationApi api = new ApplicationApi(janeDoeClient);
    ApplicationBase toCreate = randomAppWithoutRepoId();
    ApplicationBase toCreate2 = randomAppWithoutRepoId();

    List<ApplicationBase> creationPayload = List.of(toCreate, toCreate2);
    assertThrowsBadRequestException(
        () -> {
          api.crupdateApplications(
              JANE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));
        },
        "cannot have more than 2 non archived apps per user");
  }

  @Test
  void archived_app_name_can_be_reused() throws ApiException {
    ApiClient janeDoeClient = janeDoeClient();
    ApplicationApi api = new ApplicationApi(janeDoeClient);

    ApplicationBase toCreate =
        new ApplicationBase()
            .id("new_poja_application")
            .name("arch-poja")
            .userId(JANE_DOE_ID)
            .orgId(JANE_DOE_MAIN_ORG_ID)
            .githubRepository(new GithubRepository().isPrivate(false))
            .archived(false)
            .applicationImport(null);

    List<Application> expectedResponseData =
        Stream.of(toCreate)
            .map(ApplicationIT::toApplication)
            .map(a -> a.status(UNKNOWN))
            .map(ApplicationIT::ignoreIds)
            .map(ApplicationIT::ignoreRepositoryUrls)
            .toList();
    var createApplicationResponse =
        api.crupdateApplications(
            JANE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(List.of(toCreate)));
    var createApplicationResponseData =
        requireNonNull(createApplicationResponse.getData()).stream()
            .map(ApplicationIT::ignoreIds)
            .map(ApplicationIT::ignoreRepositoryUrls)
            .toList();

    assertTrue(createApplicationResponseData.containsAll(expectedResponseData));
  }

  @Test
  void joe_doe_read_owned_applications_ok() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);
    BoundedPageSize boundedPageSize = new BoundedPageSize(1);

    PagedUserApplicationsResponse pagedUserApplications =
        api.getUserApplications(
            JOE_DOE_ID, new PageFromOne(1).getValue(), boundedPageSize.getValue());
    var userIdFilteredApplications = requireNonNull(pagedUserApplications.getData());

    assertTrue(requireNonNull(userIdFilteredApplications).contains(joePojaApplication1()));
    assertFalse(requireNonNull(userIdFilteredApplications).contains(joeArchivedPojaApplication1()));
    Page<api.poja.io.repository.model.Application> allFromOrgsOwnedByUserByCriteria =
        applicationRepository.findAllFromOrgsOwnedByUserByCriteria(JOE_DOE_ID, false, unpaged());

    assertTrue(allFromOrgsOwnedByUserByCriteria.getSize() > boundedPageSize.getValue());
    assertEquals(
        allFromOrgsOwnedByUserByCriteria.getSize(), pagedUserApplications.getUserAppsCount());
  }

  @Test
  void premium_user_can_premium_app_allowed_apps() throws ApiException {
    ApiClient loremIpsumClient = loremIpsumClient();
    ApplicationApi api = new ApplicationApi(loremIpsumClient);

    var premiumOffer = premiumOfferDomain();
    when(userSubscriptionService.findActiveSubscriptionByUserId(LOREM_IPSUM_ID))
        .thenReturn(Optional.of(UserSubscription.builder().offer(premiumOffer).build()));

    long premiumAllowedAppNb = premiumOffer.getMaxApps();
    var loremIpsumApps =
        applicationRepository.findAllByUserIdAndArchived(LOREM_IPSUM_ID, false).size();
    var createdAppIds = new ArrayList<String>();
    for (int i = loremIpsumApps; i < premiumAllowedAppNb; i++) {
      createdAppIds.add(assert_lorem_ipsum_can_create_app(api));
    }
    assertThrowsBadRequestException(
        () -> {
          ApplicationBase toCreate = randomLoremIpsumAppWithoutRepoId();
          List<ApplicationBase> creationPayload = List.of(toCreate);
          api.crupdateApplications(
              LOREM_IPSUM_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));
        },
        "cannot have more than 12 non archived apps per user. you have 12 non archived apps");
    applicationRepository.deleteAllById(createdAppIds);
  }

  @Test
  void jane_doe_can_own_beta_tester_allowed_apps() throws ApiException {
    ApiClient janeDoeClient = janeDoeClient();
    ApplicationApi api = new ApplicationApi(janeDoeClient);

    var janeDoeApps = applicationRepository.findAllByUserIdAndArchived(JANE_DOE_ID, false).size();
    var janeDoeAllowAppNb = BETA_USER_ALLOWED_APP_NB;
    var createdAppIds = new ArrayList<String>();
    for (int i = janeDoeApps; i < janeDoeAllowAppNb; i++) {
      createdAppIds.add(assert_jane_doe_can_create_app(api));
    }
    assertThrowsBadRequestException(
        () -> {
          ApplicationBase toCreate = randomJaneDoeAppWithoutRepoId();
          List<ApplicationBase> creationPayload = List.of(toCreate);
          api.crupdateApplications(
              JANE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));
        },
        "cannot have more than "
            + BETA_USER_ALLOWED_APP_NB
            + " non archived apps per user. you have "
            + janeDoeAllowAppNb
            + " non archived apps");
    applicationRepository.deleteAllById(createdAppIds);
  }

  @Test
  void cannot_create_app_if_org_is_not_set_up() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    ApplicationApi api = new ApplicationApi(joeDoeClient);
    var toCreate =
        new ApplicationBase()
            .id(POJA_APPLICATION_ID + "_5")
            .name(POJA_APPLICATION_NAME + "-5")
            .githubRepository(POJA_APPLICATION_GITHUB_REPOSITORY_WITHOUT_ID)
            .userId(JOE_DOE_ID)
            .orgId(ORG_3_ID)
            .archived(false);

    assertThrowsBadRequestException(
        () ->
            api.crupdateApplications(
                ORG_3_ID, new CrupdateApplicationsRequestBody().data(List.of(toCreate))),
        "Organization with id = " + ORG_3_ID + " is in state " + ORGANIZATION_SETUP_FAILED);
  }

  private static String assert_jane_doe_can_create_app(ApplicationApi api) throws ApiException {
    ApplicationBase toCreate = randomJaneDoeAppWithoutRepoId();
    List<ApplicationBase> creationPayload = List.of(toCreate);
    var createApplicationResponse =
        api.crupdateApplications(
            JANE_DOE_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));
    List<Application> createApplicationResponseData =
        requireNonNull(createApplicationResponse.getData()).stream()
            .map(ApplicationIT::ignoreIds)
            .toList();

    List<Application> expectedResponseData =
        creationPayload.stream()
            .map(ApplicationIT::toApplication)
            .map(a -> a.status(UNKNOWN))
            .map(ApplicationIT::ignoreIds)
            .toList();
    assertEquals(
        expectedResponseData.getFirst().getId(), createApplicationResponseData.getFirst().getId());
    return toCreate.getId();
  }

  private static String assert_lorem_ipsum_can_create_app(ApplicationApi api) throws ApiException {
    ApplicationBase toCreate = randomLoremIpsumAppWithoutRepoId();
    List<ApplicationBase> creationPayload = List.of(toCreate);
    var createApplicationResponse =
        api.crupdateApplications(
            LOREM_IPSUM_MAIN_ORG_ID, new CrupdateApplicationsRequestBody().data(creationPayload));
    List<Application> createApplicationResponseData =
        requireNonNull(createApplicationResponse.getData()).stream()
            .map(ApplicationIT::ignoreIds)
            .toList();

    List<Application> expectedResponseData =
        creationPayload.stream()
            .map(ApplicationIT::toApplication)
            .map(a -> a.status(Application.StatusEnum.UNKNOWN))
            .map(ApplicationIT::ignoreIds)
            .toList();
    assertEquals(
        expectedResponseData.getFirst().getId(), createApplicationResponseData.getFirst().getId());
    return toCreate.getId();
  }
}
