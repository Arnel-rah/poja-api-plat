package api.poja.io.service.event;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshUserCostRequested;
import api.poja.io.endpoint.event.model.RefreshUsersCostTriggered;
import api.poja.io.model.User;
import api.poja.io.service.UserService;
import api.poja.io.sys.platform.SaasOnly;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@SaasOnly
public class RefreshUsersCostTriggeredService implements Consumer<RefreshUsersCostTriggered> {
  private final UserService userService;
  private final EventProducer<RefreshUserCostRequested> eventProducer;

  @Override
  public void accept(RefreshUsersCostTriggered event) {
    var date = event.getDate();
    var period = YearMonth.from(event.getDate());

    log.info("RefreshUsersCostTriggered for period {}", period);

    List<User> users = userService.findAllToBill(period);

    var events = users.stream().map(u -> toRefreshCostUserRequestedEvent(u, date)).toList();
    eventProducer.accept(events);
  }

  private static RefreshUserCostRequested toRefreshCostUserRequestedEvent(
      User user, LocalDate date) {
    return new RefreshUserCostRequested(user.getId(), date);
  }
}
