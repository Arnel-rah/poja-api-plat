package api.poja.io.service.event;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.UserSubscriptionRenewalRequested;
import api.poja.io.endpoint.event.model.UsersSubscriptionRenewalRequested;
import api.poja.io.repository.model.UserSubscription;
import api.poja.io.service.UserSubscriptionService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UsersSubscriptionRenewalRequestedService
    implements Consumer<UsersSubscriptionRenewalRequested> {
  private final UserSubscriptionService userSubscriptionService;
  private final EventProducer<PojaEvent> eventProducer;

  @Override
  public void accept(UsersSubscriptionRenewalRequested event) {
    var toRenewSubs = userSubscriptionService.findAllToRenew(event.getCurrentMonth());
    eventProducer.accept(toRenewSubs.stream().map(e -> toUserSubscriptionEvent(event, e)).toList());
  }

  private static PojaEvent toUserSubscriptionEvent(
      UsersSubscriptionRenewalRequested parent, UserSubscription userSubscription) {
    return new UserSubscriptionRenewalRequested(parent, userSubscription);
  }
}
