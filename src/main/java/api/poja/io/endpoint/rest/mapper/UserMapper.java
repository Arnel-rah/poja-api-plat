package api.poja.io.endpoint.rest.mapper;

import static api.poja.io.endpoint.rest.model.UserStatusEnum.fromValue;
import static api.poja.io.repository.model.enums.OrganizationInviteStatus.ACCEPTED;

import api.poja.io.endpoint.rest.model.GetUserResponse;
import api.poja.io.endpoint.rest.model.User;
import api.poja.io.endpoint.rest.model.UserRoleEnum;
import api.poja.io.model.UserWithLatestOrgInviteDTO;
import api.poja.io.repository.model.UserSubscription;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component("restMapper")
@AllArgsConstructor
public class UserMapper {
  private final UserRoleMapper userRoleMapper;

  public User toRest(api.poja.io.model.User domain) {
    List<UserRoleEnum> roles = Stream.of(domain.getRoles()).map(userRoleMapper::toRest).toList();

    return new User()
        .id(domain.getId())
        .username(domain.getUsername())
        .email(domain.getEmail())
        .githubId(domain.getGithubId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .roles(roles)
        .avatar(domain.getAvatar())
        .stripeId(domain.getStripeId())
        .isBetaTester(domain.isBetaTester())
        .status(fromValue(domain.getStatus().getValue()))
        .statusUpdatedAt(domain.getStatusUpdatedAt())
        .statusCheckedAt(domain.getStatusCheckedAt())
        .statusReason(domain.getStatusReason())
        .mainOrgId(domain.getMainOrgId())
        .joinedAt(domain.getJoinedAt())
        .lastConnection(domain.getLastConnection())
        .isArchived(domain.isArchived())
        .activeSubscriptionId(domain.getActiveSubscriptionId())
        .latestSubscriptionId(domain.getLatestSubscriptionId())
        .suspensionDurationInSeconds(domain.getSuspensionDuration().getSeconds())
        .archived(domain.isArchived());
  }

  public GetUserResponse toGetUserResponse(api.poja.io.model.User domain) {
    return new GetUserResponse()
        .id(domain.getId())
        .username(domain.getUsername())
        .email(domain.getEmail())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .avatar(domain.getAvatar())
        .status(fromValue(domain.getStatus().getValue()))
        .statusUpdatedAt(domain.getStatusUpdatedAt())
        .statusCheckedAt(domain.getStatusCheckedAt())
        .statusReason(domain.getStatusReason())
        .joinedAt(domain.getJoinedAt())
        .lastConnection(domain.getLastConnection())
        .activeSubscriptionId(domain.getActiveSubscriptionId())
        .latestSubscriptionId(domain.getLatestSubscriptionId())
        .suspensionDurationInSeconds(domain.getSuspensionDuration().getSeconds())
        .archived(domain.isArchived());
  }

  public GetUserResponse toGetUserResponse(UserWithLatestOrgInviteDTO domain) {
    var latestInvite = domain.latestInvite();
    var isOrgMember = latestInvite != null && latestInvite.getStatus().equals(ACCEPTED);
    var canBeInvitedToMoreOrg = true;

    UserSubscription activeSubscription = domain.activeSubscription();
    return new GetUserResponse()
        .id(domain.id())
        .username(domain.username())
        .email(domain.email())
        .firstName(domain.firstName())
        .lastName(domain.lastName())
        .avatar(domain.avatar())
        .isOrgMember(isOrgMember)
        .canBeInvitedToMoreOrg(canBeInvitedToMoreOrg)
        .status(fromValue(domain.status().getValue()))
        .joinedAt(domain.joinedAt())
        .lastConnection(domain.lastConnection())
        .statusUpdatedAt(domain.statusUpdatedAt())
        .statusCheckedAt(domain.statusCheckedAt())
        .statusReason(domain.statusReason())
        .activeSubscriptionId(activeSubscription == null ? null : activeSubscription.getId())
        .latestSubscriptionId(domain.latestSubscriptionId())
        .suspensionDurationInSeconds(domain.suspensionDurationInSeconds().getSeconds())
        .archived(domain.archived());
  }
}
