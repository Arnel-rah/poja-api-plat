package api.poja.io.endpoint.event.model;

import api.poja.io.model.PojaVersion;
import java.time.Duration;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public final class PojaConfUploaded extends PojaEvent {
  /**
   * constructor of PojaConfUploaded
   *
   * @param pojaVersion cli_version
   * @param environmentId environment configured with the conf
   * @param orgId poja conf owner userid
   * @param filename refers to the s3 key without the prefixes (orgId, environmentId, ...)
   * @param appId configuredAppId
   * @param sourceBranch branch used for the initial deployment of the specified
   *     Environment::environmentType. Defaults to the repository’s primary branch if {@code null}.
   */
  public PojaConfUploaded(
      PojaVersion pojaVersion,
      String environmentId,
      String orgId,
      String filename,
      String appId,
      String appEnvDeplId,
      String envDeplConfId,
      @Nullable String sourceBranch) {
    this.pojaVersion = pojaVersion;
    this.environmentId = environmentId;
    this.orgId = orgId;
    this.filename = filename;
    this.appId = appId;
    this.appEnvDeplId = appEnvDeplId;
    this.envDeplConfId = envDeplConfId;
    this.sourceBranch = sourceBranch;
  }

  private final PojaVersion pojaVersion;
  private final String environmentId;
  private final String orgId;
  private final String filename;
  private final String appId;
  private final String appEnvDeplId;
  private final String envDeplConfId;
  private final String sourceBranch;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(40);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
