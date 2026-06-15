package api.poja.io.service.event;

import static api.poja.io.model.RangedInstant.getRangedInstant;
import static api.poja.io.model.money.Currency.CENTS_EUR;
import static api.poja.io.model.money.Currency.EUR;
import static api.poja.io.model.money.Money.ZERO;
import static api.poja.io.repository.model.enums.InvoiceStatus.UNKNOWN;
import static java.math.RoundingMode.CEILING;
import static java.time.ZoneOffset.UTC;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.UserMonthlyPaymentRequestSaveRequested;
import api.poja.io.endpoint.event.model.UserMonthlyPaymentRequested;
import api.poja.io.model.RangedInstant;
import api.poja.io.model.money.Money;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.repository.model.UserPaymentRequest;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.PaymentRequestService;
import api.poja.io.service.UserPaymentRequestService;
import api.poja.io.service.pricing.PricingMethod;
import api.poja.io.sys.platform.SaasOnly;
import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@SaasOnly
public class UserMonthlyPaymentRequestSaveRequestedService
    implements Consumer<UserMonthlyPaymentRequestSaveRequested> {
  private final ApplicationService applicationService;
  private final BillingInfoService billingInfoService;
  private final UserPaymentRequestService userPaymentRequestService;
  private final PaymentRequestService paymentRequestService;
  private final EventProducer<UserMonthlyPaymentRequested> eventProducer;

  @Override
  public void accept(
      UserMonthlyPaymentRequestSaveRequested userMonthlyPaymentRequestSaveRequested) {
    String userId = userMonthlyPaymentRequestSaveRequested.getUserId();
    YearMonth yearMonth = userMonthlyPaymentRequestSaveRequested.getYearMonth();
    Year year = Year.of(yearMonth.getYear());
    PaymentRequestPeriod period = userMonthlyPaymentRequestSaveRequested.getPeriod();
    var pricingMethod = userMonthlyPaymentRequestSaveRequested.getPricingMethod();
    if (userPaymentRequestService.existsByUserIdAndYearAndPeriod(userId, year, period)) {
      log.error(
          "user payment request with userId = {} year = {} and period = {} already exists.",
          userId,
          year,
          period);
      return;
    }
    var paymentRequestedEvent =
        createMonthlyPaymentRequestedEvent(
            userId,
            yearMonth,
            userMonthlyPaymentRequestSaveRequested.getCustomerId(),
            pricingMethod);
    var savedPaymentRequest =
        userPaymentRequestService.save(
            UserPaymentRequest.builder()
                .paymentRequest(
                    paymentRequestService.getById(
                        userMonthlyPaymentRequestSaveRequested.getPaymentRequestId()))
                .amount(
                    paymentRequestedEvent
                        .computeDueAmount()
                        .convertCurrency(CENTS_EUR)
                        .amount()
                        .longValue())
                .invoiceId(null)
                .invoiceUrl(null)
                .invoiceStatus(UNKNOWN)
                .userId(userId)
                .build());
    paymentRequestedEvent.setPaymentRequestId(savedPaymentRequest.getId());
    paymentRequestedEvent.setUserId(savedPaymentRequest.getUserId());
    eventProducer.accept(List.of(paymentRequestedEvent));
  }

  private UserMonthlyPaymentRequested createMonthlyPaymentRequestedEvent(
      String userId, YearMonth yearMonth, String customerId, PricingMethod pricingMethod) {
    var rangedInstant = getRangedInstant(yearMonth);
    log.info(
        "billing user {} for period from {} to {} month {} year {}",
        userId,
        yearMonth.atDay(1).atStartOfDay(UTC).toInstant(),
        yearMonth.atEndOfMonth().atTime(23, 59, 59).toInstant(UTC),
        yearMonth.getMonth(),
        yearMonth.getYear());
    List<Application> apps = applicationService.findAllToBillByUserId(userId, yearMonth);
    final UserMonthlyPaymentRequested event =
        UserMonthlyPaymentRequested.builder().customerId(customerId).yearMonth(yearMonth).build();
    apps.forEach(
        app -> addInvoiceItemPricedWithin(event, pricingMethod, userId, app, rangedInstant));
    return event;
  }

  private void addInvoiceItemPricedWithin(
      UserMonthlyPaymentRequested event,
      PricingMethod pricingMethod,
      String userId,
      Application app,
      RangedInstant rangedInstant) {
    var userBillingInfoByApplication =
        billingInfoService.getUserBillingInfoByApplication(userId, app.getId(), rangedInstant);
    var dueAmount =
        userBillingInfoByApplication.stream()
            .map(BillingInfo::getComputedPriceAsMoney)
            .reduce(ZERO, Money::add);

    if (dueAmount.compareTo(ZERO) > 0) {
      var memoryDurationQuantity =
          userBillingInfoByApplication.stream()
              .map(BillingInfo::getComputedMemoryDurationInMbMinutes)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      var description = "Mb-minutes of compute (" + app.getName() + ")";
      // Round up memoryDuration to nearest integer
      var roundedMemoryDurationQuantity = memoryDurationQuantity.setScale(0, CEILING).longValue();
      var unitPrice = new Money(pricingMethod.getValue(), EUR).convertCurrency(CENTS_EUR);

      event.addInvoiceItem(app.getId(), description, unitPrice, roundedMemoryDurationQuantity);
    } else {
      log.info("will not add app.id={} app.name={}, dueAmount is 0$", app.getId(), app.getName());
    }
  }
}
