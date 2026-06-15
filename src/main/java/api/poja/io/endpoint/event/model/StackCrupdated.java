package api.poja.io.endpoint.event.model;

import api.poja.io.endpoint.event.model.StackCrupdateRequested.StackPair;
import api.poja.io.repository.model.Stack;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder(toBuilder = true)
public class StackCrupdated extends PojaEvent {
  private final String orgId;
  private final Stack stack;
  private final AppEnvDeployRequested parentAppEnvDeployRequested;
  private final String appEnvDeplId;
  private StackPair dependantStack;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(20);
  }
}
