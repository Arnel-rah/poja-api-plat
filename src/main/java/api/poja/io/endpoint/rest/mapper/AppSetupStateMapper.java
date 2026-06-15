package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.AppSetupState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppSetupStateMapper {
  private final AppSetupStateEnumMapper mapper;

  public AppSetupState toRest(api.poja.io.repository.model.AppSetupState domain) {
    return new AppSetupState()
        .id(domain.getId())
        .executionType(domain.getExecutionType())
        .progressionStatus(mapper.toRest(domain.getProgressionStatus()))
        .timestamp(domain.getTimestamp());
  }
}
