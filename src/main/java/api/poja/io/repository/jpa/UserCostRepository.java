package api.poja.io.repository.jpa;

import api.poja.io.repository.model.UserCost;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserCostRepository extends JpaRepository<UserCost, String> {
  @Query("select uc from UserCost uc where uc.userId = ?1 and uc.year = ?2 and uc.month = ?3")
  Optional<UserCost> findByUserIdAndYearMonth(String userId, int year, PaymentRequestPeriod month);

  @Query("select uc from UserCost uc where uc.year = ?1 and uc.month = ?2")
  List<UserCost> findAllByYearAndMonth(int year, PaymentRequestPeriod month);

  @Modifying
  @Query("update UserCost uc set uc.amount = ?2, uc.updatedAt = ?3 where uc.id = ?1")
  void updateAmount(String id, BigDecimal amount, Instant at);

  @Modifying
  @Query("update UserCost uc set uc.updatedAt = ?2 where uc.id = ?1")
  void updateTimestamp(String id, Instant at);
}
