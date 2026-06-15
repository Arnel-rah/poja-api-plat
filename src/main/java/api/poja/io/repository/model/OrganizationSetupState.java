package api.poja.io.repository.model;

import api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum;
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
@Table(name = "\"organization_setup_state\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@ToString(callSuper = true)
@Setter
public class OrganizationSetupState extends State<OrganizationSetupStateStatusEnum> {
  @JoinColumn(referencedColumnName = "id")
  private String orgId;
}
