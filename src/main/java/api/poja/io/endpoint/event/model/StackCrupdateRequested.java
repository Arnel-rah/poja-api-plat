package api.poja.io.endpoint.event.model;

import api.poja.io.endpoint.rest.model.StackDeployment;
import api.poja.io.repository.model.EnvDeploymentConf;
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
public class StackCrupdateRequested extends PojaEvent {
  private StackDeployment independantStackToDeploy;
  private String orgId;
  private String applicationId;
  private String environmentId;
  private AppEnvDeployRequested appEnvDeployRequested;
  private EnvDeploymentConf envDeploymentConf;
  private String appEnvDeplId;
  private StackPair stackToCrupdate;
  private StackPair dependantStack;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }

  public record StackPair(Stack first, Stack last) {}
}
