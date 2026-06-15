package api.poja.io.model;

import static api.poja.io.repository.model.workflows.TransitionResult.ko;
import static api.poja.io.repository.model.workflows.TransitionResult.ok;

import api.poja.io.repository.model.UserPaymentSetupState;
import api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum;
import api.poja.io.repository.model.workflows.AbstractStateMachine;
import api.poja.io.repository.model.workflows.TransitionResult;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UserPaymentSetupStateDTO
    extends AbstractStateMachine<UserPaymentSetupStatusEnum, UserPaymentSetupState> {
  private final String userId;
  private final List<UserPaymentSetupState> states;

  @Override
  protected List<UserPaymentSetupState> internalStates() {
    return states;
  }

  @Override
  protected void addState(UserPaymentSetupState state) {
    states.add(state);
  }

  @Override
  public TransitionResult<UserPaymentSetupStatusEnum, UserPaymentSetupState> canTransitionTo(
      UserPaymentSetupState from, UserPaymentSetupState to) {
    var currentStatus = from.getProgressionStatus();
    var targetStatus = to.getProgressionStatus();

    return switch (currentStatus) {
      case PAYMENT_SETUP_IN_PROGRESS ->
          switch (targetStatus) {
            case PAYMENT_SETUP_COMPLETED, PAYMENT_SETUP_FAILED -> ok(to);
            default -> ko();
          };
      default -> ko();
    };
  }
}
