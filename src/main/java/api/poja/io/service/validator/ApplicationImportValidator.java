package api.poja.io.service.validator;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Boolean.TRUE;

import api.poja.io.endpoint.rest.model.CreateApplicationImportRequestBody;
import api.poja.io.endpoint.rest.model.GithubRepositoryListItem;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.repository.jpa.ApplicationImportRepository;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.service.AppInstallationService;
import java.util.StringJoiner;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class ApplicationImportValidator implements Consumer<CreateApplicationImportRequestBody> {

  private final ApplicationRepository applicationRepository;
  private final AppInstallationService appInstallationService;
  private final ApplicationImportRepository applicationImportRepository;
  private final AppNameValidator appNameValidator;

  @Override
  public void accept(CreateApplicationImportRequestBody body) {
    var sj = new StringJoiner(" ");

    if (isNullOrEmpty(body.getUserId())) {
      sj.add("userId is mandatory.");
    }

    String importId = body.getId();
    if (isNullOrEmpty(importId)) {
      sj.add("id is mandatory.");
    } else if (applicationImportRepository.existsById(importId)) {
      sj.add("Application import with id " + importId + " already exists.");
    }

    appNameValidator.accept(body.getName(), body.getUserId());

    GithubRepositoryListItem ghRepo = body.getGithubRepository();
    if (ghRepo == null) {
      sj.add("Github repository is mandatory.");
    } else {
      if (ghRepo.getName() == null || ghRepo.getName().isEmpty()) {
        sj.add("Github repository name is mandatory.");
      }

      // note: Github's `size` lags behind actual content; treat "size=0" repos as valid
      if (TRUE.equals(ghRepo.getIsEmpty())) {
        log.info("Allowing empty Github repository");
      }

      if (ghRepo.getHtmlUrl() == null || ghRepo.getHtmlUrl().toString().isBlank()) {
        sj.add("Github repository html url is mandatory.");
      }

      String installationId = ghRepo.getInstallationId();
      if (installationId == null) {
        sj.add("Github repository installationId is mandatory.");
      } else if (!appInstallationService.existsById(installationId)) {
        sj.add("Github repository AppInstallation.id=" + installationId + " not found.");
      }

      var ghRepoId = ghRepo.getId();
      if (ghRepoId == null || ghRepoId.isBlank()) {
        sj.add("Github repository id is mandatory.");
      } else {
        if (applicationRepository.existsByGithubRepositoryIdAndArchived(ghRepoId, false)) {
          sj.add("Application with GitHub repository id " + ghRepoId + " already exists.");
        }
      }
    }

    if (sj.length() > 0) {
      throw new BadRequestException(sj.toString());
    }
  }
}
