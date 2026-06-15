package api.poja.io.service.github.model.exception;

public class WorkflowRunFailed extends RuntimeException {
  public WorkflowRunFailed(String message) {
    super(message);
  }
}
