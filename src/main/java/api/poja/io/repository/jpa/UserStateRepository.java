package api.poja.io.repository.jpa;

import api.poja.io.repository.model.UserState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStateRepository extends JpaRepository<UserState, String> {
  Optional<UserState> findFirstByUserIdOrderByTimestampDesc(String userId);

  List<UserState> findAllByUserId(String userId, Sort sort);
}
