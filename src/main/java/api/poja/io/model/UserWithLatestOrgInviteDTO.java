package api.poja.io.model;

import static api.poja.io.model.UserStatus.SUSPENDED;
import static java.time.Duration.ZERO;
import static java.time.Instant.now;

import api.poja.io.repository.model.OrganizationInvite;
import api.poja.io.repository.model.UserSubscription;
import java.time.Duration;
import java.time.Instant;

public record UserWithLatestOrgInviteDTO(
    String id,
    String username,
    String email,
    String firstName,
    String lastName,
    String avatar,
    UserStatus status,
    Instant joinedAt,
    Instant lastConnection,
    Instant statusUpdatedAt,
    Instant statusCheckedAt,
    String statusReason,
    boolean archived,
    UserSubscription activeSubscription,
    long orgMembershipsCount,
    OrganizationInvite latestInvite,
    String latestSubscriptionId) {
  public Duration suspensionDurationInSeconds() {
    if (SUSPENDED.equals(status)) {
      return Duration.between(now(), statusUpdatedAt);
    }
    return ZERO;
  }
}
