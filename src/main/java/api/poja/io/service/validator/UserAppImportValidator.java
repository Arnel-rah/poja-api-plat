package api.poja.io.service.validator;

import static api.poja.io.repository.model.enums.ApplicationImportStatus.IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.PENDING;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.jpa.ApplicationImportRepository;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.model.ApplicationImport;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserAppImportValidator implements Consumer<String> {
  private final ApplicationImportRepository importRepository;
  private final ApplicationRepository applicationRepository;

  @Override
  public void accept(String repoId) {
    var existingAppImports = importRepository.findAllByGithubRepositoryIdAndArchived(repoId, false);

    var repoHasActiveImports =
        existingAppImports.stream()
            .anyMatch(i -> PENDING.equals(i.getStatus()) || IN_PROGRESS.equals(i.getStatus()));

    if (repoHasActiveImports) {
      throw new BadRequestException(
          "Repository with id="
              + repoId
              + " is currently being imported by a different process. Multiple imports for single"
              + " repository not supported yet");
    }

    var repoHasActiveApps =
        existingAppImports.stream()
            .map(ApplicationImport::getCreatedAppId)
            .filter(Objects::nonNull)
            .anyMatch(i -> applicationRepository.existsByIdAndArchived(i, false));

    if (repoHasActiveApps) {
      throw new BadRequestException(
          "Repository with id="
              + repoId
              + " has already been imported. Multiple imports for single repository not supported"
              + " yet");
    }
  }
}
