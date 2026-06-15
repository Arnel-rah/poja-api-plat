package api.poja.io.endpoint.rest.controller;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;

import api.poja.io.endpoint.rest.mapper.UserSubscriptionMapper;
import api.poja.io.endpoint.rest.model.GetUserSubscriptionsResponse;
import api.poja.io.endpoint.rest.model.PayInvoice;
import api.poja.io.endpoint.rest.model.SubscribeRequestBody;
import api.poja.io.endpoint.rest.model.SubscriptionPayment;
import api.poja.io.endpoint.rest.model.UserSubscription;
import api.poja.io.service.UserSubscriptionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserSubscriptionController {
  private final UserSubscriptionService service;
  private final UserSubscriptionMapper mapper;

  @GetMapping("/users/{userId}/subscriptions")
  public GetUserSubscriptionsResponse getSubscriptions(
      @PathVariable String userId,
      @RequestParam(value = "active", required = false) Boolean active) {
    return new GetUserSubscriptionsResponse()
        .data(
            service.getSubscriptionsByUserIdAndActive(userId, active).stream()
                .map(mapper::toRest)
                .toList());
  }

  @PostMapping("/users/{userId}/subscriptions")
  public UserSubscription subscribe(
      @PathVariable String userId, @RequestBody SubscribeRequestBody body) {
    boolean isManualSubscription = true;
    return mapper.toRest(
        service.subscribe(
            userId, body.getOfferId(), now().truncatedTo(MILLIS), isManualSubscription));
  }

  @GetMapping("/users/{userId}/subscriptions/{user_subscription_id}")
  public UserSubscription getUserSubscription(
      @PathVariable String userId,
      @PathVariable("user_subscription_id") String userSubscriptionId) {
    return mapper.toRest(service.getByUserIdAndId(userId, userSubscriptionId));
  }

  @DeleteMapping("/users/{userId}/subscriptions/{user_subscription_id}")
  public UserSubscription unsubscribe(
      @PathVariable String userId,
      @PathVariable("user_subscription_id") String userSubscriptionId) {
    return mapper.toRest(service.unsubscribe(userId, userSubscriptionId));
  }

  @PostMapping("/users/{userId}/subscriptions/{user_subscription_id}/payment")
  public SubscriptionPayment paySubscription(
      @PathVariable String userId,
      @PathVariable("user_subscription_id") String userSubscriptionId,
      @RequestBody PayInvoice payInvoice) {
    var mappedSub = mapper.toRest(service.paySubscription(userId, userSubscriptionId, payInvoice));
    return new SubscriptionPayment()
        .invoice(mappedSub.getInvoice())
        .userSubscriptionId(mappedSub.getId());
  }
}
