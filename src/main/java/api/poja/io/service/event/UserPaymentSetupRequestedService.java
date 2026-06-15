package api.poja.io.service.event;

import static api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum.PAYMENT_SETUP_COMPLETED;
import static api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum.PAYMENT_SETUP_FAILED;
import static api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum.PAYMENT_SETUP_IN_PROGRESS;

import api.poja.io.endpoint.event.model.UserPaymentSetupRequested;
import api.poja.io.service.UserPaymentSetupStateService;
import api.poja.io.service.UserService;
import api.poja.io.service.stripe.StripeService;
import com.stripe.exception.StripeException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class UserPaymentSetupRequestedService implements Consumer<UserPaymentSetupRequested> {
  private final UserService userService;
  private final StripeService stripeService;
  private final UserPaymentSetupStateService userPaymentSetupStateService;

  @Override
  public void accept(UserPaymentSetupRequested userPaymentSetupRequested) {
    var user = userPaymentSetupRequested.getUser();

    log.info("Create stripe customer for user id {}", user.getId());
    userPaymentSetupStateService.save(user.getId(), PAYMENT_SETUP_IN_PROGRESS);
    var stripeName = user.getFirstName() + " " + user.getLastName();
    try {
      var customerId = stripeService.createCustomer(stripeName, user.getEmail()).getId();
      userService.updateStripeId(user.getId(), customerId);
      userPaymentSetupStateService.save(user.getId(), PAYMENT_SETUP_COMPLETED);
    } catch (StripeException e) {
      userPaymentSetupStateService.save(user.getId(), PAYMENT_SETUP_FAILED);
      log.error("Stripe customer creation failed ", e);
    }
  }
}
