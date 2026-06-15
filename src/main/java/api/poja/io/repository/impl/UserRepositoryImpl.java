package api.poja.io.repository.impl;

import static api.poja.io.endpoint.rest.model.SortOrder.DESC;

import api.poja.io.endpoint.rest.model.SortOrder;
import api.poja.io.endpoint.rest.model.SortUsersBy;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.User;
import api.poja.io.model.UserStatisticsDTO;
import api.poja.io.model.UserStatus;
import api.poja.io.model.UserWithLatestOrgInviteDTO;
import api.poja.io.model.page.Page;
import api.poja.io.repository.UserRepository;
import api.poja.io.repository.jpa.UserJpaRepository;
import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import api.poja.io.repository.model.mapper.UserMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
class UserRepositoryImpl implements UserRepository {
  private final UserJpaRepository jpaRepository;
  private final UserMapper mapper;

  @Override
  public Optional<User> findByGithubId(String githubId, Instant referenceDatetime) {
    return jpaRepository.findByGithubId(githubId, referenceDatetime).map(mapper::toModel);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpaRepository.existsByEmail(email);
  }

  @Override
  public boolean existsByGithubId(String githubId) {
    return jpaRepository.existsByGithubId(githubId);
  }

  @Override
  public boolean shouldComputeCost(String userId, YearMonth yearMonth) {
    long yearMonthParam = yearMonth.getYear() * 100L + yearMonth.getMonthValue();
    return jpaRepository.shouldComputeCost(userId, yearMonthParam);
  }

  @Override
  public List<User> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toModel).toList();
  }

  @Override
  public List<User> findAllToComputeBilling(Instant computeDatetime, LocalDate dateIntervalEnd) {
    return jpaRepository.findAllToComputeBilling(computeDatetime, dateIntervalEnd).stream()
        .map(mapper::toModel)
        .toList();
  }

  @Override
  public List<User> findAllToBillFor(YearMonth yearMonth) {
    long yearMonthParam = yearMonth.getYear() * 100L + yearMonth.getMonthValue();
    return jpaRepository.findAllToBillFor(yearMonthParam).stream().map(mapper::toModel).toList();
  }

  @Override
  public List<User> saveAll(List<User> users) {
    return jpaRepository.saveAll(users.stream().map(mapper::toEntity).toList()).stream()
        .map(mapper::toModel)
        .toList();
  }

  @Override
  public Page<User> getUsersByCriteria(
      String username,
      SortUsersBy sortBy,
      SortOrder sortOrder,
      Instant referenceDatetime,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    var order = sortOrder == null ? DESC.getValue() : sortOrder.getValue();
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());

    var data =
        switch (sortBy) {
          case USERNAME -> jpaRepository.findByUsernameSortedByUsername(username, order, pageable);
          case LAST_CONNECTION ->
              jpaRepository.findByUsernameSortedByLastConnection(username, order, pageable);
          case JOIN_DATE -> jpaRepository.findByUsernameSortedByJoinedAt(username, order, pageable);
          case null -> jpaRepository.findByUsernameSortedByJoinedAt(username, order, pageable);
        };

    return new Page<>(pageFromOne, boundedPageSize, data).map(mapper::toModel);
  }

  @Override
  public Page<User> getPaginatedUsersByOrgIdAndInviteStatus(
      String orgId,
      OrganizationInviteStatus status,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());
    var data =
        jpaRepository.getAllUsersByOrgIdAndInviteStatus(orgId, status, pageable).stream()
            .map(mapper::toModel)
            .toList();
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  @Override
  public List<User> getAllUsersByOrgIdAndInviteStatus(
      String orgId, OrganizationInviteStatus status) {
    return jpaRepository.getAllUsersByOrgIdAndInviteStatus(orgId, status).stream()
        .map(mapper::toModel)
        .toList();
  }

  @Override
  public Page<UserWithLatestOrgInviteDTO> getUsersByUsernameWithLastOrgInvite(
      String orgId,
      String username,
      Instant referenceDatetime,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    Pageable pageable = PageRequest.of(pageFromOne.getValue() - 1, boundedPageSize.getValue());

    var data =
        jpaRepository
            .findAllUsersByUsernameWithLatestOrgInvite(orgId, username, referenceDatetime, pageable)
            .toList();
    return new Page<>(pageFromOne, boundedPageSize, data);
  }

  @Override
  @Transactional
  public void updateMainOrgId(String userId, String mainOrgId) {
    jpaRepository.updateMainOrgId(userId, mainOrgId);
  }

  @Override
  @Transactional
  public void updateStripeId(String userId, String stripeId) {
    jpaRepository.updateStripeId(userId, stripeId);
  }

  @Override
  @Transactional
  public void updateStatus(
      String id,
      UserStatus status,
      String statusReason,
      Instant statusUpdatedAt,
      Instant statusCheckedAt) {
    jpaRepository.updateStatus(id, status, statusReason, statusUpdatedAt, statusCheckedAt);
  }

  @Override
  @Transactional
  public void updateStatusCheckedAt(String id, Instant now) {
    jpaRepository.updateStatusCheckedAt(id, now);
  }

  @Override
  @Transactional
  public void updateLatestSubscriptionId(String userId, String subscriptionId) {
    jpaRepository.updateLatestSubscriptionId(userId, subscriptionId);
  }

  @Override
  @Transactional
  public void updateLastConnection(String userId, Instant now) {
    jpaRepository.updateLastConnection(userId, now);
  }

  @Override
  public long countAll() {
    return jpaRepository.count();
  }

  @Override
  public void archiveUser(String id, Instant archivedAt) {
    jpaRepository.archiveUser(id, archivedAt);
  }

  @Override
  public Optional<User> findById(String id, Instant referenceDatetime) {
    return jpaRepository.getUserDTOById(id, referenceDatetime).map(mapper::toModel);
  }

  @Override
  public UserStatisticsDTO getUserStatistics() {
    return jpaRepository.getUserStatisticsDTO();
  }
}
