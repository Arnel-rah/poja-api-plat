package api.poja.io.endpoint.event.model;

import static api.poja.io.endpoint.event.EventStack.EVENT_STACK_2;
import static api.poja.io.endpoint.event.utils.TestMocks.MOCK_BUCKET_KEY;
import static api.poja.io.endpoint.event.utils.TestMocks.MOCK_INSTANT;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.endpoint.rest.model.FunctionType.FRONTAL;
import static api.poja.io.integration.conf.utils.TestMocks.ENV_DEPLOYMENT_CONF_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.GH_APP_INSTALL_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_EMAIL;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.PENDING;
import static api.poja.io.repository.model.enums.PaymentRequestPeriod.JANUARY;
import static api.poja.io.service.pricing.PricingMethod.TEN_MICRO;
import static api.poja.io.service.pricing.PricingMethod.TWENTY_MICRO;
import static java.time.Duration.ZERO;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import api.poja.io.endpoint.EndpointConf;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
import api.poja.io.endpoint.rest.model.SortDirection;
import api.poja.io.model.EnvVar;
import api.poja.io.model.User;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.Stack;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

public class EventModelSerializationTest {
  ObjectMapper om = new EndpointConf().objectMapper();

  private static @NotNull RefreshUsersBillingInfoTriggered refreshUsersBillingInfoTriggered() {
    return new RefreshUsersBillingInfoTriggered();
  }

  private static @NotNull RefreshUserBillingInfoRequested refreshUserBillingInfoRequested() {
    return new RefreshUserBillingInfoRequested(
        JOE_DOE_ID, refreshUsersBillingInfoTriggered(), TEN_MICRO);
  }

  private static @NotNull RefreshOrgBillingInfoRequested refreshOrgBillingInfoRequested() {
    return new RefreshOrgBillingInfoRequested(
        JOE_DOE_ID, JOE_DOE_MAIN_ORG_ID, refreshUserBillingInfoRequested());
  }

  private static @NotNull RefreshAppBillingInfoRequested refreshAppBillingInfoRequested() {
    return new RefreshAppBillingInfoRequested(
        JOE_DOE_ID, POJA_APPLICATION_ID, refreshOrgBillingInfoRequested());
  }

  private static @NotNull RefreshEnvBillingInfoRequested refreshEnvBillingInfoRequestedEvent() {
    return new RefreshEnvBillingInfoRequested(
        POJA_APPLICATION_ENVIRONMENT_ID,
        JOE_DOE_ID,
        POJA_APPLICATION_ENVIRONMENT_ID,
        refreshAppBillingInfoRequested());
  }

  private static @NotNull OrgInvitationEmailNotificationRequested
      orgInvitationEmailNotificationRequested() {
    return new OrgInvitationEmailNotificationRequested(
        JOE_DOE_EMAIL, "POJA invitation organization", JANE_DOE_MAIN_ORG_ID, JOE_DOE_ID);
  }

  private static BuiltEnvInfo builtEnvInfo() {
    return new BuiltEnvInfo()
        .id("build_env_info_id")
        .formattedBucketKey(MOCK_BUCKET_KEY)
        .environmentType(PROD);
  }

