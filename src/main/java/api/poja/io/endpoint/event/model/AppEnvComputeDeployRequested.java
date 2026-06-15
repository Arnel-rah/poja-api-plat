package api.poja.io.endpoint.event.model;

import static api.poja.io.endpoint.event.EventStack.EVENT_STACK_1;
import static java.time.Duration.ZERO;

import api.poja.io.endpoint.event.EventStack;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Builder
public class AppEnvComputeDeployRequested extends PojaEvent {
  @JsonProperty("org_id")
  private String orgId;

  @JsonProperty("app_id")
  private String appId;

  @JsonProperty("env_id")
  private String envId;

  @JsonProperty("formatted_bucket_key")
  private final String formattedBucketKey;

  @JsonProperty("app_name")
  private final String appName;

  @JsonProperty("stack_name")
  private final String stackName;

  @JsonProperty("independent_stack_names")
  private final List<String> independentStackNames;

  @JsonProperty("environment_type")
  private final EnvironmentType environmentType;

  @JsonProperty("request_instant")
  private final Instant requestInstant;

  @JsonProperty("app_env_deployment_id")
  private String appEnvDeploymentId;

  // unused method because event is not consumed by our api
  @Override
  public Duration maxConsumerDuration() {
    return ZERO;
  }

  // unused method because event is not consumed by our api
  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return ZERO;
  }

  /**
   * this method is unused as our api will never consume this event due to using a different
   * event.source
   *
   * @return event stack used as sqs source
   */
  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_1;
  }

  /**
   * specifies the event source, used for event consuming and producing. the event producing rule
   * will route this to the deployer app
   *
   * @return jcloudify.app.deployer event source
   */
  @Override
  public String getEventSource() {
    return "app.poja.io.deployer.event.deploy";
  }
}
