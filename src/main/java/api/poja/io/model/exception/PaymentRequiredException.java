package api.poja.io.model.exception;

import static api.poja.io.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;

public class PaymentRequiredException extends ApiException {
  public PaymentRequiredException() {
    super(CLIENT_EXCEPTION, "payment or payment details required");
  }
}
