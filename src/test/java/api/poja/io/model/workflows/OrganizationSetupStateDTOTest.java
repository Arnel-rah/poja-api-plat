package api.poja.io.model.workflows;

import static api.poja.io.endpoint.rest.model.ExecutionType.SYNCHRONOUS;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsIllegalStateTransitionException;
import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_COMPLETED;
import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_FAILED;
import static api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum.ORGANIZATION_SETUP_IN_PROGRESS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;

import api.poja.io.model.OrganizationSetupStateDTO;
import api.poja.io.repository.model.OrganizationSetupState;
import api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum;
import api.poja.io.repository.model.workflows.StateMachine;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class OrganizationSetupStateDTOTest {
  private static OrganizationSetupState state(
      OrganizationSetupStateStatusEnum status, Instant timestamp) {
    return OrganizationSetupState.builder()
        .id(randomUUID().toString())
        .timestamp(timestamp)
        .executionType(SYNCHRONOUS)
        .progressionStatus(status)
        .build();
  }

  private static StateMachine<OrganizationSetupStateStatusEnum, OrganizationSetupState>
      organizationSetupStateMachine() {
    return new OrganizationSetupStateDTO("", new ArrayList<>());
  }

  @Test
  void transition_to_older_state_ko() throws IllegalStateTransitionException {
    StateMachine<OrganizationSetupStateStatusEnum, OrganizationSetupState> stateMachine =
        organizationSetupStateMachine();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    stateMachine.transitionTo(state(ORGANIZATION_SETUP_IN_PROGRESS, ts));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition: target state 'ORGANIZATION_SETUP_FAILED'"
            + " (timestamp=2025-08-23T08:00:00Z) is older than current state"
            + " 'ORGANIZATION_SETUP_IN_PROGRESS' (timestamp=2025-08-23T09:00:00Z)",
        () -> stateMachine.transitionTo(state(ORGANIZATION_SETUP_FAILED, ts.minus(1, HOURS))));
  }

  @Test
  void transition_from_final_state_ko() throws IllegalStateTransitionException {
    StateMachine<OrganizationSetupStateStatusEnum, OrganizationSetupState> stateMachine =
        organizationSetupStateMachine();
    var ts = Instant.parse("2025-08-23T09:00:00Z");

    stateMachine.transitionTo(state(ORGANIZATION_SETUP_COMPLETED, ts));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=ORGANIZATION_SETUP_COMPLETED"
            + " to=ORGANIZATION_SETUP_IN_PROGRESS",
        () ->
            stateMachine.transitionTo(state(ORGANIZATION_SETUP_IN_PROGRESS, ts.plus(2, MINUTES))));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=ORGANIZATION_SETUP_COMPLETED to=ORGANIZATION_SETUP_FAILED",
        () -> stateMachine.transitionTo(state(ORGANIZATION_SETUP_FAILED, ts.plus(2, MINUTES))));
  }
}
