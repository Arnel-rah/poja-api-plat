package api.poja.io.repository.model.mapper;

import api.poja.io.model.importer.model.ApplicationImportLog;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component("DomainApplicationImportLogMapper")
@AllArgsConstructor
public class ApplicationImportLogMapper {

  public api.poja.io.repository.model.ApplicationImportLog toEntity(
      ApplicationImportLog domain, String stateId) {
    return api.poja.io.repository.model.ApplicationImportLog.builder()
        .id(domain.getId())
        .type(domain.getType())
        .message(domain.getMessage())
        .stateId(stateId)
        .build();
  }

  public ApplicationImportLog toDomain(api.poja.io.repository.model.ApplicationImportLog entity) {
    return ApplicationImportLog.builder()
        .id(entity.getId())
        .type(entity.getType())
        .message(entity.getMessage())
        .build();
  }
}
