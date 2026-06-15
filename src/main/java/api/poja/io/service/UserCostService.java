package api.poja.io.service;

import static java.math.BigDecimal.ZERO;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;

import api.poja.io.model.DateInterval;
import api.poja.io.model.cost.CostByTimeUpdated;
import api.poja.io.repository.jpa.UserCostRepository;
import api.poja.io.repository.model.UserCost;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserCostService {
  private final UserCostRepository repository;

  public UserCost findOrCreateByUserIdAndPeriod(String userId, YearMonth period) {
    var userCostOpt =
        repository.findByUserIdAndYearMonth(
            userId, period.getYear(), PaymentRequestPeriod.from(period.getMonth()));
    return userCostOpt.orElseGet(() -> save(newDefaultUserCost(userId, period)));
  }

  public UserCost save(UserCost toSave) {
    return repository.save(toSave);
  }

  @Transactional
  public void updateAmount(String userId, YearMonth period, BigDecimal amountUsd) {
    var userCost = findOrCreateByUserIdAndPeriod(userId, period);
    repository.updateAmount(userCost.getId(), amountUsd, now());
  }

  @Transactional
  public void updateTimestamp(String userId, YearMonth period, Instant ts) {
    var userCost = findOrCreateByUserIdAndPeriod(userId, period);
    repository.updateTimestamp(userCost.getId(), ts);
  }

  public CostByTimeUpdated getTotalCostByPeriod(YearMonth yearMonth) {
    var userCosts =
        repository.findAllByYearAndMonth(
            yearMonth.getYear(), PaymentRequestPeriod.from(yearMonth.getMonth()));

    BigDecimal totalAmount =
        userCosts.stream().map(UserCost::getAmount).reduce(ZERO, BigDecimal::add);

    Instant lastUpdatedAt =
        userCosts.stream()
            .max(comparing(UserCost::getUpdatedAt))
            .map(UserCost::getUpdatedAt)
            .orElseGet(Instant::now);

    return new CostByTimeUpdated(DateInterval.from(yearMonth), totalAmount, lastUpdatedAt);
  }

  private static UserCost newDefaultUserCost(String userId, YearMonth period) {
    return UserCost.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .year(period.getYear())
        .month(PaymentRequestPeriod.from(period.getMonth()))
        .amount(ZERO)
        .updatedAt(now())
        .build();
  }
}
