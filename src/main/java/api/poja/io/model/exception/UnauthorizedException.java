package api.poja.io.model.exception;

import static api.poja.io.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;

public class UnauthorizedException extends ApiException {

  public UnauthorizedException(String message) {
    super(CLIENT_EXCEPTION, message);
  }

  public UnauthorizedException(Exception source) {
    super(CLIENT_EXCEPTION, source);
  }
}
