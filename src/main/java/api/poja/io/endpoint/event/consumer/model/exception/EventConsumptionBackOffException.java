package api.poja.io.endpoint.event.consumer.model.exception;

public class EventConsumptionBackOffException extends RuntimeException {
  public EventConsumptionBackOffException(String message) {
    super(message);
  }
}
