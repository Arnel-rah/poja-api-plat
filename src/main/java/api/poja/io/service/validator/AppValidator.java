package api.poja.io.service.validator;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotImplementedException;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.model.Application;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppValidator implements Consumer<Application> {
  private final ApplicationRepository repository;
  private final AppNameValidator nameValidator;

  @Override
  public void accept(Application app) {
    String id = app.getId();
    String name = app.getName();

    boolean existsById = repository.existsById(id);
    if (!existsById) {
      nameValidator.checkNoDuplicateByUser(name, app.getUserId());
    }
    if (app.getGithubRepositoryId() != null) {
      boolean existsByRepoId = repository.existsByGithubRepositoryId(app.getGithubRepositoryId());
      if (!existsById && existsByRepoId) {
        throw new NotImplementedException(
            "Multiple import on single repository has not been implemented yet. Github Repository"
                + " named repoName="
                + app.getGithubRepositoryName()
                + " has already been imported by another user.");
      }
    }
    if (existsById) {
      // optional is already checked since it exists by id
      var persisted = repository.findById(id).orElseThrow();
      if (persisted.isArchived()) {
        throw new BadRequestException("archived app cannot be updated");
      }
      app.setPreviousGithubRepositoryName(persisted.getGithubRepositoryName());
    } else {
      nameValidator.checkFormat(name);
    }
  }
}