  @Test
  void app_env_compute_deploy_requested_serialization() throws JsonProcessingException {
    var appEnvComputeRequested =
        new AppEnvComputeDeployRequested(
            JOE_DOE_ID,
            POJA_APPLICATION_ID,
            POJA_APPLICATION_ENVIRONMENT_ID,
            MOCK_BUCKET_KEY,
            "mock_app_name",
            "preprod-compute-mock-app-name-uuid",
            List.of(
                "preprod-storage-bucket-mock-app-name-uuid", "preprod-event-mock-app-name-uuid"),
            PROD,
            MOCK_INSTANT,
            null);

    var serialized = om.writeValueAsString(appEnvComputeRequested);
    var deserialized = om.readValue(serialized, AppEnvComputeDeployRequested.class);

    assertEquals(appEnvComputeRequested, deserialized);
    assertEquals(MOCK_BUCKET_KEY, deserialized.getFormattedBucketKey());
    assertEquals(ZERO, deserialized.maxConsumerDuration());
    assertEquals(ZERO, deserialized.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void check_template_integrity_triggered_serialization() throws JsonProcessingException {
    var checkTemplateIntegrityTriggered =
        new CheckTemplateIntegrityTriggered(
            JOE_DOE_ID,
            POJA_APPLICATION_ID,
            POJA_APPLICATION_ENVIRONMENT_ID,
            MOCK_BUCKET_KEY,
            MOCK_BUCKET_KEY,
            builtEnvInfo(),
            "deployment_conf_id",
            "deployment_1_id");

    var serialized = om.writeValueAsString(checkTemplateIntegrityTriggered);
    var deserialized = om.readValue(serialized, CheckTemplateIntegrityTriggered.class);

    assertEquals(checkTemplateIntegrityTriggered, deserialized);
    assertEquals(MOCK_BUCKET_KEY, deserialized.getBuiltProjectBucketKey());
    assertEquals(builtEnvInfo(), deserialized.getBuiltEnvInfo());
    assertEquals(ZERO, deserialized.maxConsumerDuration());
    assertEquals(ZERO, deserialized.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void refresh_users_billing_info_triggered_serialization() throws JsonProcessingException {
    var event = refreshUsersBillingInfoTriggered();

    var serialized = om.writeValueAsString(event);
    var deserialized = om.readValue(serialized, RefreshUsersBillingInfoTriggered.class);

    assertEquals(event, deserialized);
    assertNotNull(event.getId());
    assertNotNull(event.getPricingCalculationRequestEndTime());
    assertNotNull(event.getUtcLocalDate());
    assertNotNull(event.getPricingCalculationRequestStartTime());
    assertEquals(Duration.ofSeconds(40), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void refresh_user_billing_info_requested_serialization() throws JsonProcessingException {
    var event = refreshUserBillingInfoRequested();

    var serialized = om.writeValueAsString(event);
    var deserialized = om.readValue(serialized, RefreshUserBillingInfoRequested.class);

    assertEquals(event, deserialized);
    assertNotNull(event.getId());
    assertNotNull(event.getPricingCalculationRequestStartTime());
    assertNotNull(event.getPricingCalculationRequestEndTime());
    assertNotNull(event.getPricingMethod());
    assertNotNull(event.getRefreshUsersBillingInfoTriggered());
    assertNotNull(event.getUserId());
    assertEquals(Duration.ofSeconds(45), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void refresh_org_billing_info_requested_serialization() throws JsonProcessingException {
    var event = refreshOrgBillingInfoRequested();

    var serialized = om.writeValueAsString(event);
    var deserialized = om.readValue(serialized, RefreshOrgBillingInfoRequested.class);

    assertEquals(event, deserialized);
    assertNotNull(event.getPricingCalculationRequestStartTime());
    assertNotNull(event.getPricingCalculationRequestEndTime());
    assertNotNull(event.getPricingMethod());
    assertEquals(Duration.ofSeconds(45), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void refresh_app_billing_info_requested_serialization() throws JsonProcessingException {
    var event = refreshAppBillingInfoRequested();

    var serialized = om.writeValueAsString(event);
    var deserialized = om.readValue(serialized, RefreshAppBillingInfoRequested.class);

    assertEquals(event, deserialized);
    assertNotNull(event.getPricingCalculationRequestStartTime());
    assertNotNull(event.getPricingCalculationRequestEndTime());
    assertNotNull(event.getPricingMethod());
    assertEquals(Duration.ofSeconds(15), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void refresh_env_billing_info_requested_serialization() throws JsonProcessingException {
    var event = refreshEnvBillingInfoRequestedEvent();

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, RefreshEnvBillingInfoRequested.class);

    assertEquals(event, dese);
    assertNotNull(event.getPricingCalculationRequestStartTime());
    assertNotNull(event.getPricingCalculationRequestEndTime());
    assertNotNull(event.getPricingMethod());
    assertEquals(EVENT_STACK_2, event.getEventStack());
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void get_billing_info_query_result_requested_serialization() throws JsonProcessingException {
    var event =
        new UserEnvBillingInfoUpdateFromQueryRequested(
            "queryId", "userId", "envId", refreshEnvBillingInfoRequestedEvent());

    String s = om.writeValueAsString(event);
    UserEnvBillingInfoUpdateFromQueryRequested dese =
        om.readValue(s, UserEnvBillingInfoUpdateFromQueryRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void app_env_depl_request_queued_serialization() throws JsonProcessingException {
    var event = new AppEnvDeployRequestQueued(AppEnvDeployRequested.builder().build());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, AppEnvDeployRequestQueued.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void env_archival_requested_serialization() throws JsonProcessingException {
    var event = new EnvArchivalRequested("appId", "envId", now(), false);

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, EnvArchivalRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void build_tool_verification_requested_serialization() throws JsonProcessingException {
    var event = new BuildToolConversionRequested("org_id", "import_id");

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, BuildToolConversionRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(40), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void app_archival_requested_serialization() throws JsonProcessingException {
    var event = new AppArchivalRequested("orgId", "appId", now());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, AppArchivalRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void org_archival_requested_serialization() throws JsonProcessingException {
    var event = new OrgArchivalRequested("orgId", Instant.now());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, OrgArchivalRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofMinutes(2), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(60), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void user_archival_requested_serialization() throws JsonProcessingException {
    var event = new UserArchivalRequested("userId", Instant.now());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserArchivalRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofMinutes(2), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(60), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void stack_deletion_requested_serialization() throws JsonProcessingException {
    var event = new StackDeletionRequested(new Stack());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, StackDeletionRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void user_monthly_payment_request_save_requested_serialization() throws JsonProcessingException {
    var event =
        new UserMonthlyPaymentRequestSaveRequested(
            "joe_id", "payment_request_1_id", "customer_id", JANUARY, Year.of(2015), TWENTY_MICRO);

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserMonthlyPaymentRequestSaveRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(70), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(60), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void user_monthly_payment_requested_serialization() throws JsonProcessingException {
    var event =
        new UserMonthlyPaymentRequested(
            "joe-doe-id", "customer_id", "payment_request_id", YearMonth.of(2015, Month.JANUARY));

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserMonthlyPaymentRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void stack_deletion_request_verification_requested() throws JsonProcessingException {
    Stack toDelete = new Stack();
    var event =
        new StackDeletionRequestVerificationRequested(
            toDelete, new StackDeletionRequested(toDelete));

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, StackDeletionRequestVerificationRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void user_subscription_invoice_payment_requested() throws JsonProcessingException {
    var event =
        new UserSubscriptionInvoicePaymentRequested(
            "userSubscriptionId", "invoiceId", "userId", now(), false);

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserSubscriptionInvoicePaymentRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void env_status_update_requested_event() throws JsonProcessingException {
    var event =
        new EnvStatusUpdateRequested(
            "appId", "envId", EnvStatusUpdateRequested.StatusAlteration.ACTIVATE);

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, EnvStatusUpdateRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void stack_deletion_request_verification_requested_event() throws JsonProcessingException {
    var event =
        new StackDeletionRequestVerificationRequested(new Stack(), new StackDeletionRequested());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, StackDeletionRequestVerificationRequested.class);

    assertNotNull(dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void endOfDay_refresh_users_billing_info_serialization() throws JsonProcessingException {
    var event = new EndOfDayRefreshUsersBillingInfoTriggered(LocalDate.of(2004, 8, 31));
    var eventAsString2 = "{\"local_date\":\"2024-08-31\"}";

    var serialized = om.writeValueAsString(event);
    var deserialized = om.readValue(serialized, EndOfDayRefreshUsersBillingInfoTriggered.class);
    var deserialized2 =
        om.readValue(eventAsString2, EndOfDayRefreshUsersBillingInfoTriggered.class);

    assertEquals(event, deserialized);
    assertEquals(Duration.ofSeconds(40), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
    assertEquals(
        LocalDate.of(2024, 8, 31),
        deserialized2.asRefreshUsersBillingInfoTriggered().getUtcLocalDate());
  }

  @Test
  void read_eod_from_sqs_message_ok() throws JsonProcessingException, ClassNotFoundException {
    String emptyEOD =
        """
  {
    "version": "0",
    "id": "33048af5-dfe2-5ba8-5420-593263aee446",
    "detail-type": "api.poja.io.endpoint.event.model.EndOfDayRefreshUsersBillingInfoTriggered",
    "source": "api.poja.io.event1",
    "account": "10",
    "time": "2025-04-18T00:00:00Z",
    "region": "eu-west-3",
    "resources": [],
    "detail": {}
  }
""";
    String withLocalDateEod =
        """
  {
    "version": "0",
    "id": "33048af5-dfe2-5ba8-5420-593263aee446",
    "detail-type": "api.poja.io.endpoint.event.model.EndOfDayRefreshUsersBillingInfoTriggered",
    "source": "api.poja.io.event1",
    "account": "10",
    "time": "2025-04-18T00:00:00Z",
    "region": "eu-west-3",
    "resources": [],
    "detail": {"local_date":"2004-08-31"}
  }
""";
    final String DETAIL_PROPERTY = "detail";
    String typeName = "api.poja.io.endpoint.event.model.EndOfDayRefreshUsersBillingInfoTriggered";
    Class<?> toValueType = Class.forName(typeName);

    var withDateEod =
        (EndOfDayRefreshUsersBillingInfoTriggered)
            om.convertValue(
                createSqsMessageBody(withLocalDateEod).get(DETAIL_PROPERTY), toValueType);
    var emptyEod =
        (EndOfDayRefreshUsersBillingInfoTriggered)
            om.convertValue(createSqsMessageBody(emptyEOD).get(DETAIL_PROPERTY), toValueType);

    assertEquals(
        new EndOfDayRefreshUsersBillingInfoTriggered().getLocalDate(), emptyEod.getLocalDate());
    assertEquals(
        new EndOfDayRefreshUsersBillingInfoTriggered(LocalDate.of(2004, 8, 31)).getLocalDate(),
        withDateEod.getLocalDate());
  }

  private Map<String, Object> createSqsMessageBody(String messageBody)
      throws JsonProcessingException {
    var message = new SQSEvent.SQSMessage();
    message.setBody(messageBody);
    TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
    Map<String, Object> body = om.readValue(message.getBody(), typeRef);
    return body;
  }

  @Test
  void log_query_created_serialization() throws JsonProcessingException {
    var event =
        new LogQueryCreated(
            "domainQueryId",
            Set.of("lol"),
            SortDirection.ASC,
            Set.of(PROD),
            Instant.now(),
            Instant.now(),
            Set.of(FRONTAL));

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, LogQueryCreated.class);

    assertEquals(event, dese);
    assertEquals(EVENT_STACK_2, event.getEventStack());
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(90), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void user_payment_setup_serialization() throws JsonProcessingException {
    var event = new UserPaymentSetupRequested(new User());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserPaymentSetupRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void user_main_org_setup_serialization() throws JsonProcessingException {
    var event = new UserMainOrganizationSetupRequested(new User());

    String s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserMainOrganizationSetupRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void users_subscription_renewal() throws JsonProcessingException {
    var event = new UsersSubscriptionRenewalRequested();
    var event2 = new UsersSubscriptionRenewalRequested(YearMonth.of(2024, 4));

    String s = om.writeValueAsString(event);
    String s2 = om.writeValueAsString(event2);
    var dese = om.readValue(s, UsersSubscriptionRenewalRequested.class);
    var dese2 = om.readValue(s2, UsersSubscriptionRenewalRequested.class);

    assertEquals(event, dese);
    assertEquals(event2, dese2);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void organization_invitation_email_notification() throws JsonProcessingException {
    var event = orgInvitationEmailNotificationRequested();

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, OrgInvitationEmailNotificationRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void environment_deployment_requested() throws JsonProcessingException {
    var event =
        new EnvironmentDeploymentRequested(
            JOE_DOE_ID,
            POJA_APPLICATION_ID,
            POJA_APPLICATION_ENVIRONMENT_ID,
            ENV_DEPLOYMENT_CONF_1_ID);

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, EnvironmentDeploymentRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void console_user_credentials_update_requested() throws JsonProcessingException {
    var event = new ConsoleUserCredentialsUpdateRequested("dummy_org_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, ConsoleUserCredentialsUpdateRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(20), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void console_user_credentials_update_email_notification_requested()
      throws JsonProcessingException {
    var event =
        new ConsoleUserCredentialsUpdateEmailNotificationRequested("dummy_org_id", "mock_user_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, ConsoleUserCredentialsUpdateEmailNotificationRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void application_creation_requested() throws JsonProcessingException {
    var event =
        new ApplicationCreated(
            JOE_DOE_MAIN_ORG_ID,
            POJA_APPLICATION_ID,
            "poja_application",
            false,
            GH_APP_INSTALL_1_ID,
            "a regular poja app",
            null);

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, ApplicationCreated.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void app_import_upload_requested() throws JsonProcessingException {
    var event =
        AppImportUploadRequested.builder()
            .appImport(
                ApplicationImport.builder()
                    .id("dummy")
                    .appName("dummy")
                    .appInstallationId("dummy")
                    .orgId("dummy")
                    .creationDatetime(now())
                    .githubRepositoryName("dummy")
                    .githubRepositoryHttpUrl("dummy")
                    .status(PENDING)
                    .pojaVersion(POJA_7.toHumanReadableValue())
                    .build())
            .envVars(List.of(new EnvVar("dummy", "dummy", "dummy")))
            .build();

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, AppImportUploadRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void app_lang_analysis_requested() throws JsonProcessingException {
    var event = AppLanguageAnalysisRequested.builder().importId("dummy").orgId("dummy").build();

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, AppLanguageAnalysisRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(20), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void build_tool_analysis_requested() throws JsonProcessingException {
    var event = BuildToolAnalysisRequested.builder().importId("dummy").orgId("dummy").build();

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, BuildToolAnalysisRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(20), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void import_processed() throws JsonProcessingException {
    var event = new AppImportProcessed("org_id", "import_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, AppImportProcessed.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(20), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void pre_transformation_test_run_requested() throws JsonProcessingException {
    var event = new PreTransformationTestRunRequested("import_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, PreTransformationTestRunRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void poja_gen_from_gradle_requested() throws JsonProcessingException {
    var event = new PojaConfGenFromGradleRequested("import_id", "org_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, PojaConfGenFromGradleRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(40), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void app_import_poja_conf_uploaded() throws JsonProcessingException {
    var event = new AppImportPojaConfUploaded("org_id", "import_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, AppImportPojaConfUploaded.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(40), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void code_formatting_requested() throws JsonProcessingException {
    var event = new CodeFormattingRequested("org_id", "import_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, CodeFormattingRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void post_transformation_ping_test_requested() throws JsonProcessingException {
    var event = new PostTransformationPingTestRequested("import_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, PostTransformationPingTestRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @SneakyThrows
  @Test
  void refresh_user_cost_requested() {
    var event = new RefreshUserCostRequested("user_id", LocalDate.now());

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, RefreshUserCostRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @SneakyThrows
  @Test
  void application_clone_requested() {
    var event = new ApplicationCloneRequested("org_id", "app_id", "template_id");

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, ApplicationCloneRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }

  @SneakyThrows
  @Test
  void refresh_users_cost_triggered() {
    var event = new RefreshUsersCostTriggered(LocalDate.now());
    var event2 = new RefreshUsersCostTriggered(null);

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, RefreshUsersCostTriggered.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(30), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
    assertNotNull(event2.getDate());
    assertEquals(event.maxConsumerDuration(), event2.maxConsumerDuration());
    assertEquals(
        event.maxConsumerBackoffBetweenRetries(), event2.maxConsumerBackoffBetweenRetries());
  }

  @SneakyThrows
  @Test
  void user_subscription_requested() {
    var event = new UserSubscriptionRequested("user_1_id", now().truncatedTo(MILLIS));

    var s = om.writeValueAsString(event);
    var dese = om.readValue(s, UserSubscriptionRequested.class);

    assertEquals(event, dese);
    assertEquals(Duration.ofSeconds(10), event.maxConsumerDuration());
    assertEquals(Duration.ofSeconds(30), event.maxConsumerBackoffBetweenRetries());
  }
}
