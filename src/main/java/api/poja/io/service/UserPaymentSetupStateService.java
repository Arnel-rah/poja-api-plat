package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static java.time.Instant.now;

import api.poja.io.model.UserPaymentSetupStateDTO;
import api.poja.io.repository.UserPaymentSetupStateRepository;
import api.poja.io.repository.model.UserPaymentSetupState;
import api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserPaymentSetupStateService {
  private final UserPaymentSetupStateRepository repository;
  private final UserService userService;

  public List<UserPaymentSetupState> getSortedStatesByUserId(String userId) {
    Sort sortByTimestampDesc = Sort.by("timestamp").descending();
    return repository.findAllByUserId(userId, sortByTimestampDesc);
  }

  public void save(String userId, UserPaymentSetupStatusEnum status) {
    var user = userService.getUserById(userId);
    var stateMachine =
        new UserPaymentSetupStateDTO(user.getId(), getSortedStatesByUserId(user.getId()));
    try {
      var newState = toState(userId, status);
      stateMachine.transitionTo(newState);
    } catch (IllegalStateTransitionException e) {
      throw new RuntimeException(e);
    }
    var latestStateOpt = stateMachine.currentState();
    repository.save(latestStateOpt.get());
  }

  private static UserPaymentSetupState toState(String userId, UserPaymentSetupStatusEnum status) {
    return UserPaymentSetupState.builder()
        .timestamp(now())
        .userId(userId)
        .progressionStatus(status)
        .executionType(ASYNCHRONOUS)
        .build();
  }
}
