package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.NOOBIE_ID;
import static api.poja.io.model.money.Currency.Family.EUR;
import static api.poja.io.model.money.Currency.Family.USD;
import static api.poja.io.model.money.ExchangeRateTest.FX_RATE;
import static api.poja.io.model.tag.PojaTags.APP_ID;
import static api.poja.io.model.tag.PojaTags.USER_ID;
import static java.time.Month.APRIL;
import static java.time.Month.DECEMBER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.aws.costexplorer.CostExplorerComponent;
import api.poja.io.endpoint.event.model.RefreshUserCostRequested;
import api.poja.io.model.DateInterval;
import api.poja.io.model.cost.CostByTime;
import api.poja.io.repository.model.Application;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.UserCostService;
import api.poja.io.service.UserService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.costexplorer.model.TagValues;

class RefreshUserCostRequestedServiceTest {
  UserCostService userCostServiceMock;
  UserService userServiceMock;
  ApplicationService applicationServiceMock;
  CostExplorerComponent costExplorerComponentMock;

  RefreshUserCostRequestedService subject;

  @BeforeEach
  void setUp() {
    userCostServiceMock = mock();
    userServiceMock = mock();
    applicationServiceMock = mock();
    costExplorerComponentMock = mock();
    subject =
        new RefreshUserCostRequestedService(
            FX_RATE,
            userCostServiceMock,
            userServiceMock,
            applicationServiceMock,
            costExplorerComponentMock);
  }

  @Test
  void excludedUser_shouldBe_skipped() {
    var userId = NOOBIE_ID;
    var yearMonth = YearMonth.of(2025, APRIL);
    var date = yearMonth.atDay(1);

    when(userServiceMock.shouldComputeCost(userId, yearMonth)).thenReturn(false);

    subject.accept(new RefreshUserCostRequested(userId, date));

    verify(userServiceMock).shouldComputeCost(userId, yearMonth);
    verifyNoMoreInteractions(
        userServiceMock, userCostServiceMock, applicationServiceMock, costExplorerComponentMock);
  }

  @Test
  void userCostAmount_shouldBe_updated() {
    var userId = JOE_DOE_ID;
    var yearMonth = YearMonth.of(2025, DECEMBER);
    var date = yearMonth.atDay(1);

    var amount = new BigDecimal("20.00");
    var app1 = applicationMock("haapi");
    var app2 = applicationMock("poja-api");

    when(userServiceMock.shouldComputeCost(userId, yearMonth)).thenReturn(true);
    when(applicationServiceMock.findAllNotArchivedByUserId(userId)).thenReturn(List.of(app1, app2));
    when(costExplorerComponentMock.getMonthlyServiceCostsByTags(
            eq(date), any(TagValues.class), any(TagValues.class)))
        .thenReturn(new CostByTime(DateInterval.from(yearMonth), amount));

    subject.accept(new RefreshUserCostRequested(userId, date));

    verify(userServiceMock).shouldComputeCost(userId, yearMonth);
    verify(applicationServiceMock).findAllNotArchivedByUserId(userId);
    verify(costExplorerComponentMock)
        .getMonthlyServiceCostsByTags(
            date,
            TagValues.builder().key(USER_ID).values(JOE_DOE_ID).build(),
            TagValues.builder().key(APP_ID).values("haapi", "poja-api").build());
    verify(userCostServiceMock).updateAmount(userId, yearMonth, FX_RATE.apply(amount, USD, EUR));
    verifyNoMoreInteractions(
        userServiceMock, userCostServiceMock, applicationServiceMock, costExplorerComponentMock);
  }

  @Test
  void userCostRefresh_shouldBe_skipped_when_user_hasNo_application() {
    var userId = NOOBIE_ID;
    var yearMonth = YearMonth.of(2025, DECEMBER);
    var date = yearMonth.atDay(1);

    when(userServiceMock.shouldComputeCost(userId, yearMonth)).thenReturn(true);
    when(applicationServiceMock.findAllNotArchivedByUserId(userId)).thenReturn(List.of());

    subject.accept(new RefreshUserCostRequested(userId, date));

    verify(userServiceMock).shouldComputeCost(userId, yearMonth);
    verify(applicationServiceMock).findAllNotArchivedByUserId(userId);
    verify(userCostServiceMock).updateTimestamp(eq(userId), eq(yearMonth), any());
    verifyNoMoreInteractions(userServiceMock, applicationServiceMock, costExplorerComponentMock);
  }

  static Application applicationMock(String appName) {
    Application applicationMock = mock();
    when(applicationMock.getId()).thenReturn(appName);
    return applicationMock;
  }
}
