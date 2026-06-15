package api.poja.io.service.event;

import static api.poja.io.service.OfferService.PREMIUM_OFFER_ID;

import api.poja.io.endpoint.event.model.UserSubscriptionRequested;
import api.poja.io.service.UserSubscriptionService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserSubscriptionRequestedService implements Consumer<UserSubscriptionRequested> {
  private final UserSubscriptionService userSubscriptionService;

  @Override
  public void accept(UserSubscriptionRequested event) {
    var isManualSubscription = false;
    userSubscriptionService.subscribe(
        event.getUserId(), PREMIUM_OFFER_ID, event.getSubscriptionBeginAt(), isManualSubscription);
  }
}
