package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.MonthType.DECEMBER;
import static api.poja.io.endpoint.rest.model.MonthType.JANUARY;
import static api.poja.io.integration.conf.utils.TestMocks.ADMIN_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeBillingDiscount1;
import static api.poja.io.integration.conf.utils.TestMocks.joeDoeBillingDiscount2;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.BillingApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.UserBillingDiscount;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
public class BillingDiscountIT extends MockedThirdParties {
  public static UserBillingDiscount newJaneFebDiscount() {
    return new UserBillingDiscount()
        .id("ubd_3_id")
        .userId("jane_doe_id")
        .amount(BigDecimal.valueOf(1))
        .year(2025)
        .month(DECEMBER)
        .description("bonus")
        .creationDatetime(null);
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, port);
  }

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
  }

  @Test
  void get_user_billing_discounts_ok() throws ApiException {
    ApiClient adminClient = anApiClient(JOE_DOE_TOKEN);
    BillingApi api = new BillingApi(adminClient);

    var expectedJan2024 = List.of(joeDoeBillingDiscount1());
    var expectedDec2024 = List.of(joeDoeBillingDiscount2());

    var actualJan2024 = api.getUserBillingDiscounts(JOE_DOE_ID, 2024, JANUARY);
    var actualDec2025 = api.getUserBillingDiscounts(JOE_DOE_ID, 2025, DECEMBER);

    assertEquals(expectedJan2024, actualJan2024.getData());
    assertEquals(expectedDec2024, actualDec2025.getData());
  }

  @Test
  void grant_user_billing_discount_ok() throws ApiException {
    ApiClient adminClient = anApiClient(ADMIN_TOKEN);
    BillingApi api = new BillingApi(adminClient);

    api.grantUserDiscount(JANE_DOE_ID, newJaneFebDiscount());

    var grantedDiscounts =
        Objects.requireNonNull(api.getUserBillingDiscounts(JANE_DOE_ID, 2025, DECEMBER).getData())
            .stream()
            .map(e -> e.creationDatetime(null))
            .toList();

    assertEquals(List.of(newJaneFebDiscount()), grantedDiscounts);
  }
}
