package api.poja.io.endpoint.rest.controller.admin;

import api.poja.io.endpoint.rest.mapper.UserCostMapper.UserCostMapper;
import api.poja.io.endpoint.rest.model.Cost;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.UserCost;
import api.poja.io.service.UserCostService;
import java.time.Month;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserCostController {
  private final UserCostService service;
  private final UserCostMapper mapper;

  @GetMapping("/users/{userId}/cost")
  public UserCost getUserCost(
      @PathVariable String userId, @RequestParam int year, @RequestParam MonthType month) {
    return mapper.toRest(
        service.findOrCreateByUserIdAndPeriod(
            userId, YearMonth.of(year, Month.valueOf(month.getValue()))));
  }

  @GetMapping("/total-cost")
  public Cost getTotalCost(@RequestParam int year, @RequestParam MonthType month) {
    return mapper.toRest(
        service.getTotalCostByPeriod(YearMonth.of(year, Month.valueOf(month.getValue()))));
  }
}
