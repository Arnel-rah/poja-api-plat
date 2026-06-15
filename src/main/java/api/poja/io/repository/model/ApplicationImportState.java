package api.poja.io.repository.model;

import static jakarta.persistence.FetchType.LAZY;

import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import api.poja.io.repository.model.workflows.State;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "\"application_import_state\"")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Getter
@Setter
@ToString(callSuper = true)
public class ApplicationImportState extends State<ApplicationImportStateStatus> {
  @JoinColumn(referencedColumnName = "id", updatable = false)
  private String importId;

  @OneToMany(mappedBy = "stateId", fetch = LAZY)
  @Builder.Default
  private List<ApplicationImportLog> logs = List.of();
}
