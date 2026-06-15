package api.poja.io.endpoint.event.model;

import api.poja.io.endpoint.event.model.enums.TemplateIntegrityStatus;
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
public class TemplateIntegrityCheckDone extends PojaEvent {
  @JsonProperty("org_id")
  private String orgId;

  @JsonProperty("app_id")
  private String appId;

  @JsonProperty("env_id")
  private String envId;

  @JsonProperty("built_project_bucket_key")
  private String builtProjectBucketKey;

  @JsonProperty("built_env_info")
  private BuiltEnvInfo builtEnvInfo;

  @JsonProperty("deployment_conf_id")
  private String deploymentConfId;

  @JsonProperty("status")
  private TemplateIntegrityStatus status;

  @JsonProperty("app_env_deployment_id")
  private String appEnvDeploymentId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(5);
  }
}
