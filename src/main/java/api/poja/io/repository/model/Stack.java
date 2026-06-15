package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.endpoint.rest.model.StackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"stack\"")
@EqualsAndHashCode
@ToString
/*
 * a Stack represents a group of deployable resource, normally, we would only have one stack because it is always the same in cloudFormation, however, we decide to store multiple stacks but unique by type and appEnvDeplId in order to know which stacks are needed for which deployment
 */
public class Stack implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String name;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private StackType type;

  private String cfStackId;

  @Column(name = "id_environment")
  private String environmentId;

  @Column(name = "id_application")
  private String applicationId;

  @CreationTimestamp
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIMEZONE")
  private Instant creationDatetime;

  @UpdateTimestamp
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIMEZONE")
  private Instant updateDatetime;

  private boolean archived;

  private String appEnvDeplId;
}
