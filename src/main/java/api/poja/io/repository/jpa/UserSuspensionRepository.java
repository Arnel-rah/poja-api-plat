package api.poja.io.repository.jpa;

import api.poja.io.repository.model.UserSuspension;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserSuspensionRepository extends JpaRepository<UserSuspension, String> {
  @Query(
      """
select u from UserSuspension u
where u.userId = ?1
  and u.suspensionReason = ?2
  and u.suspendedAt = (select max(us.suspendedAt)
      from UserSuspension us
      where us.userId = ?1 AND us.suspensionReason = ?2)
  AND u.suspendedAt >= ?3
""")
  Optional<UserSuspension> findByUserIdAndSuspensionReasonSince(
      String userId, String suspensionReason, Instant since);
}
