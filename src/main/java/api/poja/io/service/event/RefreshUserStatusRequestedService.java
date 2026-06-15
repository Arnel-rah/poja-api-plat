package api.poja.io.service.event;

import static api.poja.io.endpoint.event.model.UserStatusUpdateRequested.StatusAlteration.ACTIVATE;
import static api.poja.io.endpoint.event.model.UserStatusUpdateRequested.StatusAlteration.SUSPEND;
import static api.poja.io.model.RangedInstant.getRangedInstant;
import static api.poja.io.model.money.Money.ZERO;
import static java.time.Instant.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.RefreshUserStatusRequested;
import api.poja.io.endpoint.event.model.UserStatusUpdateRequested;
import api.poja.io.model.RangedInstant;
import api.poja.io.model.User;
import api.poja.io.repository.model.UserPaymentRequest;
import api.poja.io.repository.model.UserSubscription;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.UserPaymentRequestService;
import api.poja.io.service.UserService;
import api.poja.io.service.UserSubscriptionService;
import api.poja.io.service.UserSubscriptionService.SubscriptionUsage;
import api.poja.io.service.pricing.PricingConf;
import api.poja.io.service.stripe.StripeService;
import api.poja.io.service.user.UserSuspensionConf;
import api.poja.io.service.workflows.userState.UserStateService;
import api.poja.io.sys.platform.SaasOnly;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@SaasOnly
public class RefreshUserStatusRequestedService implements Consumer<RefreshUserStatusRequested> {

  public static final Duration THIRTY_DAYS = Duration.ofDays(30);
  public static final String PAYMENT_OVERDUE_STATUS_REASON = "There are payments that are overdue.";
  public static final String FREE_TIER_EXCEEDED_STATUS_REASON =
      "The limit of the free tier has been reached.";
  public static final String HAS_UNPAID_SUB_AND_DOES_NOT_COMPLY_TO_BASIC_OFFER =
      "Current month's subscription is unpaid and you have too many applications.";
  private final UserSuspensionConf userSuspensionConf;
  private final BillingInfoService billingInfoService;
  private final UserService userService;
  private final StripeService stripeService;
  private final UserPaymentRequestService userPaymentRequestService;
  private final UserSubscriptionService userSubscriptionService;
  private final EventProducer<UserStatusUpdateRequested> eventProducer;
  private final UserStateService userStateService;
  private final PricingConf pricingConf;

  private static String getNoSpendingStatusReason(Duration duration) {
    return "There has been no spending activity for %s.".formatted(duration);
  }

  private Optional<String> resolveBlockReason(
      boolean hasLateUnpaidPayments,
      boolean hasUnpaidSubAndDoesNotComplyToBasicOffer,
      boolean hasExceededFreeTierWithoutPaymentMethod,
      boolean noSpending) {
    if (hasLateUnpaidPayments) {
      return of(PAYMENT_OVERDUE_STATUS_REASON);
    }
    if (hasUnpaidSubAndDoesNotComplyToBasicOffer) {
      return of(HAS_UNPAID_SUB_AND_DOES_NOT_COMPLY_TO_BASIC_OFFER);
    }
    if (hasExceededFreeTierWithoutPaymentMethod) {
      return of(FREE_TIER_EXCEEDED_STATUS_REASON);
    }
    if (noSpending) {
      return of(getNoSpendingStatusReason(userSuspensionConf.maxAllowedInactivityDuration()));
    }
    return empty();
  }

