package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationImportState;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ApplicationImportStateMapper {
  private final ApplicationImportStateEnumMapper statusMapper;
  private final ApplicationImportLogMapper logMapper;

  public ApplicationImportState toRest(api.poja.io.repository.model.ApplicationImportState domain) {
    return new ApplicationImportState()
        .id(domain.getId())
        .executionType(domain.getExecutionType())
        .timestamp(domain.getTimestamp())
        .progressionStatus(statusMapper.toRest(domain.getProgressionStatus()))
        .logs(domain.getLogs().stream().map(logMapper::toRest).toList());
  }
}
