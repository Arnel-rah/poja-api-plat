package api.poja.io.endpoint.event.model;

import api.poja.io.repository.model.Stack;
import java.time.Duration;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder
@AllArgsConstructor
public class StackCloudPermissionRemovalRequested extends PojaEvent {
  private String orgId;
  private List<Stack> computeStacks;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(40);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
