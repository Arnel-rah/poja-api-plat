package api.poja.io.endpoint.rest.mapper;

import static java.lang.Math.toIntExact;

import api.poja.io.endpoint.rest.model.InvoiceStatusEnum;
import api.poja.io.endpoint.rest.model.MonthType;
import api.poja.io.endpoint.rest.model.Payment;
import api.poja.io.endpoint.rest.model.YearMonthType;
import api.poja.io.repository.model.UserPaymentRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MonthlyPaymentMapper {
  public Payment toRest(UserPaymentRequest domain) {
    var paymentRequest = domain.getPaymentRequest();
    return new Payment()
        .id(domain.getId())
        .yearMonth(
            new YearMonthType()
                .year(paymentRequest.getYear())
                .month(MonthType.valueOf(paymentRequest.getPeriod().name())))
        .amount(toIntExact(domain.getAmount()))
        .invoiceId(domain.getInvoiceId())
        .invoiceStatus(InvoiceStatusEnum.valueOf(domain.getInvoiceStatus().name()))
        .invoiceUrl(domain.getInvoiceUrl())
        .period(paymentRequest.getPeriod().name())
        .discountAmount(toIntExact(domain.getDiscountAmount()))
        .requestInstant(paymentRequest.getRequestInstant());
  }

  public List<Payment> toRest(List<UserPaymentRequest> monthlyPaymentRequests) {
    return monthlyPaymentRequests.stream().map(this::toRest).toList();
  }
}
