package api.poja.io.service.workflows.userState;

import static api.poja.io.endpoint.rest.model.ExecutionType.SYNCHRONOUS;
import static api.poja.io.model.UserStatus.ACTIVE;
import static api.poja.io.model.UserStatus.SUSPENDED;
import static api.poja.io.model.UserStatus.UNDER_MODIFICATION;
import static java.util.stream.Collectors.toCollection;

import api.poja.io.model.UserStatus;
import api.poja.io.repository.jpa.UserStateRepository;
import api.poja.io.repository.model.UserState;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserStateService {
  public static final String COMPUTING_USER_STATE_REASON = "computing user status";

  private final UserStateRepository repository;

  public List<UserState> findAllByUserId(String userId) {
    var sortByTimestampDesc = Sort.by("timestamp").descending();
    return repository.findAllByUserId(userId, sortByTimestampDesc);
  }

  public Optional<UserState> findLatestByUserId(String userId) {
    return repository.findFirstByUserIdOrderByTimestampDesc(userId);
  }

  private boolean isLatestStatus(String userId, UserStatus that) {
    return findLatestByUserId(userId).map(e -> e.getProgressionStatus().equals(that)).orElse(false);
  }

  public boolean isLastActive(String userId) {
    return isLatestStatus(userId, ACTIVE);
  }

  public boolean isLastSuspended(String userId) {
    return isLatestStatus(userId, SUSPENDED);
  }

  public boolean isTransitional(String userId) {
    return isLatestStatus(userId, UNDER_MODIFICATION);
  }

  public UserState save(String userId, UserStatus status, String description, Instant timestamp)
      throws IllegalStateTransitionException {
    var toSave = fromUser(userId, status, description, timestamp);
    return save(userId, toSave);
  }

  public UserState save(String userId, UserState newState) throws IllegalStateTransitionException {
    var states = findLatestByUserId(userId).stream().collect(toCollection(ArrayList::new));
    var sm = new UserStateMachine(states);

    var toSave = sm.transitionTo(newState);
    return repository.save(toSave);
  }

  private static UserState fromUser(
      String userId, UserStatus status, String description, Instant timestamp) {
    return UserState.builder()
        .userId(userId)
        .progressionStatus(status)
        .description(description)
        .timestamp(timestamp)
        .executionType(SYNCHRONOUS)
        .build();
  }
}
