package api.poja.io.service.workflows.appImportState;

import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.BUILD_TOOL_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.CODE_FORMATTING_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GENERATED_CODE_INTEGRATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POJA_CONF_GENERATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_IN_PROGRESS;
import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;

import api.poja.io.repository.model.ApplicationImportState;
import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.TransitionResult;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApplicationImportStateMachine
    extends AbstractStateMachine<ApplicationImportStateStatus, ApplicationImportState> {
  private List<ApplicationImportState> states;

  @Override
  protected List<ApplicationImportState> internalStates() {
    return states;
  }

  @Override
  protected void addState(ApplicationImportState state) {
    this.states.add(state);
  }

  @Override
  public TransitionResult<ApplicationImportStateStatus, ApplicationImportState> canTransitionTo(
      ApplicationImportState from, ApplicationImportState to) {
    var fromStatus = from.getProgressionStatus();
    var toStatus = to.getProgressionStatus();

    if (fromStatus.equals(toStatus)) {
      return ko();
    }

    return switch (fromStatus) {
      case APPLICATION_LANGUAGE_VERIFICATION_IN_PROGRESS -> {
        switch (toStatus) {
          case APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL,
              APPLICATION_LANGUAGE_VERIFICATION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case APPLICATION_LANGUAGE_VERIFICATION_SUCCESSFUL -> {
        if (BUILD_TOOL_VERIFICATION_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case BUILD_TOOL_VERIFICATION_IN_PROGRESS -> {
        switch (toStatus) {
          case BUILD_TOOL_VERIFICATION_SUCCESSFUL, BUILD_TOOL_VERIFICATION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case BUILD_TOOL_VERIFICATION_SUCCESSFUL -> {
        if (PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS -> {
        switch (toStatus) {
          case PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL,
              PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL -> {
        switch (toStatus) {
          case CONVERSION_TO_GRADLE_IN_PROGRESS,
              GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS,
              POJA_CONF_GENERATION_IN_PROGRESS -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case CONVERSION_TO_GRADLE_IN_PROGRESS -> {
        switch (toStatus) {
          case CONVERSION_TO_GRADLE_SUCCESSFUL, CONVERSION_TO_GRADLE_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case CONVERSION_TO_GRADLE_SUCCESSFUL -> {
        if (GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_IN_PROGRESS -> {
        switch (toStatus) {
          case GRADLE_BUILD_FILE_CONFLICTS_RESOLVED,
              GRADLE_BUILD_FILE_CONFLICTS_RESOLUTION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case GRADLE_BUILD_FILE_CONFLICTS_RESOLVED -> {
        if (POJA_CONF_GENERATION_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case POJA_CONF_GENERATION_IN_PROGRESS -> {
        switch (toStatus) {
          case POJA_CONF_GENERATED, POJA_CONF_GENERATION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case POJA_CONF_GENERATED -> {
        switch (toStatus) {
          case CODE_GENERATION_IN_PROGRESS, APPLICATION_SET_UP_IN_PROGRESS -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case CODE_GENERATION_IN_PROGRESS -> {
        if (GENERATED_CODE_INTEGRATION_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case GENERATED_CODE_INTEGRATION_IN_PROGRESS -> {
        switch (toStatus) {
          case GENERATED_CODE_INTEGRATED, GENERATED_CODE_INTEGRATION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case GENERATED_CODE_INTEGRATED -> {
        if (CODE_FORMATTING_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case CODE_FORMATTING_IN_PROGRESS -> {
        switch (toStatus) {
          case CODE_FORMATTING_SUCCESSFUL, CODE_FORMATTING_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case CODE_FORMATTING_SUCCESSFUL -> {
        if (POST_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case POST_TRANSFORMATION_TEST_RUN_VERIFICATION_IN_PROGRESS -> {
        switch (toStatus) {
          case POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL,
              POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL -> {
        switch (toStatus) {
          case TEST_PING_ENDPOINT_IN_PROGRESS, APPLICATION_SET_UP_IN_PROGRESS -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case TEST_PING_ENDPOINT_IN_PROGRESS -> {
        switch (toStatus) {
          case TEST_PING_ENDPOINT_SUCCESSFUL, TEST_PING_ENDPOINT_FAILED -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      case TEST_PING_ENDPOINT_SUCCESSFUL -> {
        if (APPLICATION_SET_UP_IN_PROGRESS.equals(toStatus)) {
          yield ok(to);
        }
        yield ko();
      }
      case APPLICATION_SET_UP_IN_PROGRESS -> {
        switch (toStatus) {
          case APPLICATION_SET_UP_FAILED, APPLICATION_SET_UP_SUCCESSFUL -> {
            yield ok(to);
          }
          default -> {
            yield ko();
          }
        }
      }
      default -> ko();
    };
  }
}
