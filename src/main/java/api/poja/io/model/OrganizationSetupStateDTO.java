package api.poja.io.model;

import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;

import api.poja.io.repository.model.OrganizationSetupState;
import api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum;
import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.TransitionResult;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OrganizationSetupStateDTO
    extends AbstractStateMachine<OrganizationSetupStateStatusEnum, OrganizationSetupState> {
  private final String orgId;
  private final List<OrganizationSetupState> states;

  @Override
  protected List<OrganizationSetupState> internalStates() {
    return states;
  }

  @Override
  protected void addState(OrganizationSetupState state) {
    states.add(state);
  }

  @Override
  public TransitionResult<OrganizationSetupStateStatusEnum, OrganizationSetupState> canTransitionTo(
      OrganizationSetupState from, OrganizationSetupState to) {
    var currentStatus = from.getProgressionStatus();
    var targetStatus = to.getProgressionStatus();

    return switch (currentStatus) {
      case ORGANIZATION_SETUP_IN_PROGRESS ->
          switch (targetStatus) {
            case ORGANIZATION_SETUP_COMPLETED, ORGANIZATION_SETUP_FAILED -> ok(to);
            default -> ko();
          };
      default -> ko();
    };
  }
}
