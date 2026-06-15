package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.model.PojaVersion;
import api.poja.io.repository.model.enums.ApplicationImportStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"application_import\"")
@EqualsAndHashCode
@ToString
public class ApplicationImport {
  @Id private String id;
  private String appName;
  private String githubRepositoryId;
  private String githubRepositoryName;
  private String githubRepositoryHttpUrl;
  private String githubRepositoryDescription;
  private String githubRepositoryDefaultBranch;
  private boolean githubRepositoryPrivate;

  /**
   * Identifier of the application created from this import. Populated at the end of the import
   * check and transformation process.
   */
  private String createdAppId;

  @CreationTimestamp private Instant creationDatetime;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ApplicationImportStatus status;

  private String userId;
  private String orgId;
  private String appInstallationId;
  private String pojaVersion;

  private boolean archived;
  private Instant archivedAt;

  @Transient
  public PojaVersion getPojaVersionEnum() {
    return PojaVersion.fromHumanReadableValue(pojaVersion).orElseThrow();
  }

  public String ghBranchNamePrefix() {
    return "import-" + id;
  }

  /**
   * Returns the final branch name by appending "/final" to the branch prefix.
   *
   * <p>note(!): Directly using the branch name `import/UUID` previously (for some reason) led to
   * LOCK_FAILURE errors.
   */
  public String ghMainBranchName() {
    return ghBranchNamePrefix() + "-main";
  }

  public String ghCommitMsgPrefix() {
    return "poja: import #" + id;
  }
}
