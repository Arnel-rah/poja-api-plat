package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.service.github.model.GhAppInstallation.RepositorySelection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"app_installation\"")
@EqualsAndHashCode
@ToString
public class AppInstallation {
  @Id private String id;
  private long ghId;
  private String userId;
  private String orgId;
  private String ownerGithubLogin;
  private String type;
  private String avatarUrl;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private RepositorySelection repositorySelection;
}
