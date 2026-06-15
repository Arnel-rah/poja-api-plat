package api.poja.io.service.user;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record UserSuspensionConf(
    @Value("${user.suspension.grace.period}") long suspensionGracePeriod,
    @Value("${max.allowed.inactivity.days}") long maxUserInactivityDaysNb) {
  public Duration maxAllowedInactivityDuration() {
    return Duration.ofDays(maxUserInactivityDaysNb);
  }
}
