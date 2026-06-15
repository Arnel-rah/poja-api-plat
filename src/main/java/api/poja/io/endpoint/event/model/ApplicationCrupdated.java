package api.poja.io.endpoint.event.model;

import static api.poja.io.endpoint.event.model.ApplicationCrupdated.CrupdateType.CREATE;
import static api.poja.io.endpoint.event.model.ApplicationCrupdated.CrupdateType.UPDATE;

import java.time.Duration;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ApplicationCrupdated extends PojaEvent {
  private final String applicationId;
  private final String applicationRepoName;
  private final String previousApplicationRepoName;
  private final String description;
  private final boolean repoPrivate;
  private final String installationId;
  private final boolean archived;
  private final CrupdateType crupdateType;
  private final String repoUrl;
  private final String importId;

  @Builder(toBuilder = true)
  public ApplicationCrupdated(
      String applicationId,
      String applicationRepoName,
      String previousApplicationRepoName,
      String description,
      boolean repoPrivate,
      String installationId,
      boolean archived,
      String repoUrl,
      String importId) {
    this.applicationId = applicationId;
    this.applicationRepoName = applicationRepoName;
    this.previousApplicationRepoName = previousApplicationRepoName;
    this.description = description;
    this.repoPrivate = repoPrivate;
    this.installationId = installationId;
    this.archived = archived;
    this.repoUrl = repoUrl;
    this.importId = importId;
    this.crupdateType = importId != null ? UPDATE : repoUrl == null ? CREATE : UPDATE;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }

  public enum CrupdateType {
    CREATE,
    UPDATE
  }
}
