package api.poja.io.repository.jpa;

import api.poja.io.repository.model.UserPaymentRequest;
import api.poja.io.repository.model.enums.InvoiceStatus;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPaymentRequestRepository extends JpaRepository<UserPaymentRequest, String> {
  List<UserPaymentRequest> findAllByUserId(String userId, Pageable pageable);

  @Query(
      "select u from UserPaymentRequest u inner join u.paymentRequest p where u.userId = ?1 order"
          + " by p.requestInstant desc ")
  List<UserPaymentRequest> findPaginatedByUserIdOrderByRequestInstant(
      String userId, Pageable pageable);

  @Query(
      "select CASE WHEN COUNT(upr) > 0 THEN TRUE ELSE FALSE END from UserPaymentRequest upr inner"
          + " join PaymentRequest pr on upr.paymentRequest.id = pr.id where upr.userId=?1 and"
          + "  pr.year= ?2 and pr.period=?3")
  boolean existsByUserIdAndYearAndPeriod(String userId, int year, PaymentRequestPeriod period);

  List<UserPaymentRequest> findAllByUserIdAndInvoiceStatusIn(
      String userId, Collection<InvoiceStatus> statuses);
}
