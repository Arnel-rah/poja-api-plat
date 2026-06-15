package api.poja.io.endpoint.rest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import api.poja.io.endpoint.rest.model.ExceptionModel;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.ForbiddenException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.model.exception.NotImplementedException;
import api.poja.io.model.exception.PaymentRequiredException;
import api.poja.io.model.exception.ServiceUnavailableException;
import api.poja.io.model.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class InternalToRestExceptionHandler {
  @ExceptionHandler(value = {BadRequestException.class})
  ResponseEntity<ExceptionModel> handleBadRequest(BadRequestException e) {
    log.info("Bad request", e);
    return new ResponseEntity<>(toRest(e, BAD_REQUEST), BAD_REQUEST);
  }

  @ExceptionHandler(value = {NotFoundException.class})
  ResponseEntity<ExceptionModel> handleNotFound(NotFoundException e) {
    log.info("Not found", e);
    return new ResponseEntity<>(toRest(e, NOT_FOUND), NOT_FOUND);
  }

  @ExceptionHandler(value = {ServiceUnavailableException.class})
  ResponseEntity<ExceptionModel> handleServiceUnavailable(ServiceUnavailableException e) {
    log.error("Service Unavailable", e);
    return new ResponseEntity<>(toRest(e, SERVICE_UNAVAILABLE), SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(value = {java.lang.Exception.class})
  ResponseEntity<ExceptionModel> handleDefault(java.lang.Exception e) {
    log.error("Internal error", e);
    return new ResponseEntity<>(toRest(e, INTERNAL_SERVER_ERROR), INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(
      value = {
        AccessDeniedException.class,
        ForbiddenException.class,
      })
  ResponseEntity<ExceptionModel> handleForbidden(java.lang.Exception e) {
    /* rest.model.Exception.Type.FORBIDDEN designates only authorization errors.
     * Hence do _not_ HttpsStatus.UNAUTHORIZED because, counter-intuitively,
     * it's just for authentication.
     * https://stackoverflow.com/questions/3297048/403-forbidden-vs-401-unauthorized-http-responses */
    log.info("Forbidden", e);
    var restException = new ExceptionModel();
    restException.setType(FORBIDDEN.toString());
    restException.setMessage(e.getMessage());
    return new ResponseEntity<>(restException, FORBIDDEN);
  }

  @ExceptionHandler(value = {MissingServletRequestParameterException.class})
  ResponseEntity<ExceptionModel> handleDataIntegrityViolation(
      MissingServletRequestParameterException e) {
    log.info("Missing parameter", e);
    return new ResponseEntity<>(toRest(e, BAD_REQUEST), BAD_REQUEST);
  }

  @ExceptionHandler(value = {DataIntegrityViolationException.class})
  ResponseEntity<ExceptionModel> handleDataIntegrityViolation(DataIntegrityViolationException e) {
    log.info("Bad request", e);
    return new ResponseEntity<>(toRest(e, BAD_REQUEST), BAD_REQUEST);
  }

  @ExceptionHandler(value = {NotImplementedException.class})
  ResponseEntity<ExceptionModel> handleNotImplemented(NotImplementedException e) {
    log.error("Not implemented", e);
    return new ResponseEntity<>(toRest(e, NOT_IMPLEMENTED), NOT_IMPLEMENTED);
  }

  private static ExceptionModel toRest(java.lang.Exception e, HttpStatus httpStatus) {
    var restException = new ExceptionModel();
    restException.setType(httpStatus.toString());
    restException.setMessage(e.getMessage());
    return restException;
  }

  @ExceptionHandler(
      value = {
        AuthenticationException.class,
        UsernameNotFoundException.class,
        UnauthorizedException.class
      })
  ResponseEntity<ExceptionModel> handleUnauthorized(Exception e) {
    /* rest.model.Exception.Type.Unauthorized designates only authentication errors.
     * Hence do _not_ HttpsStatus.FORBIDDEN because it's just for authorization.
     * https://stackoverflow.com/questions/3297048/403-forbidden-vs-401-unauthorized-http-responses */
    log.info("Unauthorized", e);
    var restException = new ExceptionModel();
    restException.setType(UNAUTHORIZED.toString());
    restException.setMessage(e.getMessage());
    return new ResponseEntity<>(restException, UNAUTHORIZED);
  }

  @ExceptionHandler(value = PaymentRequiredException.class)
  ResponseEntity<ExceptionModel> handlePaymentRequired(PaymentRequiredException e) {
    return new ResponseEntity<>(toRest(e, PAYMENT_REQUIRED), PAYMENT_REQUIRED);
  }
}
