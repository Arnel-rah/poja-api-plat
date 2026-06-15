package api.poja.io.repository.model.workflows.exception;

public abstract sealed class IllegalStateTransitionException extends RuntimeException
    permits InvalidStatusTransitionException, OlderStateTransitionException {
  public IllegalStateTransitionException(String message) {
    super(message);
  }
}
