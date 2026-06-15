package api.poja.io.endpoint.rest.controller;

import api.poja.io.endpoint.rest.mapper.MonthlyPaymentMapper;
import api.poja.io.endpoint.rest.mapper.PaymentCustomerMapper;
import api.poja.io.endpoint.rest.mapper.PaymentMapper;
import api.poja.io.endpoint.rest.model.PagedPayments;
import api.poja.io.endpoint.rest.model.PayInvoice;
import api.poja.io.endpoint.rest.model.Payment;
import api.poja.io.endpoint.rest.model.PaymentCustomer;
import api.poja.io.endpoint.rest.model.PaymentMethod;
import api.poja.io.endpoint.rest.model.PaymentMethodResponse;
import api.poja.io.endpoint.rest.model.PaymentMethodsAction;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.service.PaymentService;
import api.poja.io.service.UserPaymentRequestService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PaymentController {
  private final PaymentService paymentService;
  private final UserPaymentRequestService monthlyPaymentService;
  private final PaymentMapper mapper;
  private final PaymentCustomerMapper customerMapper;
  private final MonthlyPaymentMapper monthlyPaymentMapper;

  @GetMapping("/users/{userId}/payments")
  public PagedPayments getAllPayments(
      @PathVariable String userId,
      @RequestParam(required = false, defaultValue = "1") PageFromOne page,
      @RequestParam(required = false, defaultValue = "10", name = "page_size")
          BoundedPageSize pageSize) {
    var pagedResults = monthlyPaymentService.getUsersMonthlyPayments(userId, page, pageSize);
    return new PagedPayments()
        .count(pagedResults.count())
        .pageNumber(pagedResults.queryPage().getValue())
        .pageSize(pagedResults.queryPageSize().getValue())
        .hasPrevious(pagedResults.hasPrevious())
        .data(monthlyPaymentMapper.toRest(pagedResults.data().stream().toList()));
  }

  @PutMapping("/users/{userId}/payments/{paymentId}")
  public Payment payInvoiceManually(
      @PathVariable String paymentId,
      @PathVariable String userId,
      @RequestBody PayInvoice payInvoice) {
    return monthlyPaymentMapper.toRest(
        paymentService.payInvoiceManually(
            userId, paymentId, payInvoice.getInvoiceId(), payInvoice.getPaymentMethodId()));
  }

  @GetMapping("/users/{userId}/payment-details/payment-methods")
  public PaymentMethodResponse getPaymentMethods(@PathVariable String userId) {
    List<PaymentMethod> data =
        paymentService.getPaymentMethods(userId).stream().map(mapper::toRest).toList();
    return new PaymentMethodResponse().data(data);
  }

  @PutMapping("/users/{userId}/payment-details/payment-methods")
  public PaymentMethodResponse managePaymentMethod(
      @PathVariable String userId, @RequestBody PaymentMethodsAction paymentMethodsAction) {
    paymentService.managePaymentMethod(userId, paymentMethodsAction);
    List<PaymentMethod> data =
        paymentService.getPaymentMethods(userId).stream().map(mapper::toRest).toList();
    return new PaymentMethodResponse().data(data);
  }

  @GetMapping("/users/{userId}/payment-details")
  public PaymentCustomer getPaymentCustomer(@PathVariable String userId) {
    return customerMapper.toRest(paymentService.getCustomer(userId));
  }

  @PutMapping("/users/{userId}/payment-details")
  public PaymentCustomer updatePaymentCustomer(
      @PathVariable String userId, @RequestBody PaymentCustomer customer) {
    return customerMapper.toRest(paymentService.updateCustomer(userId, customer));
  }
}
