package api.poja.io.service;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;

import api.poja.io.repository.jpa.UserSuspensionRepository;
import api.poja.io.repository.model.UserSuspension;
import api.poja.io.service.user.UserSuspensionConf;
import java.time.Duration;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserSuspensionService {
  private final UserSuspensionRepository repository;
  private final UserSuspensionConf userSuspensionConf;

  public UserSuspension save(UserSuspension toSave) {
    return repository.save(toSave);
  }

  public Optional<UserSuspension> findByRecentByUserIdAndSuspensionReason(
      String userId, String suspensionReason) {
    var since = now().minus(userSuspensionConf.suspensionGracePeriod(), DAYS);
    return repository.findByUserIdAndSuspensionReasonSince(userId, suspensionReason, since);
  }

  public long computeRemainingSuspensionGracePeriodDays(UserSuspension userSuspension) {
    var nextEligible =
        userSuspension.getSuspendedAt().plus(userSuspensionConf.suspensionGracePeriod(), DAYS);
    return Duration.between(now(), nextEligible).toDays();
  }
}
