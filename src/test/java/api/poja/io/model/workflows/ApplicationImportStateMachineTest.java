package api.poja.io.model.workflows;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsIllegalStateTransitionException;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CONVERSION_TO_GRADLE_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CONVERSION_TO_GRADLE_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_FAILED;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.repository.model.ApplicationImportState;
import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import api.poja.io.repository.model.workflows.StateMachine;
import api.poja.io.service.workflows.appImportState.ApplicationImportStateMachine;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApplicationImportStateMachineTest {
  private static final Instant BASE_TIMESTAMP = Instant.parse("2025-08-23T09:00:00Z");

  private static StateMachine<ApplicationImportStateStatus, ApplicationImportState>
      applicationImportState() {
    return new ApplicationImportStateMachine(new ArrayList<>());
  }

  private static ApplicationImportState state(
      ApplicationImportStateStatus state, Instant timestamp) {
    return ApplicationImportState.builder()
        .id(randomUUID().toString())
        .timestamp(timestamp)
        .executionType(ASYNCHRONOUS)
        .progressionStatus(state)
        .build();
  }

  @Test
  void transition_to_older_state_ko() {
    var sm = applicationImportState();

    assertDoesNotThrow(
        () ->
            sm.transitionTo(state(APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS, BASE_TIMESTAMP)));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition: target state 'APPLICATION_LANGUAGE_VERIFICATION_FAILED'"
            + " (timestamp=2025-08-23T08:00:00Z) is older than current state"
            + " 'APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS' (timestamp=2025-08-23T09:00:00Z)",
        () ->
            sm.transitionTo(
                state(APPLICATION_LANGUAGE_VERIFICATION_FAILED, BASE_TIMESTAMP.minus(1, HOURS))));
  }

  @Test
  void transition_to_same_state_ko() {
    var sm = applicationImportState();

    sm.transitionTo(
        state(APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS, BASE_TIMESTAMP.plus(1, MINUTES)));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS"
            + " to=APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS",
        () ->
            sm.transitionTo(
                state(
                    APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS,
                    BASE_TIMESTAMP.plus(2, MINUTES))));
  }

  @Test
  void backward_transition_ko() {
    var sm = applicationImportState();

    sm.transitionTo(
        state(APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL, BASE_TIMESTAMP.plus(1, MINUTES)));

    assertThrowsIllegalStateTransitionException(
        "Illegal transition status from=APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL"
            + " to=APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS",
        () ->
            sm.transitionTo(
                state(
                    APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS,
                    BASE_TIMESTAMP.plus(2, MINUTES))));
  }

  @Test
  void transition_from_final_state_ko() {
    var finalStates =
        List.of(
            APPLICATION_LANGUAGE_VERIFICATION_FAILED,
            BUILD_TOOL_VERIFICATION_FAILED,
            PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED,
            CONVERSION_TO_GRADLE_FAILED,
            GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED,
            POJA_CONF_GENERATION_FAILED,
            GENERATED_CODE_INTEGRATION_FAILED,
            CODE_FORMATTING_FAILED,
            POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED,
            TEST_PING_ENDPOINT_FAILED,
            APPLICATION_SET_UP_FAILED,
            APPLICATION_SET_UP_SUCCESSFUL);

    for (var from : finalStates) {
      var sm = applicationImportState();
      sm.transitionTo(state(from, BASE_TIMESTAMP));

      for (var to : ApplicationImportStateStatus.values()) {
        assertThrowsIllegalStateTransitionException(
            "Illegal transition status from=" + from + " to=" + to,
            () -> sm.transitionTo(state(to, BASE_TIMESTAMP.plus(1, MINUTES))));
      }
    }
  }

  @Test
  void test_describe() {
    var sm = applicationImportState();

    sm.transitionTo(state(APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS, BASE_TIMESTAMP));
    sm.transitionTo(
        state(APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL, BASE_TIMESTAMP.plus(1, MINUTES)));
    sm.transitionTo(state(BUILD_TOOL_VERIFICATION_IN_PROGRESS, BASE_TIMESTAMP.plus(2, MINUTES)));
    sm.transitionTo(state(BUILD_TOOL_VERIFICATION_SUCCESSFUL, BASE_TIMESTAMP.plus(3, MINUTES)));
    sm.transitionTo(
        state(
            PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS, BASE_TIMESTAMP.plus(4, MINUTES)));
    sm.transitionTo(
        state(
            PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL, BASE_TIMESTAMP.plus(5, MINUTES)));
    sm.transitionTo(state(CONVERSION_TO_GRADLE_IN_PROGRESS, BASE_TIMESTAMP.plus(6, MINUTES)));
    sm.transitionTo(state(CONVERSION_TO_GRADLE_FAILED, BASE_TIMESTAMP.plus(7, MINUTES)));

    var expected =
        "[CONVERSION_TO_GRADLE_FAILED] -> [CONVERSION_TO_GRADLE_IN_PROGRESS] ->"
            + " [PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL] ->"
            + " [PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS] ->"
            + " [BUILD_TOOL_VERIFICATION_SUCCESSFUL] -> [BUILD_TOOL_VERIFICATION_IN_PROGRESS] ->"
            + " [APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL] ->"
            + " [APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS]";
    assertEquals(expected, sm.describe());
  }
}
