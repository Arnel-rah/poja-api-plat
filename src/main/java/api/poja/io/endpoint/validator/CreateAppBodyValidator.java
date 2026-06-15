package api.poja.io.endpoint.validator;

import static api.poja.io.service.validator.AppNameValidator.appNamePatternWithLen;
import static java.lang.Boolean.TRUE;
import static java.lang.String.join;

import api.poja.io.endpoint.rest.model.CreateAndDeployAppRequestBody;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.exception.NotImplementedException;
import api.poja.io.repository.jpa.ApplicationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateAppBodyValidator implements Consumer<CreateAndDeployAppRequestBody> {
  public static final int DOMAIN_APP_NAME_MAX_LENGTH = 11;
  private final ApplicationRepository applicationRepository;

  @Override
  public void accept(CreateAndDeployAppRequestBody body) {
    List<String> errors = new ArrayList<>();

    if (body.getApplication() == null) {
      errors.add("Application to create is null.");
    } else {
      var app = body.getApplication();
      if (app.getId() == null) {
        errors.add("Application to create has no id.");
      } else {
        var appName = app.getName();
        var appRepo = app.getGithubRepository();
        if (appRepo == null) {
          errors.add("Github repository is null.");
        } else {
          if (appRepo.getName() == null) {
            errors.add("Github repository name is null.");
          } else if (appRepo.getName().isEmpty()) {
            errors.add("Github repository name is empty.");
          }
          if (appRepo.getId() != null) {
            boolean existsByRepoId =
                applicationRepository.existsByGithubRepositoryId(app.getGithubRepository().getId());
            if (existsByRepoId) {
              throw new NotImplementedException(
                  "Multiple import on single repository has not been implemented yet. "
                      + "Github Repository named repoName="
                      + app.getGithubRepository().getName()
                      + " has already been imported by another user.");
            }
          }
        }
        if (!isAValidAppName(appName, appNamePatternWithLen(DOMAIN_APP_NAME_MAX_LENGTH))) {
          errors.add(
              "app_name must not have more than "
                  + DOMAIN_APP_NAME_MAX_LENGTH
                  + " characters and contain only lowercase letters, numbers and hyphen (-).");
        }
      }
      if (TRUE.equals(app.getArchived())) {
        errors.add("Application to create is archived.");
      }
    }
    if (body.getEnvironment() == null) {
      errors.add("Environment is null.");
    } else {
      if (TRUE.equals(body.getEnvironment().getArchived())) {
        errors.add("Environment to create is archived.");
      }
      if (body.getEnvironment().getId() == null) {
        errors.add("Environment id is null.");
      }
    }
    if (body.getEnvConf() == null) {
      errors.add("Environment configuration is null.");
    }
    if (!errors.isEmpty()) {
      throw new BadRequestException(join(" ", errors));
    }
  }

  public static boolean isAValidAppName(String appName, Pattern regexPattern) {
    return regexPattern != null && appName != null && regexPattern.matcher(appName).matches();
  }
}
