package api.poja.io.model.workflows;

import static api.poja.io.model.workflows.ExampleStateMachineImpl.UserStateEnum.DELETED;
import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;

import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.State;
import api.poja.io.repository.model.workflows.TransitionResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
final class ExampleStateMachineImpl
    extends AbstractStateMachine<
        ExampleStateMachineImpl.UserStateEnum, ExampleStateMachineImpl.UserState> {
  private List<UserState> states;

  @AllArgsConstructor
  @Getter
  enum UserStateEnum {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    DELETED("DELETED");
    private final String value;

    @Override
    public String toString() {
      return String.valueOf(value);
    }
  }

  @AllArgsConstructor
  @SuperBuilder(toBuilder = true)
  static class UserState extends State<UserStateEnum> {}

  @Override
  protected List<UserState> internalStates() {
    return states;
  }

  @Override
  protected void addState(UserState state) {
    states.add(state);
  }

  @Override
  public TransitionResult<UserStateEnum, UserState> canTransitionTo(UserState from, UserState to) {
    return switch (to.getProgressionStatus()) {
      case ACTIVE, INACTIVE -> {
        if (DELETED.equals(from.getProgressionStatus())) {
          yield ko();
        }
        yield ok(to);
      }
      case DELETED -> ok(to);
    };
  }
}
