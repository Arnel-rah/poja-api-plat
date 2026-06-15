package api.poja.io.service.event;

import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_STRIPE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_CREATION_DATETIME;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_NAME;
import static api.poja.io.model.money.Currency.CENTS_EUR;
import static api.poja.io.model.money.Currency.EUR;
import static api.poja.io.repository.model.enums.BillingInfoComputeStatus.FINISHED;
import static api.poja.io.repository.model.enums.InvoiceStatus.UNKNOWN;
import static api.poja.io.repository.model.enums.PaymentRequestPeriod.DECEMBER;
import static api.poja.io.repository.model.enums.PaymentRequestPeriod.SEPTEMBER;
import static api.poja.io.service.pricing.PricingMethod.TEN_MICRO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.UserMonthlyPaymentRequestSaveRequested;
import api.poja.io.endpoint.event.model.UserMonthlyPaymentRequested;
import api.poja.io.model.money.Money;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.BillingInfo;
import api.poja.io.repository.model.UserPaymentRequest;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.BillingInfoService;
import api.poja.io.service.PaymentRequestService;
import api.poja.io.service.UserPaymentRequestService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserMonthlyPaymentRequestSaveRequestedServiceTest {
  static final Year YEAR_OF_2024 = Year.of(2024);
  static final YearMonth SEPTEMBER_2024 = YearMonth.of(YEAR_OF_2024.getValue(), Month.SEPTEMBER);

  UserMonthlyPaymentRequestSaveRequestedService subject;
  ApplicationService applicationServiceMock;
  BillingInfoService billingInfoServiceMock;
  UserPaymentRequestService userPaymentRequestServiceMock;
  PaymentRequestService paymentRequestServiceMock;
  EventProducer<UserMonthlyPaymentRequested> eventProducerMock;

  @BeforeEach
  void setup() {
    applicationServiceMock = mock();
    billingInfoServiceMock = mock();
    userPaymentRequestServiceMock = mock();
    paymentRequestServiceMock = mock();
    eventProducerMock = mock();

    when(userPaymentRequestServiceMock.existsByUserIdAndYearAndPeriod(
            eq(JOE_DOE_ID), eq(YEAR_OF_2024), eq(DECEMBER)))
        .thenReturn(true);
    when(applicationServiceMock.findAllToBillByUserId(eq(JOE_DOE_ID), eq(SEPTEMBER_2024)))
        .thenReturn(List.of(joePojaApplication1Domain()));
    when(billingInfoServiceMock.getUserBillingInfoByApplication(
            eq(JOE_DOE_ID), eq(POJA_APPLICATION_ID), any()))
        .thenReturn(joePojaApplication1BillingInfos());
    when(paymentRequestServiceMock.getById(any())).thenReturn(null);
    when(userPaymentRequestServiceMock.save(any())).thenReturn(joeDoePaymentRequest());

    subject =
        new UserMonthlyPaymentRequestSaveRequestedService(
            applicationServiceMock,
            billingInfoServiceMock,
            userPaymentRequestServiceMock,
            paymentRequestServiceMock,
            eventProducerMock);
  }

  @Test
  void should_NOT_SaveOrFireEvent_if_alreadyExisting_userPaymentRequest() {
    var event =
        UserMonthlyPaymentRequestSaveRequested.builder()
            .userId(JOE_DOE_ID)
            .paymentRequestId("payment_request_1_id")
            .customerId(JOE_DOE_STRIPE_ID)
            .period(DECEMBER)
            .year(YEAR_OF_2024)
            .pricingMethod(TEN_MICRO)
            .build();

    assertDoesNotThrow(() -> subject.accept(event));

    verify(userPaymentRequestServiceMock, never()).save(any());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void shouldSaveAndFireEvent_if_newUserPaymentRequest() {
    var event =
        UserMonthlyPaymentRequestSaveRequested.builder()
            .userId(JOE_DOE_ID)
            .paymentRequestId("payment_request_1_id")
            .customerId(JOE_DOE_STRIPE_ID)
            .period(SEPTEMBER)
            .year(YEAR_OF_2024)
            .pricingMethod(TEN_MICRO)
            .build();

    assertDoesNotThrow(() -> subject.accept(event));

    verify(userPaymentRequestServiceMock, times(1)).save(eq(joeDoePaymentRequest()));
    verify(eventProducerMock, times(1)).accept(eq(List.of(joeDoeMonthlyPaymentRequestedEvent())));
  }

  static UserPaymentRequest joeDoePaymentRequest() {
    return UserPaymentRequest.builder()
        .paymentRequest(null)
        .amount(172_800L)
        .invoiceId(null)
        .invoiceUrl(null)
        .invoiceStatus(UNKNOWN)
        .userId(JOE_DOE_ID)
        .build();
  }

  static UserMonthlyPaymentRequested joeDoeMonthlyPaymentRequestedEvent() {
    var event =
        UserMonthlyPaymentRequested.builder()
            .customerId(JOE_DOE_STRIPE_ID)
            .yearMonth(SEPTEMBER_2024)
            .userId(JOE_DOE_ID)
            .build();
    var unitPrice = new Money(TEN_MICRO.getValue(), EUR).convertCurrency(CENTS_EUR);

    event.addInvoiceItem(
        POJA_APPLICATION_ID,
        "Mb-minutes of compute (" + POJA_APPLICATION_NAME + ")",
        unitPrice,
        172_800_240L);

    return event;
  }

  static Application joePojaApplication1Domain() {
    return Application.builder()
        .id(POJA_APPLICATION_ID)
        .name(POJA_APPLICATION_NAME)
        .userId(JOE_DOE_ID)
        .creationDatetime(POJA_APPLICATION_CREATION_DATETIME)
        .archived(false)
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .build();
  }

  static List<BillingInfo> joePojaApplication1BillingInfos() {
    return List.of(
        BillingInfo.builder()
            .id("joe_doe_billing_info_19")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-21T23:59:59Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("111.19239217329813"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("11119239.217329811"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_20")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-21T01:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("98.23"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("9823000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_21")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-22T03:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("105.75"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("10575000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_22")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-22T06:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("132.67"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("13267000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_23")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-23T08:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("175.45"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("17545000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_24")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-23T10:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("187.92"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("18792000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_25")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-24T13:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("112.45"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("11245000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_26")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-24T15:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("142.65"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("14265000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_27")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-25T18:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("190.89"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("19089000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_28")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-26T20:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("220.31"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("22031000"))
            .status(FINISHED)
            .build(),
        BillingInfo.builder()
            .id("joe_doe_billing_info_29")
            .computeDatetime(Instant.parse("2024-09-05T00:00:00Z"))
            .computationIntervalEnd(Instant.parse("2024-09-30T23:00:00Z"))
            .userId(JOE_DOE_ID)
            .appId(POJA_APPLICATION_ID)
            .envId("poja_application_environment_id")
            .queryId("dummy")
            .orgId(JOE_DOE_MAIN_ORG_ID)
            .pricingMethod(TEN_MICRO)
            .computedPrice(new BigDecimal("250.49"))
            .computedMemoryDurationInMbMinutes(new BigDecimal("25049000"))
            .status(FINISHED)
            .build());
  }
}
