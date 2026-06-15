package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.UserBillingDiscount;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import org.springframework.stereotype.Component;

@Component
public class UserBillingDiscountMapper {
  public UserBillingDiscount toRest(api.poja.io.repository.model.UserBillingDiscount domain) {
    return new UserBillingDiscount()
        .id(domain.getId())
        .amount(domain.getAmount())
        .description(domain.getDescription())
        .year(domain.getYear())
        .month(MonthType.valueOf(domain.getMonth().name()))
        .creationDatetime(domain.getCreationDatetime())
        .userId(domain.getUserId());
  }

  public api.poja.io.repository.model.UserBillingDiscount toDomain(UserBillingDiscount rest) {
    return api.poja.io.repository.model.UserBillingDiscount.builder()
        .id(rest.getId())
        .amount(rest.getAmount())
        .description(rest.getDescription())
        .year(rest.getYear())
        .month(PaymentRequestPeriod.valueOf(rest.getMonth().getValue()))
        .creationDatetime(rest.getCreationDatetime())
        .userId(rest.getUserId())
        .build();
  }
}
