package api.poja.io.repository.model;

import api.poja.io.repository.model.enums.UserPaymentSetupStatusEnum;
import api.poja.io.repository.model.workflows.State;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@Table(name = "\"user_payment_setup_state\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Setter
public class UserPaymentSetupState extends State<UserPaymentSetupStatusEnum> {
  @JoinColumn(referencedColumnName = "id")
  private String userId;
}
