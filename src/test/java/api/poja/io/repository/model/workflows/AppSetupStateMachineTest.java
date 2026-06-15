package api.poja.io.repository.model.workflows;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsIllegalStateTransitionException;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_SUCCESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.REPO_CREATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.REPO_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.REPO_CREATION_SUCCESS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import api.poja.io.model.AppSetupStateMachine;
import api.poja.io.repository.model.AppSetupState;
import api.poja.io.repository.model.enums.AppSetupStateEnum;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class AppSetupStateMachineTest {
  @Test
  void transition_from_final_state_ko() throws IllegalStateTransitionException {
    var stateMachine = deployAppStateMachine();
    var time = Instant.parse("2025-09-09T09:00:00Z");

    stateMachine.transitionTo(state(ENV_CREATION_FAILED, time));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=ENV_CREATION_FAILED to=REPO_CREATION_FAILED",
        () -> stateMachine.transitionTo(state(REPO_CREATION_FAILED, time.plusSeconds(60))));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=ENV_CREATION_FAILED" + " to=REPO_CREATION_SUCCESS",
        () -> stateMachine.transitionTo(state(REPO_CREATION_SUCCESS, time.plusSeconds(60))));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=ENV_CREATION_FAILED"
            + " to=ENV_DEPLOYMENT_INITIATION_FAILED",
        () ->
            stateMachine.transitionTo(
                state(ENV_DEPLOYMENT_INITIATION_FAILED, time.plusSeconds(60))));
  }

  @Test
  void valid_state_transitions_ok() {
    var stateMachine = deployAppStateMachine();
    var time = Instant.parse("2025-09-09T09:00:00Z");
    var status =
        List.of(
            ENV_CREATION_IN_PROGRESS,
            ENV_CREATION_SUCCESS,
            REPO_CREATION_IN_PROGRESS,
            REPO_CREATION_SUCCESS,
            ENV_DEPLOYMENT_INITIATION_IN_PROGRESS,
            ENV_DEPLOYMENT_INITIATED);
    IntStream.range(0, status.size())
        .forEach(
            i -> assert_can_transition_to(stateMachine, status.get(i), time.plusSeconds(i * 60L)));
  }

  private static StateMachine<AppSetupStateEnum, AppSetupState> deployAppStateMachine() {
    return new AppSetupStateMachine(new ArrayList<>());
  }

  private void assert_can_transition_to(
      StateMachine<AppSetupStateEnum, AppSetupState> stateMachine,
      AppSetupStateEnum to,
      Instant time) {
    var targetState = state(to, time);
    assertDoesNotThrow(() -> stateMachine.transitionTo(targetState));
  }

  private static AppSetupState state(AppSetupStateEnum status, Instant timetamp) {
    return AppSetupState.builder()
        .id(randomUUID().toString())
        .timestamp(timetamp)
        .executionType(ASYNCHRONOUS)
        .progressionStatus(status)
        .build();
  }
}
