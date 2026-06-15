package api.poja.io.service.event;

import api.poja.io.endpoint.event.model.UserSubscriptionRenewalRequested;
import api.poja.io.repository.model.UserSubscription;
import api.poja.io.service.UserSubscriptionService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserSubscriptionRenewalRequestedService
    implements Consumer<UserSubscriptionRenewalRequested> {
  private final UserSubscriptionService userSubscriptionService;

  @Override
  public void accept(UserSubscriptionRenewalRequested event) {
    UserSubscription userSubscription = event.getUserCurrentSubscription();
    boolean isManualSubscription = false;
    userSubscriptionService.subscribe(
        userSubscription.getUserId(),
        userSubscription.getOffer().getId(),
        event.getNextSubscriptionBegin(),
        isManualSubscription);
    userSubscriptionService.updateWillRenew(userSubscription.getId(), false);
  }
}
