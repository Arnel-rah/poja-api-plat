package api.poja.io.endpoint.event.model;

import static api.poja.io.endpoint.event.EventStack.EVENT_STACK_1;
import static java.time.Duration.ZERO;

import api.poja.io.endpoint.event.EventStack;
import api.poja.io.endpoint.rest.model.BuiltEnvInfo;
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
public class CheckTemplateIntegrityTriggered extends PojaEvent {
  @JsonProperty("org_id")
  private String orgId;

  @JsonProperty("app_id")
  private String appId;

  @JsonProperty("env_id")
  private String envId;

  @JsonProperty("built_project_bucket_key")
  private String builtProjectBucketKey;

  @JsonProperty("template_file_bucket_key")
  private String templateFileBucketKey;

  @JsonProperty("built_env_info")
  private BuiltEnvInfo builtEnvInfo;

  @JsonProperty("deployment_conf_id")
  private String deploymentConfId;

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
    return "app.poja.io.deployer.event.check";
  }
}
