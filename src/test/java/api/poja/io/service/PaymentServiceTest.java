package api.poja.io.service;

import static java.time.Month.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import api.poja.io.conf.MockedThirdParties;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

class PaymentServiceTest extends MockedThirdParties {
  @Autowired PaymentService subject;
  @SpyBean PaymentRequestService paymentRequestService;
  @SpyBean UserService userService;

  @Test
  void initiate_December2024_payment_attempts_ko() {
    YearMonth yearMonth = YearMonth.of(2024, DECEMBER);
    subject.initiatePaymentAttempts(yearMonth);
    verify(userService, never()).findAllToBill(yearMonth);
    verify(paymentRequestService, never()).save(any());
    verify(eventProducerMock, never()).accept(anyList());
  }

  @Test
  void initiate_January2025_payment_attempts_ok() {
    YearMonth yearMonth = YearMonth.of(2025, JANUARY);
    subject.initiatePaymentAttempts(yearMonth);
    verify(userService, times(1)).findAllToBill(yearMonth);
    verify(paymentRequestService, times(1)).save(any());
    verify(eventProducerMock, atLeast(1)).accept(anyList());
  }
}
