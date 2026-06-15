package api.poja.io.repository.jpa;

import api.poja.io.repository.model.UserBillingDiscount;
import api.poja.io.repository.model.enums.PaymentRequestPeriod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBillingDiscountRepository extends JpaRepository<UserBillingDiscount, String> {
  List<UserBillingDiscount> findAllByUserIdAndYearAndMonth(
      String userId, int year, PaymentRequestPeriod month);
}
