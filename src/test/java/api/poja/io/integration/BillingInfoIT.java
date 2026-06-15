package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.MonthType.JANUARY;
import static api.poja.io.endpoint.rest.model.MonthType.SEPTEMBER;
import static api.poja.io.endpoint.rest.model.SortBy.COST;
import static api.poja.io.endpoint.rest.model.SortBy.COST_MARGIN;
import static api.poja.io.endpoint.rest.model.SortBy.JOIN_DATE;
import static api.poja.io.endpoint.rest.model.SortBy.LAST_CONNECTION;
import static api.poja.io.endpoint.rest.model.SortBy.SUSPENSION_DURATION;
import static api.poja.io.endpoint.rest.model.SortOrder.ASC;
import static api.poja.io.endpoint.rest.model.SortOrder.DESC;
import static api.poja.io.integration.conf.utils.TestMocks.ADMIN_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.BILLING_INFO_END_TIME_QUERY;
import static api.poja.io.integration.conf.utils.TestMocks.BILLING_INFO_END_TIME_QUERY1;
import static api.poja.io.integration.conf.utils.TestMocks.BILLING_INFO_START_TIME_QUERY;
import static api.poja.io.integration.conf.utils.TestMocks.BILLING_INFO_START_TIME_QUERY1;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOINED1_TO;
import static api.poja.io.integration.conf.utils.TestMocks.JOINED_FROM;
import static api.poja.io.integration.conf.utils.TestMocks.JOINED_TO;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ENVIRONMENT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.OTHER_POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.SUSPENDED_ID;
import static api.poja.io.integration.conf.utils.TestMocks.SUSPENDED_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeBillingInfo1;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeBillingInfo2;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeMainOrgBillingInfo;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeTotalBillingInfo;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeTotalBillingInfoWithAws;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.math.BigDecimal.ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.BillingApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.PagedUsersBillingInfoResponse;
import api.poja.io.endpoint.rest.model.UserBillingInfoWithAws;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.model.User;
import api.poja.io.repository.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class BillingInfoIT extends MockedThirdParties {
  @SpyBean private UserRepository userRepositorySpy;

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    // prevent from being updated on whoami()
    doNothing().when(userRepositorySpy).updateLastConnection(anyString(), any(Instant.class));
  }

  @Test
  void get_org_billing_info_by_env_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(joeDoeClient);

    var actual =
        api.getOrgAppEnvironmentBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null);
    var actual2 =
        api.getOrgAppEnvironmentBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER);
    var actual3 =
        api.getOrgAppEnvironmentBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            OTHER_POJA_APPLICATION_ENVIRONMENT_ID,
            null,
            null,
            2024,
            SEPTEMBER);

    assertEquals(joeDoeBillingInfo1(), actual);
    assertEquals(joeDoeBillingInfo1(), actual2);
    assertEquals(joeDoeBillingInfo1(), actual3);
  }

  @Test
  void get_org_billing_info_by_app_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(joeDoeClient);

    var actual =
        api.getOrgApplicationBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null);
    var actual2 =
        api.getOrgApplicationBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            OTHER_POJA_APPLICATION_ID,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER);
    var actual3 =
        api.getOrgApplicationBillingInfo(
            JOE_DOE_MAIN_ORG_ID, OTHER_POJA_APPLICATION_ID, null, null, 2024, SEPTEMBER);

    assertTrue(actual.containsAll(List.of(joeDoeBillingInfo1(), joeDoeBillingInfo2())));
    assertTrue(actual.containsAll(List.of(joeDoeBillingInfo1(), joeDoeBillingInfo2())));
    assertTrue(actual2.containsAll(List.of(joeDoeBillingInfo1(), joeDoeBillingInfo2())));
    assertTrue(actual2.containsAll(List.of(joeDoeBillingInfo1(), joeDoeBillingInfo2())));
    assertTrue(actual3.containsAll(List.of(joeDoeBillingInfo1(), joeDoeBillingInfo2())));
    assertTrue(actual3.containsAll(List.of(joeDoeBillingInfo1(), joeDoeBillingInfo2())));
  }

  @Test
  void get_user_total_billing_info() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(joeDoeClient);

    var actual =
        api.getUserBillingInfo(
            JOE_DOE_ID, BILLING_INFO_START_TIME_QUERY, BILLING_INFO_END_TIME_QUERY, null, null);
    var actual2 =
        api.getUserBillingInfo(
            JOE_DOE_ID,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER);
    var actual3 = api.getUserBillingInfo(JOE_DOE_ID, null, null, 2024, SEPTEMBER);

    assertEquals(joeDoeTotalBillingInfo(), actual);
    assertEquals(joeDoeTotalBillingInfo(), actual2);
    assertEquals(joeDoeTotalBillingInfo(), actual3);
  }

  @Test
  void get_user_empty_billing_info() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(joeDoeClient);

    var actual =
        api.getUserBillingInfo(
            JOE_DOE_ID,
            Instant.parse("2024-01-01T00:00:00.00Z"),
            Instant.parse("2024-01-02T00:00:00.00Z"),
            null,
            null);
    var actual2 =
        api.getUserBillingInfo(
            JOE_DOE_ID,
            Instant.parse("2024-01-01T00:00:00.00Z"),
            Instant.parse("2024-01-02T00:00:00.00Z"),
            2024,
            JANUARY);
    var actual3 = api.getUserBillingInfo(JOE_DOE_ID, null, null, 2024, JANUARY);

    assertEquals(ZERO, actual.getComputedPrice());
    assertEquals(ZERO, actual2.getComputedPrice());
    assertEquals(ZERO, actual3.getComputedPrice());
  }

  @Test
  void get_org_empty_billing_info() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(joeDoeClient);

    var actual =
        api.getOrgBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            Instant.parse("2024-01-01T00:00:00.00Z"),
            Instant.parse("2024-01-02T00:00:00.00Z"),
            null,
            null);
    var actual2 =
        api.getOrgBillingInfo(
            JOE_DOE_MAIN_ORG_ID,
            Instant.parse("2024-01-01T00:00:00.00Z"),
            Instant.parse("2024-01-02T00:00:00.00Z"),
            2024,
            JANUARY);
    var actual3 = api.getOrgBillingInfo(JOE_DOE_MAIN_ORG_ID, null, null, 2024, JANUARY);

    assertEquals(ZERO, actual.getComputedPrice());
    assertEquals(ZERO, actual2.getComputedPrice());
    assertEquals(ZERO, actual3.getComputedPrice());
  }

  static PagedUsersBillingInfoResponse withoutAwsCostUpdatedAt(
      PagedUsersBillingInfoResponse response) {
    return response.data(response.getData().stream().map(e -> e.awsCostUpdatedAt(null)).toList());
  }

  @Test
  void get_users_billing() throws ApiException {
    ApiClient adminClient = anApiClient(ADMIN_TOKEN);
    BillingApi api = new BillingApi(adminClient);

    var paged =
        api.getUsersBillingInfo(
            1,
            1,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null,
            null,
            null,
            DESC,
            null,
            null,
            null);
    var actual =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                BILLING_INFO_START_TIME_QUERY,
                BILLING_INFO_END_TIME_QUERY,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    var actual2 =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                BILLING_INFO_START_TIME_QUERY,
                BILLING_INFO_END_TIME_QUERY,
                2024,
                SEPTEMBER,
                null,
                null,
                null,
                null,
                null,
                null));
    var actual3 =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                BILLING_INFO_START_TIME_QUERY,
                BILLING_INFO_END_TIME_QUERY,
                2024,
                SEPTEMBER,
                null,
                null,
                null,
                null,
                null,
                null));
    var sortedBySuspensionDurationDesc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            DESC,
            SUSPENSION_DURATION,
            null,
            null);
    var sortedBySuspensionDurationAsc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            ASC,
            SUSPENSION_DURATION,
            null,
            null);
    var usersBillingFilteredByJoinedAt =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                BILLING_INFO_START_TIME_QUERY,
                BILLING_INFO_END_TIME_QUERY,
                2024,
                null,
                null,
                "JoeDoe",
                null,
                null,
                JOINED_FROM,
                JOINED_TO));
    var billingUpToJoinedTo =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                null,
                BILLING_INFO_END_TIME_QUERY,
                null,
                null,
                null,
                null,
                null,
                null,
                JOINED_FROM,
                JOINED_TO));

    var billingFromJoinedFrom =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                BILLING_INFO_START_TIME_QUERY,
                BILLING_INFO_END_TIME_QUERY,
                2024,
                null,
                null,
                null,
                null,
                SUSPENSION_DURATION,
                JOINED_FROM,
                null));
    var billingWithoutJoeDoe =
        withoutAwsCostUpdatedAt(
            api.getUsersBillingInfo(
                1,
                500,
                BILLING_INFO_START_TIME_QUERY1,
                BILLING_INFO_END_TIME_QUERY1,
                2024,
                null,
                null,
                null,
                null,
                SUSPENSION_DURATION,
                JOINED_FROM,
                JOINED1_TO));
    var sortedByJoinDateDesc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            DESC,
            JOIN_DATE,
            null,
            null);
    var sortedByJoinDateAsc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            ASC,
            JOIN_DATE,
            null,
            null);
    var sortedByLastConnectionDesc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            DESC,
            LAST_CONNECTION,
            null,
            null);
    var sortedByLastConnectionAsc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            ASC,
            LAST_CONNECTION,
            null,
            null);
    var sortedByUserCostDesc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            DESC,
            COST,
            null,
            null);
    var sortedByUserCostAsc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null,
            null,
            null,
            ASC,
            COST,
            null,
            null);
    var sortedByUserCostMarginDesc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER,
            null,
            null,
            DESC,
            COST_MARGIN,
            null,
            null);
    var sortedByUserCostMarginAsc =
        api.getUsersBillingInfo(
            1,
            500,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null,
            null,
            null,
            ASC,
            COST_MARGIN,
            null,
            null);

    var actualSortedByComputedBilling =
        actual.getData().stream()
            .sorted(Comparator.comparing(UserBillingInfoWithAws::getComputedPrice).reversed())
            .toList();

    assertEquals(1, (int) paged.getCount());
    assertTrue(
        actual.getData().stream()
            .map(UserBillingInfoWithAws::getUserId)
            .toList()
            .containsAll(userRepositorySpy.findAll().stream().map(User::getId).toList()));

    assertTrue(Objects.requireNonNull(actual.getData()).contains(joeDoeTotalBillingInfoWithAws()));
    assertTrue(Objects.requireNonNull(actual2.getData()).contains(joeDoeTotalBillingInfoWithAws()));
    assertTrue(Objects.requireNonNull(actual3.getData()).contains(joeDoeTotalBillingInfoWithAws()));
    assertTrue(
        Objects.requireNonNull(usersBillingFilteredByJoinedAt.getData())
            .contains(joeDoeTotalBillingInfoWithAws()));
    assertEquals(actualSortedByComputedBilling, actual.getData());

    assertEquals("suspended_id", sortedBySuspensionDurationDesc.getData().getFirst().getUserId());
    assertEquals("suspended_2_id", sortedBySuspensionDurationDesc.getData().get(1).getUserId());
    assertEquals("suspended_3_id", sortedBySuspensionDurationDesc.getData().get(2).getUserId());

    assertEquals("suspended_3_id", sortedBySuspensionDurationAsc.getData().getFirst().getUserId());
    assertEquals("suspended_2_id", sortedBySuspensionDurationAsc.getData().get(1).getUserId());
    assertEquals("suspended_id", sortedBySuspensionDurationAsc.getData().get(2).getUserId());

    assertEquals("recsus_id", sortedByJoinDateAsc.getData().get(0).getUserId());
    assertEquals("admin_id", sortedByJoinDateAsc.getData().get(1).getUserId());
    assertEquals("noobie_id", sortedByJoinDateAsc.getData().get(2).getUserId());

    assertEquals("denis_ritchie_id", sortedByJoinDateDesc.getData().get(0).getUserId());
    assertEquals("lorem_ipsum_id", sortedByJoinDateDesc.getData().get(1).getUserId());
    assertEquals("to_suspend_id", sortedByJoinDateDesc.getData().get(2).getUserId());

    assertEquals("recsus_id", sortedByLastConnectionDesc.getData().get(0).getUserId());
    assertEquals("admin_id", sortedByLastConnectionDesc.getData().get(1).getUserId());
    assertEquals("denis_ritchie_id", sortedByLastConnectionDesc.getData().get(2).getUserId());

    assertEquals("lorem_ipsum_id", sortedByLastConnectionAsc.getData().get(0).getUserId());
    assertEquals("noobie_id", sortedByLastConnectionAsc.getData().get(1).getUserId());
    assertEquals("to_suspend_id", sortedByLastConnectionAsc.getData().get(2).getUserId());

    assertEquals("recsus_id", sortedByUserCostDesc.getData().get(0).getUserId());
    assertEquals("admin_id", sortedByUserCostDesc.getData().get(1).getUserId());
    assertEquals("denis_ritchie_id", sortedByUserCostDesc.getData().get(2).getUserId());
    assertEquals("lorem_ipsum_id", sortedByUserCostDesc.getData().get(3).getUserId());
    assertEquals("jane_doe_id", sortedByUserCostDesc.getData().get(4).getUserId());
    assertEquals("joe-doe-id", sortedByUserCostDesc.getData().get(5).getUserId());

    assertEquals("joe-doe-id", sortedByUserCostAsc.getData().get(0).getUserId());
    assertEquals("jane_doe_id", sortedByUserCostAsc.getData().get(1).getUserId());
    assertEquals("lorem_ipsum_id", sortedByUserCostAsc.getData().get(2).getUserId());
    assertEquals("denis_ritchie_id", sortedByUserCostAsc.getData().get(3).getUserId());
    assertEquals("admin_id", sortedByUserCostAsc.getData().get(4).getUserId());
    assertEquals("recsus_id", sortedByUserCostAsc.getData().get(5).getUserId());

    assertEquals("joe-doe-id", sortedByUserCostMarginDesc.getData().get(0).getUserId());
    assertEquals(
        new BigDecimal("3041.58217648441926"),
        sortedByUserCostMarginDesc.getData().get(0).getCostMargin());
    assertEquals("recsus_id", sortedByUserCostMarginDesc.getData().get(1).getUserId());
    assertEquals(
        new BigDecimal("114"), sortedByUserCostMarginDesc.getData().get(1).getCostMargin());
    assertEquals("jane_doe_id", sortedByUserCostMarginDesc.getData().get(2).getUserId());
    assertEquals(new BigDecimal("88"), sortedByUserCostMarginDesc.getData().get(2).getCostMargin());

    assertEquals("admin_id", sortedByUserCostMarginAsc.getData().get(0).getUserId());
    assertEquals(new BigDecimal("-5"), sortedByUserCostMarginAsc.getData().get(0).getCostMargin());
    assertEquals("denis_ritchie_id", sortedByUserCostMarginAsc.getData().get(1).getUserId());
    assertEquals(new BigDecimal("-4"), sortedByUserCostMarginAsc.getData().get(1).getCostMargin());
    assertEquals("lorem_ipsum_id", sortedByUserCostMarginAsc.getData().get(2).getUserId());
    assertEquals(new BigDecimal("-3"), sortedByUserCostMarginAsc.getData().get(2).getCostMargin());

    assertEquals("joe-doe-id", usersBillingFilteredByJoinedAt.getData().getFirst().getUserId());
    assertFalse(Objects.requireNonNull(billingUpToJoinedTo.getData()).isEmpty());
    assertFalse(Objects.requireNonNull(billingFromJoinedFrom.getData()).isEmpty());
    assertNotEquals(billingUpToJoinedTo.getData(), billingFromJoinedFrom.getData());

    assertFalse(
        Objects.requireNonNull(billingWithoutJoeDoe.getData())
            .contains(joeDoeTotalBillingInfoWithAws()));
  }

  @Test
  void suspended_can_read_billing_info() {
    ApiClient suspendedClient = anApiClient(SUSPENDED_TOKEN);
    BillingApi api = new BillingApi(suspendedClient);

    assertDoesNotThrow(
        () -> {
          api.getUserBillingInfo(
              SUSPENDED_ID, BILLING_INFO_START_TIME_QUERY, BILLING_INFO_END_TIME_QUERY, null, null);
          api.getUserBillingInfo(
              SUSPENDED_ID,
              BILLING_INFO_START_TIME_QUERY,
              BILLING_INFO_END_TIME_QUERY,
              2024,
              SEPTEMBER);
          api.getUserBillingInfo(SUSPENDED_ID, null, null, 2024, SEPTEMBER);
        });
  }

  @Test
  void get_org_billing_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(joeDoeClient);
    var joeDoeMainOrgId = JOE_DOE_MAIN_ORG_ID;

    var actual =
        api.getOrgBillingInfo(
            joeDoeMainOrgId,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null);
    var actual2 =
        api.getOrgBillingInfo(
            joeDoeMainOrgId,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER);
    var actual3 = api.getOrgBillingInfo(joeDoeMainOrgId, null, null, 2024, SEPTEMBER);

    assertEquals(joeDoeMainOrgBillingInfo(), actual);
    assertEquals(joeDoeMainOrgBillingInfo(), actual2);
    assertEquals(joeDoeMainOrgBillingInfo(), actual3);
  }

  @Test
  void get_orgs_billing_ok() throws ApiException {
    ApiClient adminClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(adminClient);

    var actual =
        api.getUserOrgsBillingInfo(
            JOE_DOE_ID,
            1,
            10,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            null,
            null);
    var actual2 =
        api.getUserOrgsBillingInfo(
            JOE_DOE_ID,
            1,
            10,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER);
    var actual3 =
        api.getUserOrgsBillingInfo(
            JOE_DOE_ID,
            1,
            10,
            BILLING_INFO_START_TIME_QUERY,
            BILLING_INFO_END_TIME_QUERY,
            2024,
            SEPTEMBER);

    assertTrue(Objects.requireNonNull(actual.getData()).contains(joeDoeMainOrgBillingInfo()));
    assertTrue(Objects.requireNonNull(actual2.getData()).contains(joeDoeMainOrgBillingInfo()));
    assertTrue(Objects.requireNonNull(actual3.getData()).contains(joeDoeMainOrgBillingInfo()));
  }
}
