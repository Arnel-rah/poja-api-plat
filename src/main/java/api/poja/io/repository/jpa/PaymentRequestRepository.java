package api.poja.io.repository.jpa;

import api.poja.io.repository.model.PaymentRequest;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, String> {
  @Query(
      "SELECT CASE WHEN COUNT(pr) > 0 THEN TRUE ELSE FALSE END "
          + "FROM PaymentRequest pr "
          + "WHERE pr.year = ?1 AND pr.period = ?2")
  boolean existsByYearAndPeriod(int year, PaymentRequestPeriod period);
}
