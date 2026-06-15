package api.poja.io.repository.model.mapper;

import static api.poja.io.endpoint.rest.security.model.UserRole.USER;
import static api.poja.io.model.UserStatus.ACTIVE;
import static api.poja.io.service.pricing.PricingMethod.TWENTY_MICRO;
import static java.time.Instant.now;

import api.poja.io.endpoint.rest.model.CreateUser;
import api.poja.io.endpoint.rest.security.model.UserRole;
import api.poja.io.model.User;
import api.poja.io.model.UserStatus;
import api.poja.io.repository.model.SubscribedUserDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.SneakyThrows;
import org.kohsuke.github.GHMyself;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public static final UserRole[] CREATE_USER_DEFAULT_ROLE = {USER};
  public static final UserStatus CREATE_USER_DEFAULT_STATUS = ACTIVE;
  private final Set<String> betaTesterGhUsernames;
  private final String endToEndTestUserId;

  @SneakyThrows
  public UserMapper(
      @Value("${beta.users}") String betaTesterGhUsernames,
      ObjectMapper om,
      @Value("${e2e.user.id}") String endToEndTestUserId) {
    this.endToEndTestUserId = endToEndTestUserId;
    this.betaTesterGhUsernames = om.readValue(betaTesterGhUsernames, new TypeReference<>() {});
  }

  private boolean isBetaTester(String githubUsername) {
    return betaTesterGhUsernames.contains(githubUsername);
  }

  private boolean isEndToEndTestUser(String id) {
    return endToEndTestUserId.equals(id);
  }

  public User toModel(CreateUser toCreate, GHMyself githubUser) {
    String githubUsername = githubUser.getLogin();
    var isBetaTester = isBetaTester(githubUsername);
    var now = now();
    return User.builder()
        .firstName(toCreate.getFirstName())
        .lastName(toCreate.getLastName())
        .githubId(String.valueOf(githubUser.getId()))
        .email(toCreate.getEmail())
        .username(githubUsername)
        .avatar(githubUser.getAvatarUrl())
        .roles(CREATE_USER_DEFAULT_ROLE)
        .pricingMethod(TWENTY_MICRO)
        .betaTester(isBetaTester)
        .status(CREATE_USER_DEFAULT_STATUS)
        .statusUpdatedAt(now)
        .statusCheckedAt(now)
        .lastConnection(now)
        .build();
  }

  public User toModel(api.poja.io.repository.model.User entity) {
    String githubUsername = entity.getUsername();
    var isBetaTester = isBetaTester(githubUsername);
    return User.builder()
        .id(entity.getId())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .githubId(entity.getGithubId())
        .email(entity.getEmail())
        .username(entity.getUsername())
        .avatar(entity.getAvatar())
        .roles(entity.getRoles())
        .pricingMethod(entity.getPricingMethod())
        .stripeId(entity.getStripeId())
        .betaTester(isBetaTester)
        .isEndToEndTestUser(isEndToEndTestUser(entity.getId()))
        .latestSubscriptionId(entity.getLatestSubscriptionId())
        .mainOrgId(entity.getMainOrgId())
        .joinedAt(entity.getJoinedAt())
        .archived(entity.isArchived())
        .status(entity.getStatus())
        .statusReason(entity.getStatusReason())
        .statusUpdatedAt(entity.getStatusUpdatedAt())
        .statusCheckedAt(entity.getStatusCheckedAt())
        .lastConnection(entity.getLastConnection())
        .build();
  }

  public User toModel(SubscribedUserDTO dto) {
    var entity = dto.user();
    var activeSubscription = dto.activeSubscription();
    String githubUsername = entity.getUsername();
    var isBetaTester = isBetaTester(githubUsername);
    return User.builder()
        .id(entity.getId())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .githubId(entity.getGithubId())
        .email(entity.getEmail())
        .username(entity.getUsername())
        .avatar(entity.getAvatar())
        .roles(entity.getRoles())
        .pricingMethod(entity.getPricingMethod())
        .stripeId(entity.getStripeId())
        .betaTester(isBetaTester)
        .isEndToEndTestUser(isEndToEndTestUser(entity.getId()))
        .mainOrgId(entity.getMainOrgId())
        .joinedAt(entity.getJoinedAt())
        .archived(entity.isArchived())
        .activeSubscriptionId(activeSubscription == null ? null : activeSubscription.getId())
        .latestSubscriptionId(entity.getLatestSubscriptionId())
        .status(entity.getStatus())
        .statusReason(entity.getStatusReason())
        .statusUpdatedAt(entity.getStatusUpdatedAt())
        .statusCheckedAt(entity.getStatusCheckedAt())
        .lastConnection(entity.getLastConnection())
        .build();
  }

  public api.poja.io.repository.model.User toEntity(User model) {
    return api.poja.io.repository.model.User.builder()
        .id(model.getId())
        .firstName(model.getFirstName())
        .lastName(model.getLastName())
        .githubId(model.getGithubId())
        .email(model.getEmail())
        .username(model.getUsername())
        .avatar(model.getAvatar())
        .roles(model.getRoles())
        .pricingMethod(model.getPricingMethod())
        .latestSubscriptionId(model.getLatestSubscriptionId())
        .stripeId(model.getStripeId())
        .status(model.getStatus())
        .statusReason(model.getStatusReason())
        .statusUpdatedAt(model.getStatusUpdatedAt())
        .statusCheckedAt(model.getStatusCheckedAt())
        .lastConnection(model.getLastConnection())
        .build();
  }
}
