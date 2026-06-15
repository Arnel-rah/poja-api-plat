package api.poja.io.repository.model;

import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.SUSPENDED;
import static api.poja.io.endpoint.rest.model.Environment.StatusEnum.UNDER_MODIFICATION;
import static jakarta.persistence.EnumType.STRING;
import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.endpoint.rest.model.Environment.StateEnum;
import api.poja.io.endpoint.rest.model.Environment.StatusEnum;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "\"environment\"")
@EqualsAndHashCode
@ToString
public class Environment implements Serializable {
  @Id private String id;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  @Column(name = "environment_type")
  private EnvironmentType environmentType;

  private boolean archived;

  @Column(name = "id_application")
  private String applicationId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  @Column(name = "state")
  private StateEnum state;

  private String configurationFileKey;
  private String codeFileKey;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant creationDatetime;

  private String currentDeploymentId;
  private String currentConfId;
  private String appliedConfId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  @Column(insertable = false, updatable = false)
  private StatusEnum status;

  /**
   * @param configurationFileKey non formatted s3 file key, will need to be formatted using
   *     ExtendedBucketComponent .getBucketKey to get the real filename
   */
  @Builder(toBuilder = true)
  public Environment(
      String id,
      EnvironmentType environmentType,
      boolean archived,
      String applicationId,
      StateEnum state,
      String configurationFileKey,
      String codeFileKey,
      List<EnvDeploymentConf> envDeploymentConfs,
      Instant creationDatetime,
      String currentDeploymentId,
      String currentConfId,
      String appliedConfId,
      Instant archivedAt,
      StatusEnum status) {
    this.id = id;
    this.environmentType = environmentType;
    this.archived = archived;
    this.applicationId = applicationId;
    this.state = state;
    this.configurationFileKey = configurationFileKey;
    this.codeFileKey = codeFileKey;
    this.envDeploymentConfs = envDeploymentConfs;
    this.creationDatetime = creationDatetime;
    this.currentDeploymentId = currentDeploymentId;
    this.currentConfId = currentConfId;
    this.appliedConfId = appliedConfId;
    this.archivedAt = archivedAt;
    this.status = status;
  }

  public boolean isSuspended() {
    return SUSPENDED.equals(status);
  }

  public boolean isUnderModification() {
    return UNDER_MODIFICATION.equals(status);
  }

  @OneToMany
  @JoinColumn(name = "env_id", insertable = false, updatable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<EnvDeploymentConf> envDeploymentConfs;

  @OneToMany
  @JoinColumn(name = "env_id", insertable = false, updatable = false)
  @OrderBy(value = "creationDatetime DESC")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @BatchSize(size = 5)
  private List<AppEnvironmentDeployment> deployments;

  private Instant archivedAt;

  public EnvDeploymentConf getLatestDeploymentConf() {
    return envDeploymentConfs.stream()
        .max(comparing(EnvDeploymentConf::getCreationDatetime))
        .orElseThrow();
  }

  public Environment archiveSelf() {
    this.setArchived(true);
    return this;
  }

  @JsonIgnore
  public String getFormattedEnvironmentType() {
    return formatEnvironmentType(environmentType);
  }

  public static String formatEnvironmentType(EnvironmentType environmentType) {
    return environmentType.toString().toLowerCase();
  }

  @JsonIgnore
  public String getGhBranchName() {
    return getFormattedEnvironmentType();
  }

  public Optional<AppEnvironmentDeployment> getLatestDeployment() {
    if (deployments == null || deployments.isEmpty()) {
      return empty();
    }
    for (AppEnvironmentDeployment deployment : deployments) {
      if (deployment.getDeployedUrl() != null) {
        return Optional.of(deployment);
      }
    }
    return empty();
  }
}
