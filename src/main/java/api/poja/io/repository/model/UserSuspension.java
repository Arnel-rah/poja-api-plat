package api.poja.io.repository.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
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

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "\"user_suspension\"")
@EqualsAndHashCode
@ToString
public class UserSuspension {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String userId;
  private String suspensionReason;
  private Instant suspendedAt;
}
