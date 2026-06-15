package api.poja.io.service;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.ApplicationImportRepository;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class ApplicationImportArchivalService {
  private final ApplicationImportRepository importRepository;

  public void archiveById(String importId) {
    var appImport =
        importRepository
            .findById(importId)
            .orElseThrow(
                () -> new NotFoundException("ApplicationImport.id=" + importId + " not found"));

    if (appImport.isArchived()) {
      throw new BadRequestException("ApplicationImport.id=" + importId + " is already archived");
    }

    appImport.setArchived(true);
    appImport.setArchivedAt(Instant.now());
    importRepository.save(appImport);

    log.info("Archived ApplicationImport.id={}", importId);
  }
}
