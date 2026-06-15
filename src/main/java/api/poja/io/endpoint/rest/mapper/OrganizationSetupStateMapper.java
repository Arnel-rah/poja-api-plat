package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.OrganizationSetupState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OrganizationSetupStateMapper {
  private final OrganizationSetupStateEnumStatusMapper statusMapper;

  public OrganizationSetupState toRest(api.poja.io.repository.model.OrganizationSetupState domain) {
    var status = statusMapper.toRest(domain.getProgressionStatus());

    return new OrganizationSetupState()
        .id(domain.getId())
        .executionType(domain.getExecutionType())
        .timestamp(domain.getTimestamp())
        .progressionStatus(status);
  }
}
