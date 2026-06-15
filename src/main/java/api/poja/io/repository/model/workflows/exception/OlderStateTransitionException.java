package api.poja.io.repository.model.workflows.exception;

import api.poja.io.repository.model.workflows.State;

public final class OlderStateTransitionException extends IllegalStateTransitionException {
  private static final String TEMPLATE =
      "Illegal transition: target state '%s' (timestamp=%s) is older than current state '%s'"
          + " (timestamp=%s)";

  public OlderStateTransitionException(State<?> from, State<?> to) {
    super(
        String.format(
            TEMPLATE,
            to.getProgressionStatus(),
            to.getTimestamp(),
            from.getProgressionStatus(),
            from.getTimestamp()));
  }
}
