package api.poja.io.repository.model;

import api.poja.io.repository.model.enums.AppSetupStateEnum;
import api.poja.io.repository.model.workflows.State;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "\"app_setup_state\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@ToString(callSuper = true)
@Setter
public class AppSetupState extends State<AppSetupStateEnum> {
  @JoinColumn(referencedColumnName = "id")
  private String appId;

  @JoinColumn(referencedColumnName = "id")
  private String orgId;
}
