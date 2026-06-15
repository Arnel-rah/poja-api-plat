package api.poja.io.service.workflows.userState;

import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;

import api.poja.io.model.UserStatus;
import api.poja.io.repository.model.UserState;
import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.TransitionResult;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserStateMachine extends AbstractStateMachine<UserStatus, UserState> {
  private final List<UserState> states;

  @Override
  protected List<UserState> internalStates() {
    return states;
  }

  @Override
  protected void addState(UserState toAdd) {
    this.states.add(toAdd);
  }

  @Override
  public TransitionResult<UserStatus, UserState> canTransitionTo(UserState from, UserState to) {
    if (from.getProgressionStatus().equals(to.getProgressionStatus())) {
      return ko();
    }
    return ok(to);
  }
}
