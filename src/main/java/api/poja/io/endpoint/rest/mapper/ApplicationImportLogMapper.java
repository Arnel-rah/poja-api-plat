package api.poja.io.endpoint.rest.mapper;

import api.poja.io.endpoint.rest.model.ApplicationImportLog;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class ApplicationImportLogMapper {
  private final ApplicationImportLogTypeMapper typeMapper;

  public ApplicationImportLog toRest(api.poja.io.repository.model.ApplicationImportLog domain) {
    return new ApplicationImportLog()
        .type(typeMapper.toRest(domain.getType()))
        .content(domain.getMessage())
        .timestamp(domain.getTimestamp());
  }
}
