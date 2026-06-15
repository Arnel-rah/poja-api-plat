package api.poja.io.model.workflows;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsIllegalStateTransitionException;
import static api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum.PAYMENT_SETUP_COMPLETED;
import static api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum.PAYMENT_SETUP_FAILED;
import static api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum.PAYMENT_SETUP_IN_PROGRESS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.model.UserPaymentSetupStateDTO;
import api.poja.io.repository.model.UserPaymentSetupState;
import api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum;
import api.poja.io.repository.model.workflows.StateMachine;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UserPaymentStateMachineTest {
  private static final Instant BASE_TIMESTAMP = Instant.parse("2025-08-23T09:00:00Z");

  private static UserPaymentSetupState state(UserPaymentSetupStatusEnum state, Instant timestamp) {
    return UserPaymentSetupState.builder()
        .id(randomUUID().toString())
        .timestamp(timestamp)
        .executionType(ASYNCHRONOUS)
        .progressionStatus(state)
        .build();
  }

  private static StateMachine<UserPaymentSetupStatusEnum, UserPaymentSetupState>
      userPaymentSetupStateMachine() {
    return new UserPaymentSetupStateDTO("", new ArrayList<>());
  }

  @Test
  void transition_to_older_state_ko() throws IllegalStateTransitionException {
    StateMachine<UserPaymentSetupStatusEnum, UserPaymentSetupState> stateMachine =
        userPaymentSetupStateMachine();

    stateMachine.transitionTo(state(PAYMENT_SETUP_IN_PROGRESS, BASE_TIMESTAMP));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition: target state 'PAYMENT_SETUP_FAILED' (timestamp=2025-08-23T08:00:00Z)"
            + " is older than current state 'PAYMENT_SETUP_IN_PROGRESS'"
            + " (timestamp=2025-08-23T09:00:00Z)",
        () ->
            stateMachine.transitionTo(state(PAYMENT_SETUP_FAILED, BASE_TIMESTAMP.minus(1, HOURS))));
  }

  @Test
  void transition_from_final_state_ko() throws IllegalStateTransitionException {
    StateMachine<UserPaymentSetupStatusEnum, UserPaymentSetupState> stateMachine =
        userPaymentSetupStateMachine();

    stateMachine.transitionTo(state(PAYMENT_SETUP_COMPLETED, BASE_TIMESTAMP));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=PAYMENT_SETUP_COMPLETED to=PAYMENT_SETUP_IN_PROGRESS",
        () ->
            stateMachine.transitionTo(
                state(PAYMENT_SETUP_IN_PROGRESS, BASE_TIMESTAMP.plus(2, MINUTES))));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=PAYMENT_SETUP_COMPLETED to=PAYMENT_SETUP_FAILED",
        () ->
            stateMachine.transitionTo(
                state(PAYMENT_SETUP_FAILED, BASE_TIMESTAMP.plus(2, MINUTES))));
  }

  @Test
  void transition_ok() throws IllegalStateTransitionException {
    StateMachine<UserPaymentSetupStatusEnum, UserPaymentSetupState> stateMachine =
        userPaymentSetupStateMachine();

    stateMachine.transitionTo(state(PAYMENT_SETUP_IN_PROGRESS, BASE_TIMESTAMP));
    stateMachine.transitionTo(state(PAYMENT_SETUP_COMPLETED, BASE_TIMESTAMP.plus(2, MINUTES)));
    var actual = stateMachine.states().stream().map(UserPaymentStateMachineTest::ignoreId).toList();

    assertEquals(validStates(), actual);
  }

  private static List<UserPaymentSetupState> validStates() {
    return List.of(
        UserPaymentSetupState.builder()
            .timestamp(BASE_TIMESTAMP.plus(2, MINUTES))
            .executionType(ASYNCHRONOUS)
            .progressionStatus(PAYMENT_SETUP_COMPLETED)
            .build(),
        UserPaymentSetupState.builder()
            .timestamp(BASE_TIMESTAMP)
            .executionType(ASYNCHRONOUS)
            .progressionStatus(PAYMENT_SETUP_IN_PROGRESS)
            .build());
  }

  private static UserPaymentSetupState ignoreId(UserPaymentSetupState state) {
    return state.toBuilder().id(null).build();
  }
}
