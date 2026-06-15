package api.poja.io.unit.validator;

import static api.poja.io.endpoint.validator.CreateAppBodyValidator.DOMAIN_APP_NAME_MAX_LENGTH;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.model.ApplicationBase;
import api.poja.io.endpoint.rest.model.CreateAndDeployAppRequestBody;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.GithubRepository;
import api.poja.io.endpoint.validator.CreateAppBodyValidator;
import api.poja.io.model.exception.NotImplementedException;
import api.poja.io.repository.jpa.ApplicationRepository;
import org.junit.jupiter.api.Test;

class CreateAppBodyValidatorTest extends MockedThirdParties {
  private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
  private final CreateAppBodyValidator subject = new CreateAppBodyValidator(applicationRepository);

  @Test
  void should_not_throw_exception_for_valid_input() {
    assertDoesNotThrow(() -> subject.accept(request_body_with_valid_input()));
  }

  @Test
  void should_throw_bad_request_exception_for_null_app_to_create() {
    assertThrowsDomainBadRequestException(
        "Application to create is null.", () -> subject.accept(request_body_with_app_null()));
  }

  @Test
  void should_throw_bad_request_exception_for_null_env_to_create() {
    assertThrowsDomainBadRequestException(
        "Environment is null.", () -> subject.accept(request_body_with_env_null()));
  }

  @Test
  void should_throw_bad_request_exception_for_null_env_conf_to_create() {
    assertThrowsDomainBadRequestException(
        "Environment configuration is null.",
        () -> subject.accept(request_body_with_env_conf_null()));
  }

  @Test
  void should_throw_bad_request_exception_for_archived_application() {
    assertThrowsDomainBadRequestException(
        "Application to create is archived.",
        () -> subject.accept(request_body_with_app_archived()));
  }

  @Test
  void should_throw_bad_request_exception_for_archived_environment() {
    assertThrowsDomainBadRequestException(
        "Environment to create is archived.",
        () -> subject.accept(request_body_with_env_archived()));
  }

  @Test
  void should_throw_bad_request_exception_for_invalid_app_name() {
    assertThrowsDomainBadRequestException(
        "app_name must not have more than "
            + DOMAIN_APP_NAME_MAX_LENGTH
            + " characters and contain only lowercase letters, numbers and hyphen (-).",
        () -> subject.accept(request_body_with_invalid_app_name()));
  }

  @Test
  void should_throw_bad_request_exception_for_repository_name_null() {
    assertThrowsDomainBadRequestException(
        "Github repository name is null.",
        () -> subject.accept(request_body_with_github_repository_name_null()));
  }

  @Test
  void should_throw_bad_request_exception_for_env_id_null() {
    assertThrowsDomainBadRequestException(
        "Environment id is null.", () -> subject.accept(request_body_with_env_id_null()));
  }

  @Test
  void should_throw_bad_request_exception_for_repository_null() {
    assertThrowsDomainBadRequestException(
        "Github repository is null.",
        () -> subject.accept(request_body_with_github_repository_null()));
  }

  @Test
  void should_throw_not_implemented_exception_when_github_repository_already_imported() {
    var body =
        new CreateAndDeployAppRequestBody()
            .application(minimalValidApp())
            .environment(minimalValidEnv())
            .envConf(minimalValidEnvConf());
    var repositoryName = minimalValidApp().getGithubRepository().getName();

    when(applicationRepository.existsByGithubRepositoryId(
            minimalValidApp().getGithubRepository().getId()))
        .thenReturn(true);

    assertThrows(
        NotImplementedException.class,
        () -> subject.accept(body),
        "Multiple import on single repository has not been implemented yet. "
            + "Github Repository named repoName="
            + repositoryName
            + " has already been imported by another user.");
  }

  private static GithubRepository minimalGhRepository() {
    return new GithubRepository().id("gh_repo_id").name("repository_name");
  }

  private static ApplicationBase minimalValidApp() {
    return new ApplicationBase()
        .id("app_id")
        .name("valid-name")
        .archived(false)
        .githubRepository(minimalGhRepository());
  }

  private static CrupdateEnvironment minimalValidEnv() {
    return new CrupdateEnvironment().id("env_id").archived(false);
  }

  private static EnvConf minimalValidEnvConf() {
    return new EnvConf().id("env_conf_id");
  }

  private static CreateAndDeployAppRequestBody request_body_with_valid_input() {
    return new CreateAndDeployAppRequestBody()
        .application(minimalValidApp())
        .environment(minimalValidEnv())
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_app_null() {
    return new CreateAndDeployAppRequestBody()
        .application(null)
        .environment(minimalValidEnv())
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_env_null() {
    return new CreateAndDeployAppRequestBody()
        .application(minimalValidApp())
        .environment(null)
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_env_conf_null() {
    return new CreateAndDeployAppRequestBody()
        .application(minimalValidApp())
        .environment(minimalValidEnv())
        .envConf(null);
  }

  private static CreateAndDeployAppRequestBody request_body_with_app_archived() {
    var app = minimalValidApp().archived(true);
    return new CreateAndDeployAppRequestBody()
        .application(app)
        .environment(minimalValidEnv())
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_env_archived() {
    var env = minimalValidEnv().archived(true);
    return new CreateAndDeployAppRequestBody()
        .application(minimalValidApp())
        .environment(env)
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_invalid_app_name() {
    var app = minimalValidApp().name("invalid_app_name");
    return new CreateAndDeployAppRequestBody()
        .application(app)
        .environment(minimalValidEnv())
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_github_repository_null() {
    var app = minimalValidApp().githubRepository(null);
    return new CreateAndDeployAppRequestBody()
        .application(app)
        .environment(minimalValidEnv())
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_github_repository_name_null() {
    var app = minimalValidApp().githubRepository(minimalGhRepository().name(null));
    return new CreateAndDeployAppRequestBody()
        .application(app)
        .environment(minimalValidEnv())
        .envConf(minimalValidEnvConf());
  }

  private static CreateAndDeployAppRequestBody request_body_with_env_id_null() {
    return new CreateAndDeployAppRequestBody()
        .application(minimalValidApp())
        .environment(minimalValidEnv().id(null))
        .envConf(minimalValidEnvConf());
  }
}
