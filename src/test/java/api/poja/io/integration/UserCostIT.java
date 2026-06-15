package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.MonthType.DECEMBER;
import static api.poja.io.endpoint.rest.model.MonthType.JANUARY;
import static api.poja.io.endpoint.rest.model.MonthType.NOVEMBER;
import static api.poja.io.integration.conf.utils.TestMocks.ADMIN_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JANE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.NOOBIE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.joe_2025_dec;
import static api.poja.io.integration.conf.utils.TestMocks.noobie_2024_dec;
import static api.poja.io.integration.conf.utils.TestMocks.noobie_2025_dec;
import static api.poja.io.integration.conf.utils.TestMocks.noobie_2025_nov;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsUnauthorizedException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.CostApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.model.UserCost;
import api.poja.io.integration.conf.utils.TestUtils;
import api.poja.io.repository.jpa.UserCostRepository;
import java.math.BigDecimal;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserCostIT extends MockedThirdParties {
  @Autowired UserCostRepository repository;

  @BeforeEach
  void setUp() {
    setUpGithub(githubComponentMock);
  }

  private ApiClient adminApiClient() {
    return TestUtils.anApiClient(ADMIN_TOKEN, port);
  }

  private ApiClient joeDoeApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  private ApiClient anonymousApiClient() {
    return TestUtils.anApiClient("dummy", port);
  }

  @SneakyThrows
  @Test
  void userCost_canBe_retrieved() {
    var api = new CostApi(adminApiClient());

    assertEquals(joe_2025_dec(), ignoreUpdateTs(api.getUserCost(JOE_DOE_ID, 2025, DECEMBER)));
    assertEquals(noobie_2025_dec(), ignoreUpdateTs(api.getUserCost(NOOBIE_ID, 2025, DECEMBER)));
    assertEquals(noobie_2025_nov(), ignoreUpdateTs(api.getUserCost(NOOBIE_ID, 2025, NOVEMBER)));
    assertEquals(noobie_2024_dec(), ignoreUpdateTs(api.getUserCost(NOOBIE_ID, 2024, DECEMBER)));

    long oldCount = repository.count();
    var newUserCost = api.getUserCost(JANE_DOE_ID, 2025, DECEMBER);
    assertEquals(JANE_DOE_ID, newUserCost.getUserId());
    assertNotNull(newUserCost.getUpdatedAt());
    assertEquals(BigDecimal.ZERO, newUserCost.getAmount());

    assertEquals(oldCount + 1, repository.count());
  }

  @SneakyThrows
  @Test
  void totalCost_canBe_retrieved() {
    var api = new CostApi(adminApiClient());

    var dec_2025 = api.getTotalCost(2025, DECEMBER);
    var dec_2024 = api.getTotalCost(2024, DECEMBER);
    var nov_2025 = api.getTotalCost(2025, NOVEMBER);
    var jan_2022 = api.getTotalCost(2022, JANUARY);

    assertEquals(new BigDecimal("3.00"), dec_2025.getAmount());
    assertEquals(new BigDecimal("9.00"), dec_2024.getAmount());
    assertEquals(new BigDecimal("12.00"), nov_2025.getAmount());
    assertEquals(BigDecimal.ZERO, jan_2022.getAmount());
  }

  @SneakyThrows
  @Test
  void costApi_canOnlyBe_accessed_byAdmin() {
    int year = 2025;
    var month = DECEMBER;

    assertDoesNotThrow(() -> new CostApi(adminApiClient()).getTotalCost(year, month));
    assertThrowsForbiddenException(
        () -> new CostApi(joeDoeApiClient()).getTotalCost(year, month), "Access Denied");
  }

  @SneakyThrows
  @Test
  void costApi_cannotBe_accessed_anonymously() {
    var api = new CostApi(anonymousApiClient());

    assertThrowsUnauthorizedException(() -> api.getTotalCost(2025, DECEMBER), "Bad credentials");
  }

  private static UserCost ignoreUpdateTs(UserCost userCost) {
    return userCost.updatedAt(null);
  }
}
