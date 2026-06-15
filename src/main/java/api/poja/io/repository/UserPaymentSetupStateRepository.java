package api.poja.io.repository;

import api.poja.io.repository.model.UserPaymentSetupState;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPaymentSetupStateRepository
    extends JpaRepository<UserPaymentSetupState, String> {
  List<UserPaymentSetupState> findAllByUserId(String userId, Sort sort);
}
