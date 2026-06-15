package api.poja.io.repository.model;

import api.poja.io.model.UserStatus;
import api.poja.io.repository.model.workflows.State;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@Table(name = "\"user_state\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString(callSuper = true)
public class UserState extends State<UserStatus> {
  @JoinColumn(referencedColumnName = "id", updatable = false)
  private String userId;

  private String description;
}
