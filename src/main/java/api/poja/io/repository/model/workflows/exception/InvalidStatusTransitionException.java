package api.poja.io.repository.model.workflows.exception;

import api.poja.io.repository.model.workflows.State;

public final class InvalidStatusTransitionException extends IllegalStateTransitionException {
  private static final String TEMPLATE = "Illegal transition status from=%s to=%s";

  public InvalidStatusTransitionException(State<?> from, State<?> to) {
    super(String.format(TEMPLATE, from.getProgressionStatus(), to.getProgressionStatus()));
  }
}
