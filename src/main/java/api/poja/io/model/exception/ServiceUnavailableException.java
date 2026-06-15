package api.poja.io.model.exception;

public class ServiceUnavailableException extends ApiException {
  public ServiceUnavailableException(Exception source) {
    super(ExceptionType.SERVER_EXCEPTION, source);
  }

  public ServiceUnavailableException(String message) {
    super(ExceptionType.SERVER_EXCEPTION, message);
  }
}
