package api.poja.io.endpoint.rest.mapper.UserCostMapper;

import api.poja.io.endpoint.rest.model.Cost;
import api.poja.io.endpoint.rest.model.UserCost;
import api.poja.io.model.cost.CostByTimeUpdated;
import java.time.LocalDate;
import java.time.Month;
import org.springframework.stereotype.Component;

@Component
public class UserCostMapper {
  public UserCost toRest(api.poja.io.repository.model.UserCost domain) {
    var startDate = LocalDate.of(domain.getYear(), Month.valueOf(domain.getMonth().name()), 1);
    return new UserCost()
        .id(domain.getId())
        .userId(domain.getUserId())
        .amount(domain.getAmount())
        .startDate(startDate)
        .endDate(startDate.withDayOfMonth(startDate.lengthOfMonth()))
        .updatedAt(domain.getUpdatedAt());
  }

  public Cost toRest(CostByTimeUpdated domain) {
    var timePeriod = domain.timePeriod();
    return new Cost()
        .startDate(timePeriod.start())
        .endDate(timePeriod.end())
        .amount(domain.amount())
        .updatedAt(domain.updatedAt());
  }
}
