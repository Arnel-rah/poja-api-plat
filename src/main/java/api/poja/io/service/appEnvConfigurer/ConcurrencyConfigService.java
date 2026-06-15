package api.poja.io.service.appEnvConfigurer;

import api.poja.io.endpoint.rest.model.ComputeConf9;
import api.poja.io.endpoint.rest.model.ConcurrencyConf;
import api.poja.io.endpoint.rest.model.ConcurrencyConf2;
import api.poja.io.endpoint.rest.model.ConcurrencyConf9;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.pojaConf.conf1.PojaConf1Concurrency;
import api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency;
import api.poja.io.model.pojaConf.conf9.PojaConf9Concurrency;
import api.poja.io.service.UserSubscriptionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConcurrencyConfigService {
  private final UserSubscriptionService userSubscriptionService;

  public PojaConf1Concurrency pojaConf1ConcurrencyFrom(
      ConcurrencyConf concurrencyConf, String userId) {
    var activeUserSubscription = userSubscriptionService.findActiveSubscriptionByUserId(userId);
    boolean hasActiveSubscription = activeUserSubscription.isPresent();
    if (hasActiveSubscription) {
      if (concurrencyConf == null) {
        return PojaConf1Concurrency.PREMIUM_USER_CONCURRENCY;
      }
      var constraints = PojaConf1Concurrency.getPremiumUserInvalidAttributes(concurrencyConf);
      var concurrency = PojaConf1Concurrency.fromRest(concurrencyConf);
      if (constraints.isEmpty()) {
        return concurrency;
      }
      throw new BadRequestException(String.join(". ", constraints));
    }
    if (concurrencyConf == null) {
      return PojaConf1Concurrency.BASIC_USER_CONCURRENCY;
    }
    var constraints = PojaConf1Concurrency.getBasicUserInvalidAttributes(concurrencyConf);
    var concurrency = PojaConf1Concurrency.fromRest(concurrencyConf);
    if (constraints.isEmpty()) {
      return concurrency;
    }
    throw new BadRequestException(String.join(". ", constraints));
  }

  public PojaConf2Concurrency pojaConf2ConcurrencyFrom(
      ConcurrencyConf2 concurrencyConf, String userId) {
    var activeUserSubscription = userSubscriptionService.findActiveSubscriptionByUserId(userId);
    boolean hasActiveSubscription = activeUserSubscription.isPresent();
    if (hasActiveSubscription) {
      if (concurrencyConf == null) {
        return PojaConf2Concurrency.PREMIUM_USER_CONCURRENCY;
      }
      var constraints = PojaConf2Concurrency.getPremiumUserInvalidAttributes(concurrencyConf);
      var concurrency = new PojaConf2Concurrency(concurrencyConf);
      if (constraints.isEmpty()) {
        return concurrency;
      }
      throw new BadRequestException(String.join(". ", constraints));
    }
    if (concurrencyConf == null) {
      return PojaConf2Concurrency.BASIC_USER_CONCURRENCY;
    }
    var constraints = PojaConf2Concurrency.getBasicUserInvalidAttributes(concurrencyConf);
    var concurrency = new PojaConf2Concurrency(concurrencyConf);
    if (constraints.isEmpty()) {
      return concurrency;
    }
    throw new BadRequestException(String.join(". ", constraints));
  }

  public PojaConf9Concurrency pojaConf9ConcurrencyFrom(
      ConcurrencyConf9 concurrencyConf, ComputeConf9 computeConf, String userId) {
    var activeUserSubscription = userSubscriptionService.findActiveSubscriptionByUserId(userId);
    boolean hasActiveSubscription = activeUserSubscription.isPresent();
    if (hasActiveSubscription) {
      if (concurrencyConf == null) {
        return PojaConf9Concurrency.PREMIUM_USER_CONCURRENCY;
      }
      var constraints =
          PojaConf9Concurrency.getPremiumUserInvalidAttributes(concurrencyConf, computeConf);
      var concurrency = new PojaConf9Concurrency(concurrencyConf);
      if (constraints.isEmpty()) {
        return concurrency;
      }
      throw new BadRequestException(String.join(". ", constraints));
    }
    if (concurrencyConf == null) {
      return PojaConf9Concurrency.BASIC_USER_CONCURRENCY;
    }
    var constraints =
        PojaConf9Concurrency.getBasicUserInvalidAttributes(concurrencyConf, computeConf);
    var concurrency = new PojaConf9Concurrency(concurrencyConf);
    if (constraints.isEmpty()) {
      return concurrency;
    }
    throw new BadRequestException(String.join(". ", constraints));
  }
}
