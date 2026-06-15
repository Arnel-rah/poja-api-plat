package api.poja.io.service;

import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.PaymentRequestRepository;
import api.poja.io.repository.model.PaymentRequest;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import java.time.Year;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentRequestService {
  private final PaymentRequestRepository repository;

  public PaymentRequest save(PaymentRequest paymentRequest) {
    return repository.save(paymentRequest);
  }

  public boolean existsByYearAndPeriod(Year year, PaymentRequestPeriod period) {
    return repository.existsByYearAndPeriod(year.getValue(), period);
  }

  public PaymentRequest getById(String id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Payment identified by id=" + id + " not found"));
  }

  public Optional<PaymentRequest> findById(String id) {
    return repository.findById(id);
  }
}
