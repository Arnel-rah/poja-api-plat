package api.poja.io.endpoint.event.model;

import api.poja.io.endpoint.event.model.enums.StackCrupdateStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder
public class ComputeStackCrupdated extends PojaEvent {
  @JsonProperty("org_id")
  private String orgId;

  @JsonProperty("app_id")
  private String appId;

  @JsonProperty("env_id")
  private String envId;

  @JsonProperty("stack_name")
  private String stackName;

  @JsonProperty("app_env_deployment_id")
  private String appEnvDeploymentId;

  @JsonProperty("stack_deployment_state")
  private StackCrupdateStatus stackDeploymentState;

  @JsonProperty("api_url")
  private String apiUrl;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
