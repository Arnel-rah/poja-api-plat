package api.poja.io.endpoint.event.model;

import static api.poja.io.endpoint.event.EventStack.EVENT_STACK_2;

import api.poja.io.endpoint.event.EventStack;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.endpoint.rest.model.FunctionType;
import api.poja.io.endpoint.rest.model.SortDirection;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class LogQueryCreated extends PojaEvent {
  private String domainQueryId;
  private Set<String> queryFilterKeywords;
  private SortDirection queryTimestampSortDirection;
  private Set<EnvironmentType> environmentTypes;
  private Instant queryInstantIntervalBegin;
  private Instant queryInstantIntervalEnd;
  private Set<FunctionType> functionTypes;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(90);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
