package api.poja.io.model;

import static api.poja.io.model.UserStatus.SUSPENDED;
import static java.time.Duration.ZERO;
import static java.time.Instant.now;

import api.poja.io.endpoint.rest.security.model.UserRole;
import api.poja.io.service.pricing.PricingMethod;
import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Slf4j
public class User {
  private String id;

  private String firstName;

  private String lastName;

  private String username;

  private String email;

  private UserRole[] roles;

  private String githubId;

  private String avatar;

  private String stripeId;

  private PricingMethod pricingMethod;

  /**
   * note(!): Will only contain the user final state, retrieve latest status including transitional
   * ones using {@link api.poja.io.service.workflows.userState.UserStateService}
   */
  private UserStatus status;

  @Nullable private Instant lastConnection;
  private Instant statusUpdatedAt;
  private Instant statusCheckedAt;
  private String statusReason;

  private boolean betaTester;
  private boolean isEndToEndTestUser;
  private String mainOrgId;
  private boolean archived;

  private Instant joinedAt;
  private String activeSubscriptionId;
  private String latestSubscriptionId;

  /**
   * This was used to compute the suspension duration of a user as the user had the confusing
   * 'UNDER_MODIFICATION' status which made a final status duration computation impossible
   */
  @Deprecated
  public Duration getSuspensionDuration() {
    if (!SUSPENDED.equals(status) || null == statusUpdatedAt) {
      return ZERO;
    }
    return Duration.between(now(), statusUpdatedAt);
  }
}
