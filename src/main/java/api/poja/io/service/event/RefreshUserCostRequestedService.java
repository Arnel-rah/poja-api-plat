package api.poja.io.service.event;

import static api.poja.io.model.money.Currency.EUR;
import static api.poja.io.model.money.Currency.USD;
import static api.poja.io.model.tag.PojaTags.APP_ID;
import static api.poja.io.model.tag.PojaTags.USER_ID;
import static java.time.Instant.now;

import api.poja.io.aws.costexplorer.CostExplorerComponent;
import api.poja.io.endpoint.event.model.RefreshUserCostRequested;
import api.poja.io.model.cost.CostByTime;
import api.poja.io.model.money.ExchangeRate;
import api.poja.io.model.money.Money;
import api.poja.io.repository.model.Application;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.UserCostService;
import api.poja.io.service.UserService;
import api.poja.io.sys.platform.SaasOnly;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.costexplorer.model.TagValues;

@Slf4j
@Service
@AllArgsConstructor
@SaasOnly
public class RefreshUserCostRequestedService implements Consumer<RefreshUserCostRequested> {
  private final ExchangeRate exchangeRate;
  private final UserCostService userCostService;
  private final UserService userService;
  private final ApplicationService applicationService;
  private final CostExplorerComponent costExplorer;

  @Override
  public void accept(RefreshUserCostRequested event) {
    var userId = event.getUserId();
    var date = event.getDate();
    var yearMonth = YearMonth.from(date);

    log.info("RefreshUserCostRequested for user {}, period {}", userId, yearMonth);

    if (!userService.shouldComputeCost(userId, yearMonth)) {
      log.info("Not computing user cost for user {}, period {}", userId, yearMonth);
      return;
    }

    log.info("Starting cost refresh for user {}, period {}", userId, yearMonth);

    var applicationIds =
        applicationService.findAllNotArchivedByUserId(userId).stream()
            .map(Application::getId)
            .toList();

    if (applicationIds.isEmpty()) {
      log.info("No applications found for user {}, updating timestamp...", userId);
      userCostService.updateTimestamp(userId, yearMonth, now());
      return;
    }

    TagValues[] tagValues = {
      TagValues.builder().key(USER_ID).values(userId).build(),
      TagValues.builder().key(APP_ID).values(applicationIds).build(),
    };

    var costByTime = costExplorer.getMonthlyServiceCostsByTags(date, tagValues);

    log.info("user {} AWS Cost {} on {}", userId, costByTime.amount(), date);

    var eurAmount = fromAwsCeAmount(costByTime);
    userCostService.updateAmount(userId, yearMonth, eurAmount);
  }

  private BigDecimal fromAwsCeAmount(CostByTime awsCost) {
    var moneyUsd = new Money(awsCost.amount(), USD);
    var moneyEur = moneyUsd.convertCurrency(EUR, exchangeRate);
    return moneyEur.amount();
  }
}
