package api.poja.io.service;

import static api.poja.io.endpoint.event.model.UserStatusUpdateRequested.StatusAlteration.ACTIVATE;
import static api.poja.io.endpoint.event.model.UserStatusUpdateRequested.StatusAlteration.SUSPEND;
import static api.poja.io.model.UserStatus.ACTIVE;
import static api.poja.io.model.UserStatus.SUSPENDED;
import static api.poja.io.model.UserStatus.UNDER_MODIFICATION;
import static api.poja.io.service.workflows.userState.UserStateService.COMPUTING_USER_STATE_REASON;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.PojaEvent;
import api.poja.io.endpoint.event.model.UserArchivalRequested;
import api.poja.io.endpoint.event.model.UserCreated;
import api.poja.io.endpoint.event.model.UserMainOrganizationSetupRequested;
import api.poja.io.endpoint.event.model.UserPaymentSetupRequested;
import api.poja.io.endpoint.event.model.UserStatusUpdateRequested;
import api.poja.io.endpoint.event.model.UserSubscriptionRequested;
import api.poja.io.endpoint.rest.model.CreateUser;
import api.poja.io.endpoint.rest.model.SortOrder;
import api.poja.io.endpoint.rest.model.SortUsersBy;
import api.poja.io.endpoint.rest.model.UserStatistics;
import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.User;
import api.poja.io.model.UserStatus;
import api.poja.io.model.UserWithLatestOrgInviteDTO;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.ForbiddenException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.page.Page;
import api.poja.io.repository.UserRepository;
import api.poja.io.repository.model.UserState;
import api.poja.io.repository.model.UserSuspension;
import api.poja.io.repository.model.mapper.UserMapper;
import api.poja.io.service.subscription.SubscriptionConf;
import api.poja.io.service.symjaService.SymjaService;
import api.poja.io.service.validator.EmailValidator;
import api.poja.io.service.validator.UserSuspensionValidator;
import api.poja.io.service.validator.UsersThresholdValidator;
import api.poja.io.service.workflows.userState.UserStateService;
import api.poja.io.sys.platform.PlatformConf;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHMyself;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

  public static final Duration LAST_CONNECTION_UPDATE_THRESHOLD = Duration.ofMinutes(10);

  private final EmailValidator emailValidator;
  private final UserRepository repository;
  private final GithubComponent githubComponent;
  private final UserMapper mapper;
  private final EventProducer<PojaEvent> eventProducer;
  private final UsersThresholdValidator thresholdValidator;
  private final UserSuspensionValidator userSuspensionValidator;
  private final UserSuspensionService userSuspensionService;
  private final UserStateService stateService;
  private final PlatformConf platformConf;

  public List<User> createUsers(List<CreateUser> toCreate) {
    thresholdValidator.accept(toCreate.size());
    List<User> toSave = toCreate.stream().map(this::fromCreateUser).toList();
    var saved = repository.saveAll(toSave);
    eventProducer.accept(
        saved.stream()
            .peek(u -> updateUserStatus(u.getId(), ACTIVE, null, u.getStatusUpdatedAt()))
            .flatMap(
                u ->
                    Stream.of(
                            toUserOrgEvent(u),
                            toUserPaymentSetupEvent(u),
                            toUserCreated(u),
                            platformConf.isIdp() ? toUserSubscriptionRequested(u) : null)
                        .filter(Objects::nonNull))
            .toList());
    return saved;
  }

  private static UserCreated toUserCreated(User user) {
    return new UserCreated(user.getId());
  }

  private static UserMainOrganizationSetupRequested toUserOrgEvent(User user) {
    return new UserMainOrganizationSetupRequested(user);
  }

  private static UserSubscriptionRequested toUserSubscriptionRequested(User user) {
    return new UserSubscriptionRequested(user.getId(), now().truncatedTo(MILLIS));
  }

  private static UserPaymentSetupRequested toUserPaymentSetupEvent(User user) {
    return new UserPaymentSetupRequested(user);
  }

  public List<User> findAllToBill(YearMonth yearMonth) {
    return repository.findAllToBillFor(yearMonth);
  }

  public boolean shouldComputeCost(String userId, YearMonth yearMonth) {
    return repository.shouldComputeCost(userId, yearMonth);
  }

  public User getUserById(String userId) {
    return findById(userId)
        .orElseThrow(
            () -> new NotFoundException("The user identified by id " + userId + " is not found"));
  }

  public List<User> findAllToComputeBillingFor(Instant computeDatetime, LocalDate dateIntervalEnd) {
    return repository.findAllToComputeBilling(computeDatetime, dateIntervalEnd);
  }

  public Optional<User> findById(String userId) {
    return repository.findById(userId, now());
  }

  private User fromCreateUser(CreateUser createUser) {
    emailValidator.accept(createUser.getEmail());

    var githubUser = getUserByToken(createUser.getToken());
    var user = mapper.toModel(createUser, githubUser);
    if (repository.existsByEmail(user.getEmail()))
      throw new BadRequestException("An account with the same email already exists");
    if (repository.existsByGithubId(user.getGithubId()))
      throw new BadRequestException("An account with the same github id already exists");
    return user;
  }

  private GHMyself getUserByToken(String token) {
    return githubComponent
        .getCurrentUserByToken(token)
        .orElseThrow(() -> new ForbiddenException("Invalid token"));
  }

  public User findByGithubUserId(String githubUserId) {
    return repository
        .findByGithubId(githubUserId, now())
        .orElseThrow(
            () ->
                new NotFoundException(
                    "The user identified by the github id " + githubUserId + " is not found"));
  }

  public void updateMainOrgId(String userId, String mainOrgId) {
    repository.updateMainOrgId(userId, mainOrgId);
  }

  public void updateStripeId(String userId, String stripeId) {
    repository.updateStripeId(userId, stripeId);
  }

  public Page<User> getUsers(
      String username,
      SortUsersBy sortBy,
      SortOrder sortOrder,
      PageFromOne pageFromOne,
      BoundedPageSize boundedPageSize) {
    return repository.getUsersByCriteria(
        username, sortBy, sortOrder, now(), pageFromOne, boundedPageSize);
  }

  public Page<UserWithLatestOrgInviteDTO> getUsers(
      String orgId, String username, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    return repository.getUsersByUsernameWithLastOrgInvite(
        orgId, username, now(), pageFromOne, boundedPageSize);
  }

  public void updateLastConnection(User user, Instant now) {
    var lastConnection = user.getLastConnection();
    if (lastConnection == null) {
      repository.updateLastConnection(user.getId(), now);
      return;
    }
    if (now.isBefore(lastConnection)) {
      log.error("current time is before last_connection");
      return;
    }
    var isStale =
        Duration.between(lastConnection, now).compareTo(LAST_CONNECTION_UPDATE_THRESHOLD) > 0;
    if (isStale) {
      repository.updateLastConnection(user.getId(), now);
    }
  }

  @SneakyThrows
  public void updateUserStatus(String userId, UserStatus status, String reason, Instant at) {
    var saved = stateService.save(userId, status, reason, at);
    saveOrUpdateStatus(userId, saved);
    saveStateIfSuspended(userId, saved);
  }

  private void saveOrUpdateStatus(String userId, UserState state) {
    var status = state.getProgressionStatus();
    if (UNDER_MODIFICATION.equals(status)) {
      // do nothing
      return;
    }
    var user = getUserById(userId);
    if (status.equals(user.getStatus())) {
      repository.updateStatusCheckedAt(user.getId(), state.getTimestamp());
      return;
    }
    repository.updateStatus(
        user.getId(), status, state.getDescription(), state.getTimestamp(), state.getTimestamp());
  }

  private void saveStateIfSuspended(String userId, UserState state) {
    if (!SUSPENDED.equals(state.getProgressionStatus())) {
      return;
    }
    userSuspensionService.save(
        UserSuspension.builder()
            .userId(userId)
            .suspensionReason(state.getDescription())
            .suspendedAt(state.getTimestamp())
            .build());
  }

  public void updateUserLatestSubscriptionId(String userId, String subscriptionId) {
    repository.updateLatestSubscriptionId(userId, subscriptionId);
  }

  private final SymjaService symjaService;
  private final SubscriptionConf subscriptionConf;

  public UserStatistics getUserStats() {
    var userStatistics = repository.getUserStatistics();
    return new UserStatistics()
        .maxUsersNb(
            symjaService
                .computeMaxUsersGivenPremium(subscriptionConf.maxPremiumSubscribersNb())
                .longValue())
        .archivedUsersNb(userStatistics.archivedCount())
        .suspendedUsersNb(userStatistics.suspendedCount())
        .usersCount(userStatistics.totalCount());
  }

  @Transactional
  public User archiveUser(String userId) {
    var user = getUserById(userId);
    if (user.isArchived()) {
      throw new BadRequestException("user is already archived");
    }

    if (stateService.isTransitional(userId)) {
      throw new BadRequestException("user status is still under modification.");
    }

    repository.archiveUser(userId, now());
    eventProducer.accept(List.of(new UserArchivalRequested(userId, now())));
    return getUserById(userId);
  }

  @SneakyThrows
  public User updateUserStatusAsync(String userId, UserStatus status, String statusReason) {
    var user = getUserById(userId);

    if (user.isArchived()) {
      throw new BadRequestException("user is already archived");
    }
    if (status.equals(user.getStatus())) {
      return user;
    }
    if (stateService.isTransitional(userId)) {
      throw new BadRequestException("user status is still under modification.");
    }
    if (SUSPENDED.equals(status)) {
      userSuspensionValidator.accept(userId, statusReason);
    }

    updateUserStatus(userId, UNDER_MODIFICATION, COMPUTING_USER_STATE_REASON, now());
    eventProducer.accept(
        List.of(new UserStatusUpdateRequested(userId, getStatus(status), statusReason, now())));
    return user;
  }

  private static UserStatusUpdateRequested.StatusAlteration getStatus(UserStatus status) {
    return switch (status) {
      case ACTIVE -> ACTIVATE;
      case SUSPENDED -> SUSPEND;
      case UNKNOWN, UNDER_MODIFICATION -> throw new IllegalArgumentException();
    };
  }
}
