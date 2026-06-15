package api.poja.io.service;

import api.poja.io.endpoint.rest.mapper.UserBillingDiscountMapper;
import api.poja.io.repository.jpa.UserBillingDiscountRepository;
import api.poja.io.repository.model.UserBillingDiscount;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserBillingDiscountService {
  private final UserService userService;
  private final UserBillingDiscountRepository repository;
  private final UserBillingDiscountMapper mapper;

  public List<UserBillingDiscount> findAllByUserIdAndYearAndMonth(
      String userId, Integer year, PaymentRequestPeriod month) {
    var yearMonth =
        year == null || month == null
            ? YearMonth.now()
            : YearMonth.of(year, Month.valueOf(month.name()));
    return repository.findAllByUserIdAndYearAndMonth(
        userId, yearMonth.getYear(), PaymentRequestPeriod.valueOf(yearMonth.getMonth().name()));
  }

  public UserBillingDiscount save(
      String userId, api.poja.io.endpoint.rest.model.UserBillingDiscount toSave) {
    userService.getUserById(userId);
    return repository.save(mapper.toDomain(toSave));
  }
}
