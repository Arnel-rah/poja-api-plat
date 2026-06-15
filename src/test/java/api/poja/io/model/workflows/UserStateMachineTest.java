package api.poja.io.model.workflows;

import static api.poja.io.endpoint.rest.model.ExecutionType.SYNCHRONOUS;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsIllegalStateTransitionException;
import static api.poja.io.model.UserStatus.ACTIVE;
import static api.poja.io.model.UserStatus.SUSPENDED;
import static api.poja.io.model.UserStatus.UNDER_MODIFICATION;
import static api.poja.io.model.UserStatus.UNKNOWN;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.model.UserStatus;
import api.poja.io.repository.model.UserState;
import api.poja.io.repository.model.workflows.StateMachine;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import api.poja.io.service.workflows.userState.UserStateMachine;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class UserStateMachineTest {
  private static UserState state(UserStatus state, Instant ts) {
    return UserState.builder()
        .id(randomUUID().toString())
        .timestamp(ts)
        .executionType(SYNCHRONOUS)
        .progressionStatus(state)
        .build();
  }

  private static StateMachine<UserStatus, UserState> userStateMachine() {
    return new UserStateMachine(new ArrayList<>());
  }

  @Test
  void transition_to_older_state_ko() throws IllegalStateTransitionException {
    var sm = userStateMachine();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    sm.transitionTo(state(ACTIVE, ts));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition: target state 'UNKNOWN' (timestamp=2025-08-23T08:00:00Z) is older than"
            + " current state 'ACTIVE' (timestamp=2025-08-23T09:00:00Z)",
        () -> sm.transitionTo(state(UNKNOWN, ts.minus(1, HOURS))));
  }

  @Test
  void transition_to_same_state_ko() throws IllegalStateTransitionException {
    var sm = userStateMachine();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    sm.transitionTo(state(UNDER_MODIFICATION, ts.plus(1, MINUTES)));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=UNDER_MODIFICATION to=UNDER_MODIFICATION",
        () -> sm.transitionTo(state(UNDER_MODIFICATION, ts.plus(2, MINUTES))));
  }

  @Test
  void test_describe() throws IllegalStateTransitionException {
    var sm = userStateMachine();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    sm.transitionTo(state(UNKNOWN, ts));
    sm.transitionTo(state(ACTIVE, ts.plus(1, MINUTES)));
    sm.transitionTo(state(UNDER_MODIFICATION, ts.plus(2, MINUTES)));
    sm.transitionTo(state(ACTIVE, ts.plus(3, MINUTES)));
    sm.transitionTo(state(SUSPENDED, ts.plus(4, MINUTES)));

    var expected = "[SUSPENDED] -> [ACTIVE] -> [UNDER_MODIFICATION] -> [ACTIVE] -> [UNKNOWN]";
    assertEquals(expected, sm.describe());
  }
}
