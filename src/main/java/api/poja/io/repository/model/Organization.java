package api.poja.io.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "\"organization\"")
@EqualsAndHashCode
@ToString
public class Organization {
  @Id private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "owner_id")
  private String ownerId;

  @CreationTimestamp
  @Column(name = "creation_datetime")
  private Instant creationDatetime;

  private String consoleUsername;
  private String consolePassword;
  private String consoleAccountId;
  private String consoleUserPolicyDocumentName;
  private String consoleUserGroupName;
  private String consoleUserGroupPolicyDocumentName;
}
