package api.poja.io.model.workflows;

import static api.poja.io.endpoint.rest.model.ExecutionType.SYNCHRONOUS;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsIllegalStateTransitionException;
import static api.poja.io.model.workflows.ExampleStateMachineImpl.UserStateEnum.ACTIVE;
import static api.poja.io.model.workflows.ExampleStateMachineImpl.UserStateEnum.DELETED;
import static api.poja.io.model.workflows.ExampleStateMachineImpl.UserStateEnum.INACTIVE;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.repository.model.workflows.StateMachine;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/**
 * Each implementation of {@link api.poja.io.repository.model.workflows.AbstractStateMachine} should
 * have its own test class, and tests {@code transition_ok()} + {@code test_describe()} (implemented
 * or the default one), {@code transition_to_older_state_ko()} transition_from_final_state_ko is
 * very specific, a state machine impl doesn't necessarily have final state, transition_ko_...
 * depending on the actual impl is expected
 */
class StateMachineTest {
  private static ExampleStateMachineImpl.UserState state(
      ExampleStateMachineImpl.UserStateEnum state, Instant timestamp) {
    return ExampleStateMachineImpl.UserState.builder()
        .id(randomUUID().toString())
        .timestamp(timestamp)
        .executionType(SYNCHRONOUS)
        .progressionStatus(state)
        .build();
  }

  private static StateMachine<
          ExampleStateMachineImpl.UserStateEnum, ExampleStateMachineImpl.UserState>
      exampleStateMachineImpl() {
    return new ExampleStateMachineImpl(new ArrayList<>());
  }

  @Test
  void transition_to_older_state_ko() throws IllegalStateTransitionException {
    StateMachine<ExampleStateMachineImpl.UserStateEnum, ExampleStateMachineImpl.UserState>
        stateMachine = exampleStateMachineImpl();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    stateMachine.transitionTo(state(ACTIVE, ts));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition: target state 'INACTIVE' (timestamp=2025-08-23T08:00:00Z) is older than"
            + " current state 'ACTIVE' (timestamp=2025-08-23T09:00:00Z)",
        () -> stateMachine.transitionTo(state(INACTIVE, ts.minus(1, HOURS))));
  }

  @Test
  void transition_from_final_state_ko() throws IllegalStateTransitionException {
    StateMachine<ExampleStateMachineImpl.UserStateEnum, ExampleStateMachineImpl.UserState>
        stateMachine = exampleStateMachineImpl();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    stateMachine.transitionTo(state(DELETED, ts.plus(1, MINUTES)));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=DELETED to=ACTIVE",
        () -> stateMachine.transitionTo(state(ACTIVE, ts.plus(2, MINUTES))));
  }

  @Test
  void test_describe() throws IllegalStateTransitionException {
    StateMachine<ExampleStateMachineImpl.UserStateEnum, ExampleStateMachineImpl.UserState>
        stateMachine = exampleStateMachineImpl();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    stateMachine.transitionTo(state(INACTIVE, ts));
    stateMachine.transitionTo(state(ACTIVE, ts.plus(1, MINUTES)));
    stateMachine.transitionTo(state(ACTIVE, ts.plus(2, MINUTES)));
    stateMachine.transitionTo(state(INACTIVE, ts.plus(3, MINUTES)));
    stateMachine.transitionTo(state(ACTIVE, ts.plus(4, MINUTES)));

    var expected = "[ACTIVE] -> [INACTIVE] -> [ACTIVE] -> [ACTIVE] -> [INACTIVE]";
    assertEquals(expected, stateMachine.describe());
  }
}
