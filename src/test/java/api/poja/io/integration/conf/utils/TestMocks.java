package api.poja.io.integration.conf.utils;

import static api.poja.io.endpoint.rest.model.ApplicationImportStatusEnum.PENDING;
import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.HTTP_API;
import static api.poja.io.endpoint.rest.model.DatabaseConf2.WithDatabaseEnum.NONE;
import static api.poja.io.endpoint.rest.model.DurationUnit.MINUTES;
import static api.poja.io.endpoint.rest.model.Environment.StateEnum.HEALTHY;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.endpoint.rest.model.EventStackSource._1;
import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static api.poja.io.endpoint.rest.model.ExecutionType.SYNCHRONOUS;
import static api.poja.io.endpoint.rest.model.MonthType.JANUARY;
import static api.poja.io.endpoint.rest.model.OrganizationInviteType.ACCEPTED;
import static api.poja.io.endpoint.rest.model.StackResourceStatusType.CREATE_COMPLETE;
import static api.poja.io.endpoint.rest.model.StackResourceStatusType.CREATE_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.StackResourceStatusType.UPDATE_IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.COMPLETED;
import static api.poja.io.endpoint.rest.model.UserPaymentSetupStatusEnum.IN_PROGRESS;
import static api.poja.io.endpoint.rest.model.UserRoleEnum.USER;
import static api.poja.io.endpoint.rest.model.UserStatusEnum.ACTIVE;
import static api.poja.io.endpoint.rest.model.UserStatusEnum.SUSPENDED;
import static api.poja.io.endpoint.rest.model.UserStatusEnum.UNDER_MODIFICATION;
import static api.poja.io.endpoint.rest.model.WithQueuesNbEnum.NUMBER_2;
import static api.poja.io.integration.conf.utils.TestUtils.APP_INSTALLATION_1_ID;
import static api.poja.io.model.PojaVersion.POJA_1;
import static api.poja.io.model.PojaVersion.POJA_2;
import static api.poja.io.model.PojaVersion.POJA_3;
import static api.poja.io.model.PojaVersion.POJA_4;
import static api.poja.io.model.PojaVersion.POJA_5;
import static api.poja.io.model.PojaVersion.POJA_6;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.PojaVersion.POJA_8;
import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.service.github.model.GhAppInstallation.RepositorySelection.ALL;
import static api.poja.io.service.pricing.PricingMethod.TEN_MICRO;
import static api.poja.io.service.workflows.userState.UserStateService.COMPUTING_USER_STATE_REASON;
import static java.net.URI.create;

