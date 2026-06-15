package api.poja.io.model;

import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;

import api.poja.io.repository.model.AppSetupState;
import api.poja.io.repository.model.enums.AppSetupStateEnum;
import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.TransitionResult;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AppSetupStateMachine extends AbstractStateMachine<AppSetupStateEnum, AppSetupState> {
  private final List<AppSetupState> states;

  @Override
  protected List<AppSetupState> internalStates() {
    return states;
  }

  @Override
  protected void addState(AppSetupState state) {
    states.add(state);
  }

  @Override
  public TransitionResult<AppSetupStateEnum, AppSetupState> canTransitionTo(
      AppSetupState from, AppSetupState to) {
    if (from.getProgressionStatus().isFinal()) {
      return ko();
    }
    if (from.getProgressionStatus().equals(to.getProgressionStatus())) {
      return ko();
    }
    return ok(to);
  }
}
