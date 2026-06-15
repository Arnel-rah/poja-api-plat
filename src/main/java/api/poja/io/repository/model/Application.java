package api.poja.io.repository.model;

import static api.poja.io.endpoint.rest.model.Application.StatusEnum.ACTIVE;
import static api.poja.io.endpoint.rest.model.Application.StatusEnum.SUSPENDED;
import static api.poja.io.endpoint.rest.model.Application.StatusEnum.UNDER_MODIFICATION;
import static api.poja.io.endpoint.rest.model.Application.StatusEnum.UNKNOWN;
import static api.poja.io.service.validator.AppNameValidator.DOMAIN_APP_NAME_LEN;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;

import api.poja.io.endpoint.rest.model.Application.StatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"application\"")
@EqualsAndHashCode
@ToString
public class Application implements Serializable {
  @Id private String id;

  private String name;
  private boolean archived;

  private String githubRepositoryName;
  private boolean isGithubRepositoryPrivate;
  private String installationId;
  private String importId;

  @Transient @EqualsAndHashCode.Exclude private String previousGithubRepositoryName;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant creationDatetime;

  @Column(name = "id_user")
  private String userId;

  private String description;

  @OneToMany(mappedBy = "applicationId", cascade = ALL, fetch = EAGER)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @SQLRestriction("archived = 'false'")
  private List<Environment> environments;

  @Column(name = "repo_http_url")
  private String githubRepositoryUrl;

  @Column private String githubRepositoryId;

  @Column private String orgId;
  private Instant archivedAt;

  @JsonIgnore
  public String getFormattedName() {
    return name.replace("_", "-");
  }

  @JsonIgnore
  public String getFormattedUserId() {
    return userId.substring(0, 8);
  }

  public boolean isSuspended() {
    return SUSPENDED.equals(getStatus());
  }

  @JsonIgnore
  public boolean isImported() {
    return importId != null;
  }

  @JsonIgnore
  public StatusEnum getStatus() {
    if (environments == null || environments.isEmpty()) {
      return UNKNOWN;
    }

    if (environments.stream().anyMatch(Environment::isUnderModification)) {
      return UNDER_MODIFICATION;
    }

    if (environments.stream().filter(e -> !e.isArchived()).allMatch(Environment::isSuspended)) {
      return SUSPENDED;
    }
    return ACTIVE;
  }

  public String getSuffixedAppName() {
    var name = getFormattedName();
    if (name.length() <= DOMAIN_APP_NAME_LEN) {
      return String.join("-", name, getFormattedUserId());
    }
    return name;
  }
}
