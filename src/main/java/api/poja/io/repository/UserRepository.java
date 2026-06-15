package api.poja.io.repository;

import api.poja.io.endpoint.rest.model.SortOrder;
import api.poja.io.endpoint.rest.model.SortUsersBy;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.User;
import api.poja.io.model.UserStatisticsDTO;
import api.poja.io.model.UserStatus;
import api.poja.io.model.UserWithLatestOrgInviteDTO;
import api.poja.io.model.page.Page;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
  Optional<User> findByGithubId(String githubId, Instant referenceDatetime);

  boolean existsByEmail(String email);

  boolean existsByGithubId(String githubId);

  boolean shouldComputeCost(String userId, YearMonth yearMonth);

  List<User> findAll();

  List<User> findAllToComputeBilling(Instant computeDatetime, LocalDate dateIntervalEnd);

  List<User> findAllToBillFor(YearMonth yearMonth);

  List<User> saveAll(List<User> users);

  Page<User> getUsersByCriteria(
      String username,
      SortUsersBy sortBy,
      SortOrder sortOrder,
      Instant referenceDatetime,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize);

  Page<User> getPaginatedUsersByOrgIdAndInviteStatus(
      String orgId,
      OrganizationInviteStatus status,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize);

  List<User> getAllUsersByOrgIdAndInviteStatus(String orgId, OrganizationInviteStatus status);

  Page<UserWithLatestOrgInviteDTO> getUsersByUsernameWithLastOrgInvite(
      String orgId,
      String username,
      Instant referenceDatetime,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize);

  void updateMainOrgId(String userId, String mainOrgId);

  void updateStripeId(String userId, String stripeId);

  void updateStatus(
      String id,
      UserStatus status,
      String statusReason,
      Instant statusUpdatedAt,
      Instant statusCheckedAt);

  void updateStatusCheckedAt(String id, Instant now);

  void updateLatestSubscriptionId(String userId, String subscriptionId);

  void updateLastConnection(String userId, Instant now);

  long countAll();

  void archiveUser(String id, Instant archivedAt);

  Optional<User> findById(String id, Instant referenceDatetime);

  UserStatisticsDTO getUserStatistics();
}