  @Override
  public void accept(RefreshUserStatusRequested refreshUserStatusRequested) {
    User user = userService.getUserById(refreshUserStatusRequested.getUserId());
    boolean hasPaymentMethods = !stripeService.getPaymentMethods(user.getStripeId()).isEmpty();
    boolean hasLateUnpaidPayments =
        !userPaymentRequestService.getUsersMonthlyPayments(user.getId()).stream()
            .filter(UserPaymentRequest::isOneMonthLateAndDefinitelyUnpaid)
            .toList()
            .isEmpty();
    var isPremium =
        userSubscriptionService.findActiveSubscriptionByUserId(user.getId()).isPresent();
    YearMonth currentYearMonth = YearMonth.now();
    if ((hasPaymentMethods || isPremium) && !hasLateUnpaidPayments) {
      activateUser(user, refreshUserStatusRequested);
      return;
    }

    var hasUnpaidSubAndDoesNotComplyToBasicOffer =
        hasUnpaidSubAndDoesNotComplyToBasicOffer(user, currentYearMonth);

    if (userStateService.isLastSuspended(user.getId())) {
      log.info("User.id={} is currently suspended.", user.getId());
      return;
    }

    var rangedInstant =
        new RangedInstant(
            refreshUserStatusRequested.getPricingCalculationRequestStartTime(),
            refreshUserStatusRequested.getPricingCalculationRequestEndTime());
    var monthToDateBilling =
        billingInfoService.getUserBillingInfo(user.getId(), getRangedInstant(currentYearMonth));

    boolean hasExceededFreeTier =
        monthToDateBilling.getComputedPriceAsMoney().compareTo(pricingConf.freeTierAsMoney()) > 0;
    boolean spendingInactive = isSpendingInactive(user.getId(), rangedInstant, user.getJoinedAt());
    boolean hasExceededFreeTierWithoutPaymentMethod = hasExceededFreeTier && !hasPaymentMethods;

    Optional<String> blockReason =
        resolveBlockReason(
            hasLateUnpaidPayments,
            hasUnpaidSubAndDoesNotComplyToBasicOffer,
            hasExceededFreeTierWithoutPaymentMethod,
            spendingInactive);
    blockReason.ifPresentOrElse(
        s ->
            eventProducer.accept(
                List.of(
                    UserStatusUpdateRequested.builder()
                        .status(SUSPEND)
                        .statusReason(s)
                        .userId(refreshUserStatusRequested.getUserId())
                        .requestedAt(now())
                        .build())),
        () -> activateUser(user, refreshUserStatusRequested));
  }

  private void activateUser(User user, RefreshUserStatusRequested refreshUserStatusRequested) {
    if (userStateService.isLastActive(user.getId())) {
      log.info("User.id={} is currently active.", user.getId());
      return;
    }
    eventProducer.accept(
        List.of(
            UserStatusUpdateRequested.builder()
                .status(ACTIVATE)
                .userId(refreshUserStatusRequested.getUserId())
                .requestedAt(now())
                .build()));
  }

  private boolean hasUnpaidSubAndDoesNotComplyToBasicOffer(User user, YearMonth currentYearMonth) {
    Optional<UserSubscription> optionalUnpaidSubscriptionOfCurrentMonth =
        userSubscriptionService.findUnpaidOfYearMonth(user.getId(), currentYearMonth);
    boolean hasUnpaidSubscription = optionalUnpaidSubscriptionOfCurrentMonth.isPresent();
    if (hasUnpaidSubscription) {
      UserSubscription unpaidSubscription = optionalUnpaidSubscriptionOfCurrentMonth.get();
      SubscriptionUsage basicOfferSubscriptionUsage =
          userSubscriptionService.getBasicOfferSubscriptionUsage(unpaidSubscription);
      return !basicOfferSubscriptionUsage.compliesToOffer();
    }
    return false;
  }

  private boolean isSpendingInactive(
      String userId, RangedInstant pricingComputationDatetime, Instant joinedAt) {
    Instant start =
        joinedAt.isAfter(pricingComputationDatetime.start())
            ? joinedAt
            : pricingComputationDatetime.start();
    if (Duration.between(start, pricingComputationDatetime.end())
            .compareTo(userSuspensionConf.maxAllowedInactivityDuration())
        < 0) {
      return false;
    }
    var rangedInstantBilling =
        billingInfoService.getUserBillingInfo(userId, pricingComputationDatetime);
    return ZERO.equals(rangedInstantBilling.getComputedPriceAsMoney());
  }
}
