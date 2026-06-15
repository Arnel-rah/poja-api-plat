package api.poja.io.service;

import api.poja.io.repository.jpa.ApplicationImportLogRepository;
import api.poja.io.repository.model.ApplicationImportLog;
import api.poja.io.repository.model.mapper.ApplicationImportLogMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApplicationImportLogService {
  private final ApplicationImportLogRepository repository;
  private final ApplicationImportLogMapper mapper;

  public List<ApplicationImportLog> getLogsByStateId(String stateId) {
    var sortByTimestampAsc = Sort.by("timestamp").descending();
    return repository.findAllByStateId(stateId, sortByTimestampAsc);
  }

  public List<ApplicationImportLog> saveAll(List<ApplicationImportLog> logs) {
    return repository.saveAll(logs);
  }

  public List<ApplicationImportLog> saveAll(
      List<api.poja.io.model.importer.model.ApplicationImportLog> logs, String stateId) {
    var logsEntities = logs.stream().map(e -> mapper.toEntity(e, stateId)).toList();
    return saveAll(logsEntities);
  }
}
