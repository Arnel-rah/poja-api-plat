package api.poja.io.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import api.poja.io.repository.model.enums.OrganizationInviteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "\"organization_invite\"")
@EqualsAndHashCode
@ToString
public class OrganizationInvite {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @Column(name = "inviter_org")
  private String inviterOrg;

  @Column(name = "invited_user")
  private String invitedUser;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(STRING)
  private OrganizationInviteStatus status;

  @Column(name = "creation_datetime")
  @CreationTimestamp
  private Instant creationDatetime;
}
