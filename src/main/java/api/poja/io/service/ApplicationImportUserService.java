package api.poja.io.service;

import static api.poja.io.repository.model.enums.ApplicationImportStatus.IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.PENDING;

import api.poja.io.repository.jpa.ApplicationImportRepository;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApplicationImportUserService {
  private final ApplicationImportRepository repository;

  public long countUserActiveAppImports(String userId) {
    return repository.countApplicationImportsByUserIdAndStatusInAndArchived(
        userId, Set.of(PENDING, IN_PROGRESS), false);
  }

  public long countUserActiveAppImports(String userId, Set<String> exclude) {
    return repository.countApplicationImportsByUserIdAndStatusInAndIdNotInAndArchived(
        userId, Set.of(PENDING, IN_PROGRESS), exclude, false);
  }
}
