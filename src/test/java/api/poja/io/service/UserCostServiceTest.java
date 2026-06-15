package api.poja.io.service;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.LOREM_IPSUM_ID;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import api.poja.io.conf.MockedThirdParties;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserCostServiceTest extends MockedThirdParties {
  @Autowired UserCostService userCostService;

  @SneakyThrows
  @Test
  void userCostUpdateTimestamp_should_alwaysBe_updated_on_updateAmount_call() {
    var period = YearMonth.of(2020, Month.DECEMBER);

    var d1 = userCostService.findOrCreateByUserIdAndPeriod(LOREM_IPSUM_ID, period);
    var d1t = d1.getUpdatedAt();

    assertNotNull(d1t);

    Thread.sleep(500);

    userCostService.updateAmount(LOREM_IPSUM_ID, period, new BigDecimal("2"));

    var d2 = userCostService.findOrCreateByUserIdAndPeriod(JOE_DOE_ID, period);
    var d2t = d2.getUpdatedAt();

    assertNotNull(d2t);
    assertNotEquals(d1t, d2t);

    Thread.sleep(500);

    userCostService.updateAmount(LOREM_IPSUM_ID, period, new BigDecimal("2"));

    var d3 = userCostService.findOrCreateByUserIdAndPeriod(JOE_DOE_ID, period);
    var d3t = d3.getUpdatedAt();

    assertNotNull(d3t);
    assertNotEquals(d2t, d3t);
  }
}
