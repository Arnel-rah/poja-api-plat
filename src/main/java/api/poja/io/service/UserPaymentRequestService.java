package api.poja.io.service;

import static org.springframework.data.domain.Pageable.unpaged;

import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.jpa.UserPaymentRequestRepository;
import api.poja.io.repository.model.UserPaymentRequest;
import api.poja.io.repository.model.enums.InvoiceStatus;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserPaymentRequestService {
  private final UserPaymentRequestRepository repository;

  public UserPaymentRequest save(UserPaymentRequest userPaymentRequest) {
    return repository.save(userPaymentRequest);
  }

  public Page<UserPaymentRequest> getUsersMonthlyPayments(
      String userId, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());
    var data = repository.findPaginatedByUserIdOrderByRequestInstant(userId, pageable);
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  public List<UserPaymentRequest> getUsersMonthlyPayments(String userId) {
    return repository.findAllByUserId(userId, unpaged());
  }

  public UserPaymentRequest getById(String id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Payment identified by id=" + id + " not found"));
  }

  public boolean existsByUserIdAndYearAndPeriod(
      String userId, Year year, PaymentRequestPeriod period) {
    return repository.existsByUserIdAndYearAndPeriod(userId, year.getValue(), period);
  }

  public Optional<UserPaymentRequest> findById(String id) {
    return repository.findById(id);
  }

  public List<UserPaymentRequest> findAllByUserIdAndInvoiceStatusIn(
      String userId, Set<InvoiceStatus> statuses) {
    return repository.findAllByUserIdAndInvoiceStatusIn(userId, statuses);
  }
}