import api.poja.io.endpoint.rest.model.Application;
import api.poja.io.endpoint.rest.model.ApplicationBase;
import api.poja.io.endpoint.rest.model.ApplicationImportLog;
import api.poja.io.endpoint.rest.model.ApplicationImportLogTypeEnum;
import api.poja.io.endpoint.rest.model.ApplicationImportState;
import api.poja.io.endpoint.rest.model.ApplicationImportStateEnum;
import api.poja.io.endpoint.rest.model.ApplicationTemplate;
import api.poja.io.endpoint.rest.model.ComputeConf;
import api.poja.io.endpoint.rest.model.ComputeConf2;
import api.poja.io.endpoint.rest.model.ComputeConf9;
import api.poja.io.endpoint.rest.model.ConcurrencyConf;
import api.poja.io.endpoint.rest.model.ConcurrencyConf2;
import api.poja.io.endpoint.rest.model.ConcurrencyConf9;
import api.poja.io.endpoint.rest.model.DatabaseConf;
import api.poja.io.endpoint.rest.model.DatabaseConf2;
import api.poja.io.endpoint.rest.model.Duration;
import api.poja.io.endpoint.rest.model.EnvBillingInfo;
import api.poja.io.endpoint.rest.model.EnvVar;
import api.poja.io.endpoint.rest.model.Environment;
import api.poja.io.endpoint.rest.model.ExecutionType;
import api.poja.io.endpoint.rest.model.GenApiClientConf;
import api.poja.io.endpoint.rest.model.GeneralPojaConf;
import api.poja.io.endpoint.rest.model.GeneralPojaConf2;
import api.poja.io.endpoint.rest.model.GeneralPojaConf6;
import api.poja.io.endpoint.rest.model.GeneralPojaConf7;
import api.poja.io.endpoint.rest.model.GetUserResponse;
import api.poja.io.endpoint.rest.model.GithubRepository;
import api.poja.io.endpoint.rest.model.IntegrationConf;
import api.poja.io.endpoint.rest.model.MailingConf;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.Offer;
import api.poja.io.endpoint.rest.model.OrgBillingInfo;
import api.poja.io.endpoint.rest.model.Organization;
import api.poja.io.endpoint.rest.model.OrganizationInvite;
import api.poja.io.endpoint.rest.model.OrganizationInviteType;
import api.poja.io.endpoint.rest.model.OrganizationSetupState;
import api.poja.io.endpoint.rest.model.OrganizationSetupStatusEnum;
import api.poja.io.endpoint.rest.model.PojaConf1;
import api.poja.io.endpoint.rest.model.PojaConf2;
import api.poja.io.endpoint.rest.model.PojaConf3;
import api.poja.io.endpoint.rest.model.PojaConf4;
import api.poja.io.endpoint.rest.model.PojaConf5;
import api.poja.io.endpoint.rest.model.PojaConf6;
import api.poja.io.endpoint.rest.model.PojaConf7;
import api.poja.io.endpoint.rest.model.PojaConf8;
import api.poja.io.endpoint.rest.model.PojaConf9;
import api.poja.io.endpoint.rest.model.ScheduledTask;
import api.poja.io.endpoint.rest.model.ScheduledTask9;
import api.poja.io.endpoint.rest.model.StackEvent;
import api.poja.io.endpoint.rest.model.TestingConf;
import api.poja.io.endpoint.rest.model.User;
import api.poja.io.endpoint.rest.model.UserBillingDiscount;
import api.poja.io.endpoint.rest.model.UserBillingInfo;
import api.poja.io.endpoint.rest.model.UserBillingInfoWithAws;
import api.poja.io.endpoint.rest.model.UserCost;
import api.poja.io.endpoint.rest.model.UserPaymentSetupState;
import api.poja.io.endpoint.rest.model.UserState;
import api.poja.io.endpoint.rest.model.UserStatusEnum;
import api.poja.io.endpoint.rest.model.Worker;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.enums.ApplicationImportStatus;
import com.stripe.model.Address;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestMocks {
  public static final String BAD_TOKEN = "BadToken";
  public static final String NO_MATCHING_DB_ACCOUNT_TOKEN = "NoMatchingAccountToken";
  public static final String JOE_DOE_ID = "joe-doe-id";
  public static final String NEW_USER_GITHUB_ID = "8987";
  public static final String NEW_USER_TOKEN = "new_user_github_token";
  public static final String A_GITHUB_APP_TOKEN = "github_app_token";
  public static final String JOE_DOE_EMAIL = "joe@email.com";
  public static final String JOE_DOE_GITHUB_ID = "1234";
  public static final String JOE_DOE_USERNAME = "JoeDoe";
  public static final String JOE_DOE_AVATAR =
      "https://github.com/images/" + JOE_DOE_USERNAME + ".png";
  public static final String JOE_DOE_TOKEN = "joe_doe_token";
  public static final String JOE_DOE_STRIPE_ID = "joe_stripe_id";
  public static final String JOE_DOE_MAIN_ORG_ID = "org-" + JOE_DOE_USERNAME + "-id";
  public static final String ADMIN_ID = "admin_id";
  public static final String ADMIN_TOKEN = "admin_token";
  public static final String ADMIN_GITHUB_ID = "1007";
  public static final String SUSPENDED_ID = "suspended_id";
  public static final String SUSPENDED_TOKEN = "suspended_token";
  public static final String NOOBIE_ID = "noobie_id";
  public static final String NOOBIE_TOKEN = "noobie_token";
  public static final String NOOBIE_GITHUB_ID = "1902";
  public static final String SUSPENDED_GITHUB_ID = "1008";
  public static final String JANE_DOE_ID = "jane_doe_id";
  public static final String JANE_DOE_TOKEN = "jane_doe_token";
  public static final String JANE_DOE_EMAIL = "jane@email.com";
  public static final String JANE_DOE_GITHUB_ID = "4321";
  public static final String JANE_DOE_USERNAME = "JaneDoe";
  public static final String JANE_DOE_AVATAR =
      "https://github.com/images/" + JANE_DOE_USERNAME + ".png";
  public static final String JANE_DOE_STRIPE_ID = "jane_stripe_id";
  public static final String JANE_DOE_MAIN_ORG_ID = "org-JaneDoe-id";
  public static final String DENIS_RITCHIE_TOKEN = "denis_ritchie_token";
  public static final String DENIS_RITCHIE_ID = "denis_ritchie_id";
  public static final String DENIS_RITCHIE_GITHUB_ID = "1010";
  public static final String LOREM_IPSUM_TOKEN = "lorem_ipsum_token";
  public static final String LOREM_IPSUM_ID = "lorem_ipsum_id";
  public static final String LOREM_IPSUM_GITHUB_ID = "4567";
  public static final String LOREM_IPSUM_MAIN_ORG_ID = "org-LoremIpsum-id";
  public static final String USER_TO_UPSERT_ID = "to_upsert_id";
  public static final String TO_UPSERT_STRIPE_CUSTOMER_ID = "stripe-customer-id";
  public static final String ARCHIVED_TOKEN = "archived_token";
  public static final String ARCHIVED_GITHUB_ID = "1009";
  public static final String POJA_CREATED_STACK_ID = "poja_created_stack_id";
  public static final String POJA_CF_STACK_ID = "poja_cf_stack_id";
  public static final String POJA_APPLICATION_ID = "poja_application_id";
  public static final String POJA_APPLICATION_NAME = "poja-app";
  public static final String POJA_APPLICATION_ENVIRONMENT_ID = "poja_application_environment_id";
  public static final String GH_APP_INSTALL_1_ID = "gh_app_install_1_id";
  public static final String GH_APP_INSTALL_2_ID = "gh_app_install_2_id";
  public static final String POJA_APPLICATION_REPO_ID = "gh_repository_1_id";
  public static final String CRUPDATE_ENVIRONMENT_ID = "crupdate_environment_id";
  public static final String ENV_CONF_ID = "env_conf_id";
  public static final GithubRepository POJA_APPLICATION_GITHUB_REPOSITORY =
      new GithubRepository()
          .id(POJA_APPLICATION_REPO_ID)
          .name("poja_application")
          .isPrivate(false)
          .description("a regular poja app")
          .installationId(GH_APP_INSTALL_1_ID)
          .htmlUrl(create("http://github.com/user/repo"));
  public static final GithubRepository POJA_APPLICATION_GITHUB_REPOSITORY_WITHOUT_ID =
      new GithubRepository()
          .name("poja_application")
          .isPrivate(false)
          .description("a regular poja app")
          .installationId(GH_APP_INSTALL_1_ID)
          .htmlUrl(create("http://github.com/user/repo"));
  public static final Instant POJA_APPLICATION_CREATION_DATETIME =
      Instant.parse("2023-06-18T10:15:30.00Z");
  public static final String EVENT_STACK_ID = "event_stack_1_id";
  public static final String EVENT_STACK_NAME = "poja_app_event_stack";
  public static final String BUCKET_STACK_ID = "bucket_stack_1_id";
  public static final String BUCKET_STACK_NAME = "poja_app_bucket_stack";
  public static final String COMPUTE_PERM_STACK_ID = "compute_perm_stack_1_id";
  public static final String COMPUTE_PERM_STACK_NAME = "poja_app_compute_perm_stack";
  public static final String OTHER_POJA_APPLICATION_ID = "other_poja_application_id";
  public static final String OTHER_POJA_APPLICATION_ENVIRONMENT_ID =
      "other_poja_application_environment_id";
  public static final String OTHER_POJA_APPLICATION_ENVIRONMENT_2_ID =
      "other_poja_application_environment_2_id";
  public static final String PROD_COMPUTE_FRONTAL_FUNCTION = "prod-compute-frontal-function";
  public static final String PROD_COMPUTE_WORKER_1_FUNCTION = "prod-compute-worker-1-function";
  public static final String PROD_COMPUTE_WORKER_2_FUNCTION = "prod-compute-worker-2-function";
  public static final Instant BILLING_INFO_START_TIME_QUERY =
      Instant.parse("2024-09-01T00:00:00.00Z");
  public static final Instant BILLING_INFO_END_TIME_QUERY =
      Instant.parse("2024-09-30T23:59:59.00Z");
  public static final Instant JOINED_FROM = Instant.parse("2024-01-01T00:00:00Z");
  public static final Instant JOINED_TO = Instant.parse("2024-09-01T00:00:00Z");
  public static final Instant BILLING_INFO_START_TIME_QUERY1 =
      Instant.parse("2024-01-01T00:00:00.00Z");
  public static final Instant BILLING_INFO_END_TIME_QUERY1 =
      Instant.parse("2024-04-30T23:59:59.00Z");
  public static final Instant JOINED1_TO = Instant.parse("2024-05-15T00:00:00Z");
  public static final String POJA_APPLICATION_2_ID = "poja_application_2_id";
  public static final String POJA_APPLICATION_5_ID = "poja_application_5_id";
  public static final String ORG_1_ID = "org_1_id";
  public static final String ORG_2_ID = "org_2_id";
  public static final String ORG_3_ID = "org_3_id";
  public static final String ORG_5_ID = "org_5_id";
  public static final String ORG_1_NAME = "org_1_name";
  public static final String ORG_2_NAME = "org_2_name";
  public static final String INVITE_TO_CANCEL_ID = "invite_17_id";
  public static final String PREMIUM_OFFER_ID = "cb038529-dea0-43ab-b9bc-262ab668f150";
  public static final String ENV_DEPLOYMENT_CONF_1_ID = "env_deployment_conf_1_id";
  public static final String APP_IMPORT_1_ID = "import_1";
  public static final String APP_IMPORT_1_APP_NAME = "to_import";
  public static final String APP_IMPORT_4_ID = "import_4";
  public static final String APP_IMPORT_6_ID = "import_6";
  public static final String APP_IMPORT_7_ID = "import_7";
  public static final String JOE_DOE_DEFAULT_PAYMENT_METHOD_ID = "dummy";

  public static Customer joeDoeStripeCustomer() {
    var invoiceSettings = new Customer.InvoiceSettings();
    invoiceSettings.setDefaultPaymentMethod(JOE_DOE_DEFAULT_PAYMENT_METHOD_ID);

    Customer customer = new Customer();
    customer.setId(JOE_DOE_STRIPE_ID);
    customer.setName("stripe customer");
    customer.setEmail("test@example.com");
    customer.setInvoiceSettings(invoiceSettings);
    return customer;
  }

  public static Customer joeDoeStripeCustomerWithLocation() {
    var invoiceSettings = new Customer.InvoiceSettings();
    invoiceSettings.setDefaultPaymentMethod(JOE_DOE_DEFAULT_PAYMENT_METHOD_ID);

    var address = new Address();
    address.setCity("Paris");
    address.setCountry("FR");
    address.setPostalCode("123");
    address.setLine1("Rue 123");

    Customer customer = new Customer();
    customer.setId(JOE_DOE_STRIPE_ID);
    customer.setName("stripe customer");
    customer.setEmail("test@example.com");
    customer.setInvoiceSettings(invoiceSettings);
    customer.setAddress(address);
    return customer;
  }

  public static Customer toUpsertStripeCustomer() {
    Customer customer = new Customer();
    customer.setId(TO_UPSERT_STRIPE_CUSTOMER_ID);
    customer.setName("to upsert");
    customer.setEmail("to@upsert.com");
    return customer;
  }

  public static PaymentMethod paymentMethod() {
    PaymentMethod.Card card = new PaymentMethod.Card();
    card.setBrand("visa");
    card.setLast4("4242");
    card.setExpMonth(12L);
    card.setExpYear(2025L);

    var billingDetails = new PaymentMethod.BillingDetails();
    var address = new Address();
    address.setCountry("payment_method_country");
    billingDetails.setAddress(address);

    PaymentMethod paymentMethod = new PaymentMethod();
    paymentMethod.setId("payment_method_id");
    paymentMethod.setType("card");
    paymentMethod.setCard(card);
    paymentMethod.setCustomer(JOE_DOE_STRIPE_ID);
    paymentMethod.setBillingDetails(billingDetails);
    return paymentMethod;
  }

  public static List<PaymentMethod> paymentMethods() {
    List<PaymentMethod> paymentMethods = new ArrayList<>();
    paymentMethods.add(paymentMethod());
    return paymentMethods;
  }

  public static User joeDoeUser() {
    return new User()
        .id(JOE_DOE_ID)
        .email(JOE_DOE_EMAIL)
        .username(JOE_DOE_USERNAME)
        .roles(List.of(USER))
        .firstName("Joe")
        .lastName("Doe")
        .githubId(JOE_DOE_GITHUB_ID)
        .avatar(JOE_DOE_AVATAR)
        .stripeId(JOE_DOE_STRIPE_ID)
        .isBetaTester(true)
        .status(ACTIVE)
        .isArchived(false)
        .mainOrgId(JOE_DOE_MAIN_ORG_ID)
        .suspensionDurationInSeconds(0L)
        .statusUpdatedAt(Instant.parse("2024-03-25T12:00:00Z"))
        .joinedAt(Instant.parse("2024-03-25T12:00:00.00Z"))
        .lastConnection(Instant.parse("2025-10-01T14:00:00.00Z"));
  }

  public static GetUserResponse joeDoeUserResponse() {
    return new GetUserResponse()
        .id(JOE_DOE_ID)
        .email(JOE_DOE_EMAIL)
        .username(JOE_DOE_USERNAME)
        .firstName("Joe")
        .lastName("Doe")
        .avatar(JOE_DOE_AVATAR)
        .archived(false)
        .status(UserStatusEnum.ACTIVE)
        .suspensionDurationInSeconds(0L)
        .statusUpdatedAt(Instant.parse("2024-03-25T12:00:00Z"))
        .joinedAt(Instant.parse("2024-03-25T12:00:00.00Z"))
        .activeSubscriptionId(null)
        .latestSubscriptionId(null)
        .lastConnection(Instant.parse("2025-10-01T14:00:00.00Z"));
  }

  public static Organization joeDoeMainOrg() {
    return new Organization()
        .id(JOE_DOE_MAIN_ORG_ID)
        .name("org-" + JOE_DOE_USERNAME)
        .ownerId(JOE_DOE_ID);
  }

  public static User janeDoeUser() {
    return new User()
        .id(JANE_DOE_ID)
        .email(JANE_DOE_EMAIL)
        .username(JANE_DOE_USERNAME)
        .roles(List.of(USER))
        .firstName("Jane")
        .lastName("Doe")
        .githubId(JANE_DOE_GITHUB_ID)
        .avatar(JANE_DOE_AVATAR)
        .status(ACTIVE)
        .isBetaTester(true)
        .isArchived(false)
        .stripeId(JANE_DOE_STRIPE_ID)
        .mainOrgId(JANE_DOE_MAIN_ORG_ID)
        .statusUpdatedAt(Instant.parse("2024-03-25T16:00:00Z"))
        .suspensionDurationInSeconds(0L)
        .joinedAt(Instant.parse("2024-03-25T12:00:00.00Z"))
        .lastConnection(Instant.parse("2025-10-01T14:00:00.00Z"));
  }

  public static GetUserResponse janeDoeUserResponse() {
    return new GetUserResponse()
        .id(JANE_DOE_ID)
        .email(JANE_DOE_EMAIL)
        .username(JANE_DOE_USERNAME)
        .firstName("Jane")
        .lastName("Doe")
        .avatar(JANE_DOE_AVATAR)
        .archived(false)
        .status(UserStatusEnum.ACTIVE)
        .activeSubscriptionId(null)
        .latestSubscriptionId(null)
        .statusUpdatedAt(Instant.parse("2024-03-25T16:00:00Z"))
        .joinedAt(Instant.parse("2024-03-25T12:00:00.00Z"))
        .suspensionDurationInSeconds(0L)
        .lastConnection(Instant.parse("2025-10-01T14:00:00.00Z"));
  }

  public static List<Environment> pojaAppEnvironments() {
    return List.of(pojaAppProdEnvironment());
  }

  public static Environment pojaAppProdEnvironment() {
    return new Environment()
        .id(POJA_APPLICATION_ENVIRONMENT_ID)
        .environmentType(PROD)
        .status(Environment.StatusEnum.ACTIVE)
        .state(HEALTHY);
  }

  public static ApplicationBase applicationToCreate() {
    return new ApplicationBase()
        .id(POJA_APPLICATION_ID + "_4")
        .name(POJA_APPLICATION_NAME + "-4")
        .userId(JOE_DOE_ID)
        .githubRepository(POJA_APPLICATION_GITHUB_REPOSITORY_WITHOUT_ID)
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .archived(false)
        .applicationImport(null);
  }

  public static Application joePojaApplication1() {
    return new Application()
        .id(POJA_APPLICATION_ID)
        .name(POJA_APPLICATION_NAME)
        .userId(JOE_DOE_ID)
        .creationDatetime(POJA_APPLICATION_CREATION_DATETIME)
        .githubRepository(POJA_APPLICATION_GITHUB_REPOSITORY)
        .archived(false)
        .status(Application.StatusEnum.ACTIVE)
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .applicationImport(null);
  }

  public static Application joePojaApplication2() {
    return new Application()
        .id(POJA_APPLICATION_2_ID)
        .name("poja-app-2")
        .userId(JOE_DOE_ID)
        .creationDatetime(Instant.parse("2023-06-18T10:16:30.00Z"))
        .githubRepository(
            new GithubRepository()
                .id("gh_repository_2_id")
                .name("poja_application_2")
                .isPrivate(false)
                .description("a regular poja app")
                .installationId(GH_APP_INSTALL_1_ID)
                .htmlUrl(create("http://github.com/user/repo")))
        .archived(false)
        .status(Application.StatusEnum.UNKNOWN)
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .applicationImport(null);
  }

  public static Application joeArchivedPojaApplication1() {
    return new Application()
        .id(POJA_APPLICATION_5_ID)
        .name(POJA_APPLICATION_NAME + "-5")
        .userId(JOE_DOE_ID)
        .creationDatetime(POJA_APPLICATION_CREATION_DATETIME)
        .githubRepository(POJA_APPLICATION_GITHUB_REPOSITORY)
        .archived(true)
        .status(Application.StatusEnum.UNKNOWN)
        .orgId(JOE_DOE_MAIN_ORG_ID);
  }

  public static Application janePojaApplication() {
    return new Application()
        .id("poja_application_3_id")
        .name("poja-app-3")
        .userId(JANE_DOE_ID)
        .creationDatetime(Instant.parse("2023-06-18T10:17:30.00Z"))
        .githubRepository(POJA_APPLICATION_GITHUB_REPOSITORY)
        .archived(false)
        .status(Application.StatusEnum.ACTIVE)
        .orgId(JANE_DOE_MAIN_ORG_ID);
  }

  public static PojaConf1 getValidPojaConf1() {
    String humanReadableValuePojaVersion = POJA_1.toHumanReadableValue();
    return new PojaConf1()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf()
                .appName("appname")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .withQueuesNb(NUMBER_2)
                .customJavaDeps(List.of())
                .customJavaEnvVars(Map.of())
                .customJavaRepositories(List.of()))
        .database(
            new DatabaseConf()
                .withDatabase(DatabaseConf.WithDatabaseEnum.NONE)
                .databaseNonRootPassword(null)
                .databaseNonRootUsername(null)
                .prodDbClusterTimeout(null)
                .auroraAutoPause(null)
                .auroraMaxCapacity(null)
                .auroraMinCapacity(null)
                .auroraSleep(null)
                .auroraScalePoint(null))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .workerMemory(BigDecimal.valueOf(512))
                .workerBatch(BigDecimal.valueOf(5))
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(600)))
        .concurrency(
            new ConcurrencyConf()
                .frontalReservedConcurrentExecutionsNb(5)
                .workerReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"));
  }

  public static PojaConf2 getValidPojaConf2() {
    String humanReadableValuePojaVersion = POJA_2.toHumanReadableValue();
    return new PojaConf2()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf2()
                .appName("appname")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(Map.of())
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(DatabaseConf2.WithDatabaseEnum.NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(HTTP_API)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask1")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf3 getValidPojaConf3(
      ComputeConf2.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    String humanReadableValuePojaVersion = POJA_3.toHumanReadableValue();
    return new PojaConf3()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf2()
                .appName("appname")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(Map.of())
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(DatabaseConf2.WithDatabaseEnum.NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask1")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf4 getValidPojaConf4(
      ComputeConf2.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    String humanReadableValuePojaVersion = POJA_4.toHumanReadableValue();
    return new PojaConf4()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf2()
                .appName("appname")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(Map.of())
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(DatabaseConf2.WithDatabaseEnum.NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask1")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf5 getValidPojaConf5(
      ComputeConf2.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    String humanReadableValuePojaVersion = POJA_5.toHumanReadableValue();
    return new PojaConf5()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf2()
                .appName("appname")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(Map.of())
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(DatabaseConf2.WithDatabaseEnum.NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask1")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf6 getValidPojaConf6(
      ComputeConf2.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    var humanReadableValuePojaVersion = POJA_6.toHumanReadableValue();
    return new PojaConf6()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf6()
                .appName("appname")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(
                    List.of(
                        new EnvVar()
                            .name("API_KEY")
                            .value("prod_api_key_123")
                            .testValue("test_api_key_456"),
                        new EnvVar().name("JWT_SECRET").value("prod_jwt_secret_dummy"),
                        new EnvVar()
                            .name("DATABASE_URL")
                            .value("postgres://prod.db.com:5432/mydb")
                            .testValue("postgres://test.db.com:5432/testdb")))
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask1")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf7 getValidPojaConf7(
      ComputeConf2.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    var humanReadableValuePojaVersion = POJA_7.toHumanReadableValue();
    return new PojaConf7()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf7()
                .appName("appname")
                .javaMainClass("PojaApplication")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(
                    List.of(
                        new EnvVar()
                            .name("API_KEY")
                            .value("prod_api_key_123")
                            .testValue("test_api_key_456"),
                        new EnvVar().name("JWT_SECRET").value("prod_jwt_secret_dummy"),
                        new EnvVar()
                            .name("DATABASE_URL")
                            .value("postgres://prod.db.com:5432/mydb")
                            .testValue("postgres://test.db.com:5432/testdb")))
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask1")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf8 getValidPojaConf8(
      ComputeConf2.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    var humanReadableValuePojaVersion = POJA_8.toHumanReadableValue();
    return new PojaConf8()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf7()
                .appName("appname")
                .javaMainClass("PojaApplication")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(
                    List.of(
                        new EnvVar()
                            .name("API_KEY")
                            .value("prod_api_key_123")
                            .testValue("test_api_key_456"),
                        new EnvVar().name("JWT_SECRET").value("prod_jwt_secret_dummy"),
                        new EnvVar()
                            .name("DATABASE_URL")
                            .value("postgres://prod.db.com:5432/mydb")
                            .testValue("postgres://test.db.com:5432/testdb")))
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf2()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .worker1Memory(BigDecimal.valueOf(512))
                .worker2Memory(BigDecimal.valueOf(513))
                .worker1Batch(BigDecimal.valueOf(5))
                .worker2Batch(BigDecimal.valueOf(6))
                .withQueuesNb(NUMBER_2)
                .workerFunction1Timeout(BigDecimal.valueOf(600))
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workerFunction2Timeout(BigDecimal.valueOf(700)))
        .concurrency(
            new ConcurrencyConf2()
                .frontalReservedConcurrentExecutionsNb(5)
                .worker1ReservedConcurrentExecutionsNb(5)
                .worker2ReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask()
                    .name("ScheduledTask")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(_1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static PojaConf9 getValidPojaConf9(
      ComputeConf9.FrontalFunctionInvocationMethodEnum invocationMethodEnum) {
    var humanReadableValuePojaVersion = POJA_9.toHumanReadableValue();
    return new PojaConf9()
        .version(humanReadableValuePojaVersion)
        .general(
            new GeneralPojaConf7()
                .appName("appname")
                .javaMainClass("PojaApplication")
                .packageFullName("com.test.api")
                .withSnapstart(false)
                .customJavaDeps(List.of())
                .customJavaEnvVars(
                    List.of(
                        new EnvVar()
                            .name("API_KEY")
                            .value("prod_api_key_123")
                            .testValue("test_api_key_456"),
                        new EnvVar().name("JWT_SECRET").value("prod_jwt_secret_dummy"),
                        new EnvVar()
                            .name("DATABASE_URL")
                            .value("postgres://prod.db.com:5432/mydb")
                            .testValue("postgres://test.db.com:5432/testdb")))
                .customJavaRepositories(List.of()))
        .database(new DatabaseConf2().withDatabase(NONE))
        .emailing(new MailingConf().sesSource("mail@mail.com"))
        .genApiClient(
            new GenApiClientConf()
                .awsAccountId(null)
                .tsClientDefaultOpenapiServerUrl(null)
                .tsClientApiUrlEnvVarName(null)
                .codeartifactRepositoryName(null)
                .codeartifactDomainName(null))
        .integration(
            new IntegrationConf()
                .withSentry(false)
                .withSwaggerUi(false)
                .withSonar(false)
                .withFileStorage(false)
                .withCodeql(false))
        .compute(
            new ComputeConf9()
                .frontalMemory(BigDecimal.valueOf(1024))
                .frontalFunctionTimeout(BigDecimal.valueOf(600))
                .frontalFunctionInvocationMethod(invocationMethodEnum)
                .apiGatewayTimeout(BigDecimal.valueOf(30000))
                .workers(
                    List.of(
                        new Worker()
                            .memory(BigDecimal.valueOf(512))
                            .batch(BigDecimal.valueOf(5))
                            .timeout(BigDecimal.valueOf(600))
                            .reservedConcurrentExecutionsNb(5),
                        new Worker()
                            .memory(BigDecimal.valueOf(513))
                            .batch(BigDecimal.valueOf(6))
                            .timeout(BigDecimal.valueOf(700))
                            .reservedConcurrentExecutionsNb(5),
                        new Worker()
                            .memory(BigDecimal.valueOf(2048))
                            .batch(BigDecimal.valueOf(10))
                            .timeout(BigDecimal.valueOf(900))
                            .reservedConcurrentExecutionsNb(25),
                        new Worker()
                            .memory(BigDecimal.valueOf(512))
                            .batch(BigDecimal.valueOf(1))
                            .timeout(BigDecimal.valueOf(30))
                            .reservedConcurrentExecutionsNb(null))))
        .concurrency(new ConcurrencyConf9().frontalReservedConcurrentExecutionsNb(5))
        .testing(
            new TestingConf().jacocoMinCoverage(BigDecimal.valueOf(0.2)).javaFacadeIt("FacadeIT"))
        .scheduledTasks(
            List.of(
                new ScheduledTask9()
                    .name("ScheduledTask")
                    .description("some scheduled task")
                    .className("classname")
                    .eventStackSource(1)
                    .scheduleExpression("cron(0 6 * * ? *)")));
  }

  public static List<StackEvent> permStackEvents() {
    StackEvent createInProgress =
        new StackEvent()
            .eventId("ExecutionRole-CREATE_IN_PROGRESS-2024-07-26T05:08:30.029Z")
            .logicalResourceId("ExecutionRole")
            .resourceType("AWS::IAM::Role")
            .timestamp(Instant.parse("2024-07-26T05:08:30.029Z"))
            .resourceStatus(CREATE_IN_PROGRESS)
            .statusMessage(null);
    StackEvent createComplete =
        new StackEvent()
            .eventId("ExecutionRole-CREATE_COMPLETE-2024-07-26T05:08:48.624Z")
            .logicalResourceId("ExecutionRole")
            .resourceType("AWS::IAM::Role")
            .timestamp(Instant.parse("2024-07-26T05:08:48.624Z"))
            .resourceStatus(CREATE_COMPLETE)
            .statusMessage(null);
    StackEvent updateInProgress =
        new StackEvent()
            .eventId("9094a550-4b12-11ef-804a-0642aee31ca5")
            .logicalResourceId("prod-compute-permission-poja-second")
            .resourceType("AWS::CloudFormation::Stack")
            .timestamp(Instant.parse("2024-07-26T05:47:37.873Z"))
            .resourceStatus(UPDATE_IN_PROGRESS)
            .statusMessage("User Initiated");
    return List.of(createInProgress, createComplete, updateInProgress);
  }

  public static EnvBillingInfo joeDoeBillingInfo1() {
    var duration = new Duration().amount(590.0).unit(MINUTES);
    return new EnvBillingInfo()
        .startTime(BILLING_INFO_START_TIME_QUERY)
        .appId(OTHER_POJA_APPLICATION_ID)
        .envId(OTHER_POJA_APPLICATION_ENVIRONMENT_ID)
        .endTime(BILLING_INFO_END_TIME_QUERY)
        .computedPrice(new BigDecimal("1045.23239217329813"))
        .pricingMethod(TEN_MICRO.getName())
        .computeTime(Instant.parse("2024-09-11T23:00:00.00Z"))
        .resourceInvocationTotalDuration(duration);
  }

  public static EnvBillingInfo joeDoeBillingInfo2() {
    var duration = new Duration().amount(390.0).unit(MINUTES);
    return new EnvBillingInfo()
        .startTime(BILLING_INFO_START_TIME_QUERY)
        .endTime(BILLING_INFO_END_TIME_QUERY)
        .appId(OTHER_POJA_APPLICATION_ID)
        .envId(OTHER_POJA_APPLICATION_ENVIRONMENT_2_ID)
        .computedPrice(new BigDecimal("761.227392137823"))
        .pricingMethod(TEN_MICRO.getName())
        .computeTime(Instant.parse("2024-09-20T14:00:00.00Z"))
        .resourceInvocationTotalDuration(duration);
  }

  public static UserBillingInfoWithAws joeDoeTotalBillingInfoWithAws() {
    var duration = new Duration().amount(1680.0).unit(MINUTES);
    var awsCost = new BigDecimal("1");
    var computedPrice = new BigDecimal("3042.58217648441926");
    return new UserBillingInfoWithAws()
        .userId(JOE_DOE_ID)
        .startTime(BILLING_INFO_START_TIME_QUERY)
        .endTime(BILLING_INFO_END_TIME_QUERY)
        .computeTime(Instant.parse("2024-09-30T23:00:00Z"))
        .computedPrice(computedPrice)
        .pricingMethod(TEN_MICRO.getName())
        .awsCost(awsCost)
        .costMargin(computedPrice.subtract(awsCost))
        .resourceInvocationTotalDuration(duration);
  }

  public static UserBillingInfo joeDoeTotalBillingInfo() {
    var duration = new Duration().amount(1680.0).unit(MINUTES);
    return new UserBillingInfo()
        .userId(JOE_DOE_ID)
        .startTime(BILLING_INFO_START_TIME_QUERY)
        .endTime(BILLING_INFO_END_TIME_QUERY)
        .computeTime(Instant.parse("2024-09-30T23:00:00Z"))
        .computedPrice(new BigDecimal("3042.58217648441926"))
        .pricingMethod(TEN_MICRO.getName())
        .resourceInvocationTotalDuration(duration);
  }

  public static OrgBillingInfo joeDoeMainOrgBillingInfo() {
    var duration = new Duration().amount(1680.0).unit(MINUTES);
    return new OrgBillingInfo()
        .userId(JOE_DOE_ID)
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .startTime(BILLING_INFO_START_TIME_QUERY)
        .endTime(BILLING_INFO_END_TIME_QUERY)
        .computeTime(Instant.parse("2024-09-30T23:00:00Z"))
        .computedPrice(new BigDecimal("3042.58217648441926"))
        .pricingMethod(TEN_MICRO.getName())
        .resourceInvocationTotalDuration(duration);
  }

  public static Organization joeDoeOrg() {
    return new Organization().id(ORG_1_ID).name(ORG_1_NAME).ownerId(JOE_DOE_ID);
  }

  public static Organization janeDoeOrg() {
    return new Organization().id(ORG_2_ID).name(ORG_2_NAME).ownerId(JANE_DOE_ID);
  }

  public static Organization janeDoeNewOrg() {
    return new Organization().id("jane_doe_new_org").name("jd_new_org").ownerId(JANE_DOE_ID);
  }

  public static List<Organization> janeDoeOrgs() {
    return List.of(joeDoeOrg(), janeDoeOrg());
  }

  public static Organization joeDoeOrg2() {
    return new Organization().id("new_org_id").name("new_org_name").ownerId(JOE_DOE_ID);
  }

  public static List<OrganizationInvite> joeDoeAcceptedOrgInvites() {
    return List.of(
        new OrganizationInvite()
            .id("invite_1_id")
            .userId(JOE_DOE_ID)
            .type(ACCEPTED)
            .invitedAt(Instant.parse("2025-02-21T00:00:00.00Z"))
            .orgId(ORG_1_ID),
        new OrganizationInvite()
            .id("invite_4_id")
            .userId(JOE_DOE_ID)
            .type(ACCEPTED)
            .invitedAt(Instant.parse("2025-02-21T00:00:00.00Z"))
            .orgId(ORG_3_ID));
  }

  public static List<OrganizationInvite> joeDoePendingOrgInvites() {
    return List.of(
        new OrganizationInvite()
            .id("invite_8_id")
            .userId(JOE_DOE_ID)
            .type(OrganizationInviteType.PENDING)
            .invitedAt(Instant.parse("2025-02-21T00:00:00.00Z"))
            .orgId(ORG_2_ID));
  }

  public static List<OrganizationInvite> joeDoeOrgPendingInvites() {
    return List.of(
        new OrganizationInvite()
            .id("invite_14_id")
            .userId(LOREM_IPSUM_ID)
            .type(OrganizationInviteType.PENDING)
            .orgId(JOE_DOE_MAIN_ORG_ID),
        new OrganizationInvite()
            .id("invite_15_id")
            .userId(JANE_DOE_ID)
            .type(OrganizationInviteType.PENDING)
            .orgId(JOE_DOE_MAIN_ORG_ID),
        new OrganizationInvite()
            .id("invite_16_id")
            .userId(DENIS_RITCHIE_ID)
            .type(OrganizationInviteType.PENDING)
            .orgId(JOE_DOE_MAIN_ORG_ID));
  }

  public static OrganizationInvite inviteToCancel() {
    return new OrganizationInvite()
        .id(INVITE_TO_CANCEL_ID)
        .type(OrganizationInviteType.PENDING)
        .orgId(JANE_DOE_MAIN_ORG_ID)
        .userId(JOE_DOE_ID);
  }

  public static UserBillingDiscount joeDoeBillingDiscount1() {
    return new UserBillingDiscount()
        .id("ubd_1_id")
        .userId(JOE_DOE_ID)
        .amount(BigDecimal.valueOf(2))
        .year(2024)
        .month(JANUARY)
        .description("gift")
        .creationDatetime(Instant.parse("2024-01-02T00:00:00.00Z"));
  }

  public static UserBillingDiscount joeDoeBillingDiscount2() {
    return new UserBillingDiscount()
        .id("ubd_2_id")
        .userId(JOE_DOE_ID)
        .amount(BigDecimal.valueOf(1))
        .year(2025)
        .month(MonthType.DECEMBER)
        .description("bonus")
        .creationDatetime(Instant.parse("2025-12-05T00:00:00.00Z"));
  }

  public static Offer premiumOffer() {
    return new Offer()
        .id(PREMIUM_OFFER_ID)
        .name("premium")
        .maxApps(12L)
        .price(BigDecimal.valueOf(1))
        .nbMaxSubscribers(1L)
        .remainingPlaces(1L);
  }

  public static List<OrganizationSetupState> joeDoeOrganizationSetupStates() {
    return List.of(
        new OrganizationSetupState()
            .id("joe_doe_org_setup_state_2")
            .executionType(ASYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T12:01:00.00Z"))
            .progressionStatus(OrganizationSetupStatusEnum.COMPLETED),
        new OrganizationSetupState()
            .id("joe_doe_org_setup_state_1")
            .executionType(ASYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T12:00:00.00Z"))
            .progressionStatus(OrganizationSetupStatusEnum.IN_PROGRESS));
  }

  public static List<UserPaymentSetupState> joeDoePaymentSetupStates() {
    return List.of(
        new UserPaymentSetupState()
            .id("joe_doe_payment_setup_state_2")
            .executionType(ASYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T12:01:00.00Z"))
            .progressionStatus(COMPLETED),
        new UserPaymentSetupState()
            .id("joe_doe_payment_setup_state_1")
            .executionType(ASYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T12:00:00.00Z"))
            .progressionStatus(IN_PROGRESS));
  }

  public static List<UserState> noobieStates() {
    return List.of(
        new UserState()
            .id("ns_5_id")
            .userId(NOOBIE_ID)
            .progressionStatus(ACTIVE)
            .description("admin: confirmed usual activity")
            .executionType(SYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T16:00:00.00Z")),
        new UserState()
            .id("ns_4_id")
            .userId(NOOBIE_ID)
            .progressionStatus(UNDER_MODIFICATION)
            .description(COMPUTING_USER_STATE_REASON)
            .executionType(SYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T15:00:00.00Z")),
        new UserState()
            .id("ns_3_id")
            .userId(NOOBIE_ID)
            .progressionStatus(SUSPENDED)
            .description("admin: suspicious activity")
            .executionType(SYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T14:00:00.00Z")),
        new UserState()
            .id("ns_2_id")
            .userId(NOOBIE_ID)
            .progressionStatus(UNDER_MODIFICATION)
            .description(COMPUTING_USER_STATE_REASON)
            .executionType(SYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T13:00:00.00Z")),
        new UserState()
            .id("ns_1_id")
            .userId(NOOBIE_ID)
            .progressionStatus(ACTIVE)
            .description(null)
            .executionType(SYNCHRONOUS)
            .timestamp(Instant.parse("2024-03-25T12:00:00.00Z")));
  }

  public static api.poja.io.repository.model.Offer premiumOfferDomain() {
    return api.poja.io.repository.model.Offer.builder()
        .id(PREMIUM_OFFER_ID)
        .name("premium")
        .maxApps(12L)
        .price(BigDecimal.valueOf(1))
        .build();
  }

  public static ApplicationImport pendingAppImport() {
    return ApplicationImport.builder()
        .id(APP_IMPORT_1_ID)
        .appName("to_import")
        .githubRepositoryName("dummy")
        .githubRepositoryHttpUrl("https://github.com/user/dummy")
        .orgId("org_1_id")
        .userId(JOE_DOE_ID)
        .appInstallationId("gh_app_install_1_id")
        .status(ApplicationImportStatus.PENDING)
        .pojaVersion(POJA_7.toHumanReadableValue())
        .build();
  }

  public static api.poja.io.endpoint.rest.model.ApplicationImport applicationImport1() {
    return new api.poja.io.endpoint.rest.model.ApplicationImport()
        .id(APP_IMPORT_1_ID)
        .name("to_import")
        .pojaVersion(POJA_7.toHumanReadableValue())
        .githubRepositoryName("dummy")
        .githubRepositoryHttpUrl("https://github.com/user/dummy")
        .orgId(ORG_1_ID)
        .userId(JOE_DOE_ID)
        .status(PENDING);
  }

  public static api.poja.io.endpoint.rest.model.ApplicationImport applicationImport6() {
    return new api.poja.io.endpoint.rest.model.ApplicationImport()
        .id(APP_IMPORT_6_ID)
        .name("to_import")
        .pojaVersion(POJA_7.toHumanReadableValue())
        .githubRepositoryName("dummy")
        .githubRepositoryHttpUrl("https://github.com/user/dummy")
        .orgId(ORG_1_ID)
        .userId(JOE_DOE_ID)
        .status(PENDING);
  }

  public static api.poja.io.endpoint.rest.model.ApplicationImport applicationImport7() {
    return new api.poja.io.endpoint.rest.model.ApplicationImport()
        .id(APP_IMPORT_7_ID)
        .name("to_import")
        .pojaVersion(POJA_7.toHumanReadableValue())
        .githubRepositoryName("dummy")
        .githubRepositoryHttpUrl("https://github.com/user/dummy")
        .orgId(ORG_1_ID)
        .userId(JOE_DOE_ID)
        .status(PENDING);
  }

  public static List<api.poja.io.endpoint.rest.model.ApplicationImport> org1_applicationImports() {
    return List.of(applicationImport1(), applicationImport6(), applicationImport7());
  }

  public static List<ApplicationImportState> applicationImport1States() {
    return List.of(
        new ApplicationImportState()
            .id("import_1_state_01")
            .progressionStatus(
                ApplicationImportStateEnum.APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:43:12Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_02")
            .progressionStatus(
                ApplicationImportStateEnum.APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T08:44:03Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_03")
            .progressionStatus(ApplicationImportStateEnum.BUILD_TOOL_VERIFICATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:45:10Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_04")
            .progressionStatus(ApplicationImportStateEnum.BUILD_TOOL_VERIFICATION_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T08:46:01Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_05")
            .progressionStatus(
                ApplicationImportStateEnum.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:46:50Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_06")
            .progressionStatus(
                ApplicationImportStateEnum.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T08:47:30Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_07")
            .progressionStatus(ApplicationImportStateEnum.CONVERSION_TO_GRADLE_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:47:19Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_08")
            .progressionStatus(ApplicationImportStateEnum.CONVERSION_TO_GRADLE_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T08:48:02Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_09")
            .progressionStatus(
                ApplicationImportStateEnum.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:50:55Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(
                List.of(
                    new ApplicationImportLog()
                        .content(
                            "Resolved dependency version conflict: spring-boot-starter-web 3.2.0 →"
                                + " 3.2.1")
                        .type(ApplicationImportLogTypeEnum.INFO)
                        .timestamp(Instant.parse("2024-09-01T08:50:56Z")),
                    new ApplicationImportLog()
                        .content("Merged build.gradle blocks: repositories + dependencies")
                        .type(ApplicationImportLogTypeEnum.INFO)
                        .timestamp(Instant.parse("2024-09-01T08:50:57Z")))),
        new ApplicationImportState()
            .id("import_1_state_10")
            .progressionStatus(ApplicationImportStateEnum.GRADLE_BUILD_FILE_CONFLICTS_RESOLVED)
            .timestamp(Instant.parse("2024-09-01T08:52:12Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_11")
            .progressionStatus(ApplicationImportStateEnum.POJA_CONF_GENERATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:52:45Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_12")
            .progressionStatus(ApplicationImportStateEnum.POJA_CONF_GENERATED)
            .timestamp(Instant.parse("2024-09-01T08:53:20Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_13")
            .progressionStatus(ApplicationImportStateEnum.CODE_GENERATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:53:40Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(
                List.of(
                    new ApplicationImportLog()
                        .content("Starting code generation using Poja template v4.1")
                        .type(ApplicationImportLogTypeEnum.INFO)
                        .timestamp(Instant.parse("2024-09-01T08:53:41Z")),
                    new ApplicationImportLog()
                        .content("Unused user-defined controller detected; skipping Poja override")
                        .type(ApplicationImportLogTypeEnum.WARNING)
                        .timestamp(Instant.parse("2024-09-01T08:53:42Z")))),
        new ApplicationImportState()
            .id("import_1_state_14")
            .progressionStatus(ApplicationImportStateEnum.GENERATED_CODE_INTEGRATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T08:54:18Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(
                List.of(
                    new ApplicationImportLog()
                        .content("Integrating generated code into user repository structure")
                        .type(ApplicationImportLogTypeEnum.INFO)
                        .timestamp(Instant.parse("2024-09-01T08:54:19Z")),
                    new ApplicationImportLog()
                        .content("Resolved file overwrite rules for src/main/java/api package")
                        .type(ApplicationImportLogTypeEnum.INFO)
                        .timestamp(Instant.parse("2024-09-01T08:54:20Z")))),
        new ApplicationImportState()
            .id("import_1_state_15")
            .progressionStatus(ApplicationImportStateEnum.GENERATED_CODE_INTEGRATED)
            .timestamp(Instant.parse("2024-09-01T08:55:33Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_18")
            .progressionStatus(ApplicationImportStateEnum.TEST_PING_ENDPOINT_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T09:00:14Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_1_state_19")
            .progressionStatus(ApplicationImportStateEnum.TEST_PING_ENDPOINT_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T09:01:09Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()));
  }

  public static List<ApplicationImportState> applicationImport6States() {
    return List.of(
        new ApplicationImportState()
            .id("import_6_state_01")
            .progressionStatus(
                ApplicationImportStateEnum.APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T10:12:05Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_6_state_02")
            .progressionStatus(
                ApplicationImportStateEnum.APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T10:12:44Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_6_state_03")
            .progressionStatus(ApplicationImportStateEnum.BUILD_TOOL_VERIFICATION_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T10:14:01Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_6_state_04")
            .progressionStatus(ApplicationImportStateEnum.BUILD_TOOL_VERIFICATION_SUCCESSFUL)
            .timestamp(Instant.parse("2024-09-01T10:14:47Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_6_state_05")
            .progressionStatus(ApplicationImportStateEnum.CONVERSION_TO_GRADLE_IN_PROGRESS)
            .timestamp(Instant.parse("2024-09-01T10:16:12Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(List.of()),
        new ApplicationImportState()
            .id("import_6_state_06")
            .progressionStatus(ApplicationImportStateEnum.CONVERSION_TO_GRADLE_FAILED)
            .timestamp(Instant.parse("2024-09-01T10:16:29Z"))
            .executionType(ExecutionType.ASYNCHRONOUS)
            .logs(
                List.of(
                    new ApplicationImportLog()
                        .content("Failed to detect a supported build tool automatically.")
                        .type(ApplicationImportLogTypeEnum.ERROR)
                        .timestamp(Instant.parse("2024-09-01T10:16:13Z")),
                    new ApplicationImportLog()
                        .content(
                            "Unable to identify a supported build tool from project files; aborting"
                                + " Gradle conversion.")
                        .type(ApplicationImportLogTypeEnum.ERROR)
                        .timestamp(Instant.parse("2024-09-01T10:16:14Z")),
                    new ApplicationImportLog()
                        .content("Stopping process and marking import as failed.")
                        .type(ApplicationImportLogTypeEnum.INFO)
                        .timestamp(Instant.parse("2024-09-01T10:16:15Z")))));
  }

  public static AppInstallation appInstall1() {
    return AppInstallation.builder()
        .id(GH_APP_INSTALL_1_ID)
        .userId(JOE_DOE_ID)
        .ghId(APP_INSTALLATION_1_ID)
        .ownerGithubLogin("joedoelogin1")
        .type("User")
        .avatarUrl("http://testimage.com")
        .repositorySelection(ALL)
        .build();
  }

  public static List<EnvVar> envVarsWithTestValues() {
    return List.of(
        new EnvVar().name("ENV1").value("dummy").testValue("dummy"),
        new EnvVar().name("ENV2").value("dummy").testValue("dummy"));
  }

  public static UserCost joe_2025_dec() {
    return new UserCost()
        .id("uc_1")
        .userId(JOE_DOE_ID)
        .amount(BigDecimal.valueOf(2.25))
        .startDate(LocalDate.of(2025, 12, 1))
        .endDate(YearMonth.of(2025, Month.DECEMBER).atEndOfMonth())
        .updatedAt(null);
  }

  public static UserCost noobie_2025_dec() {
    return new UserCost()
        .id("uc_2")
        .userId(NOOBIE_ID)
        .amount(BigDecimal.valueOf(0.75))
        .startDate(LocalDate.of(2025, 12, 1))
        .endDate(YearMonth.of(2025, Month.DECEMBER).atEndOfMonth())
        .updatedAt(null);
  }

  public static UserCost noobie_2025_nov() {
    return new UserCost()
        .id("uc_3")
        .userId(NOOBIE_ID)
        .amount(new BigDecimal("12.00"))
        .startDate(LocalDate.of(2025, 11, 1))
        .endDate(YearMonth.of(2025, Month.NOVEMBER).atEndOfMonth())
        .updatedAt(null);
  }

  public static UserCost noobie_2024_dec() {
    return new UserCost()
        .id("uc_4")
        .userId(NOOBIE_ID)
        .amount(new BigDecimal("9.00"))
        .startDate(LocalDate.of(2024, 12, 1))
        .endDate(YearMonth.of(2024, Month.DECEMBER).atEndOfMonth())
        .updatedAt(null);
  }

  public static final String HELLO_WORLD_TEMPLATE_ID = "at_1";
  public static final String THYMELEAF_TEMPLATE_ID = "at_2";

  public static ApplicationTemplate helloWorldTemplate() {
    return new ApplicationTemplate()
        .id(HELLO_WORLD_TEMPLATE_ID)
        .name("hello-world")
        .description("deploy hello-world template")
        .repositoryUrl(URI.create("https://github.com/poja/hello-world-template"))
        .demoUrl(URI.create("https://hello-world-template-demo.on.aws"))
        .withCustomConfig(true);
  }

  public static ApplicationTemplate thymeleafTemplate() {
    return new ApplicationTemplate()
        .id(THYMELEAF_TEMPLATE_ID)
        .name("thymeleaf")
        .description("deploy thymeleaf template")
        .repositoryUrl(URI.create("https://github.com/poja/thymeleaf-template"))
        .demoUrl(null)
        .withCustomConfig(false);
  }

  public static List<ApplicationTemplate> applicationTemplates() {
    return List.of(helloWorldTemplate(), thymeleafTemplate());
  }
}
