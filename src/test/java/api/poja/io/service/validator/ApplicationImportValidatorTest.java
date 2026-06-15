package api.poja.io.service.validator;

import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsDomainBadRequestException;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.rest.model.CreateApplicationImportRequestBody;
import api.poja.io.endpoint.rest.model.GithubRepositoryListItem;
import api.poja.io.repository.jpa.ApplicationImportRepository;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.service.AppInstallationService;
import java.net.URI;
import org.junit.jupiter.api.Test;

class ApplicationImportValidatorTest {

  static final String EXISTING_APP_NAME = "existent";
  static final String EXISTING_IMPORT_ID = "existingImportId";
  static final String EXISTING_GH_REPO_ID = "existingGhRepoId";
  static final String NON_EXISTENT_INSTALLATION_ID = "nonExistentInstallationId";

  final ApplicationRepository applicationRepositoryMock = mock();
  final ApplicationImportRepository applicationImportRepositoryMock = mock();
  final AppInstallationService appInstallationServiceMock = mock();
  final AppNameValidator appNameValidator = new AppNameValidator(applicationRepositoryMock);
  final ApplicationImportValidator subject =
      new ApplicationImportValidator(
          applicationRepositoryMock,
          appInstallationServiceMock,
          applicationImportRepositoryMock,
          appNameValidator);

  {
    when(applicationRepositoryMock.existsByNameAndUserIdAndArchived(
            eq(EXISTING_APP_NAME), anyString(), eq(false)))
        .thenReturn(true);
    when(applicationRepositoryMock.existsByGithubRepositoryIdAndArchived(
            EXISTING_GH_REPO_ID, false))
        .thenReturn(true);
    when(applicationImportRepositoryMock.existsById(EXISTING_IMPORT_ID)).thenReturn(true);
    when(appInstallationServiceMock.existsById(
            valid_appImport().getGithubRepository().getInstallationId()))
        .thenReturn(true);
    when(appInstallationServiceMock.existsById(NON_EXISTENT_INSTALLATION_ID)).thenReturn(false);
  }

  @Test
  void validAppImport_should_pass() {
    var appImport = valid_appImport();

    assertDoesNotThrow(() -> subject.accept(appImport));
  }

  @Test
  void alreadyExistingAppName_cannotBe_used_toCreate_appImport() {
    var appImport = existentAppName_import();

    assertThrowsDomainBadRequestException(
        "Application with name " + EXISTING_APP_NAME + " already exists.",
        () -> subject.accept(appImport));
  }

  @Test
  void alreadyExistingGithubRepositoryId_cannotBe_used() {
    var appImport = existingGhRepoId_appImport();

    assertThrowsDomainBadRequestException(
        "Application with GitHub repository id " + EXISTING_GH_REPO_ID + " already exists.",
        () -> subject.accept(appImport));
  }

  @Test
  void nullOrBlank_githubRepositoryId_is_rejected() {
    assertThrowsDomainBadRequestException(
        "Github repository id is mandatory.", () -> subject.accept(nullGhRepoId_appImport()));

    assertThrowsDomainBadRequestException(
        "Github repository id is mandatory.", () -> subject.accept(blankGhRepoId_appImport()));
  }

  @Test
  void alreadyExistingImportId_cannotBe_used() {
    var appImport = existingImportId_appImport();

    assertThrowsDomainBadRequestException(
        "Application import with id " + EXISTING_IMPORT_ID + " already exists.",
        () -> subject.accept(appImport));
  }

  @Test
  void nullOrBlank_importId_is_rejected() {
    assertThrowsDomainBadRequestException(
        "id is mandatory.", () -> subject.accept(nullImportId_appImport()));

    assertThrowsDomainBadRequestException(
        "id is mandatory.", () -> subject.accept(blankImportId_appImport()));
  }

  @Test
  void invalidAppImport_fields_are_reported() {

    assertThrowsDomainBadRequestException(
        "Github repository is mandatory.", () -> subject.accept(noRepo_appImport()));

    assertThrowsDomainBadRequestException(
        "Github repository name is mandatory. Github repository html"
            + " url is mandatory. Github repository installationId is mandatory.",
        () -> subject.accept(invalidGhRepo_appImport()));

    assertThrowsDomainBadRequestException(
        "Github repository AppInstallation.id=" + NON_EXISTENT_INSTALLATION_ID + " not found.",
        () -> subject.accept(installation404_githubRepository_appImport()));
  }

  private static CreateApplicationImportRequestBody noRepo_appImport() {
    return valid_appImport().githubRepository(null);
  }

  private static CreateApplicationImportRequestBody valid_appImport() {
    return new CreateApplicationImportRequestBody()
        .id(randomUUID().toString())
        .name("app")
        .userId("any-user")
        .githubRepository(
            new GithubRepositoryListItem()
                .id(randomUUID().toString())
                .name("poja-app")
                .isEmpty(false)
                .defaultBranch("master")
                .htmlUrl(URI.create("https://github.com/anyuser/test-project"))
                .isPrivate(false)
                .installationId("installation_1_id"));
  }

  private static CreateApplicationImportRequestBody existentAppName_import() {
    return valid_appImport().name(EXISTING_APP_NAME);
  }

  private static CreateApplicationImportRequestBody existingGhRepoId_appImport() {
    var appImport = valid_appImport();
    var ghRepo = appImport.getGithubRepository();
    return appImport.githubRepository(ghRepo.id(EXISTING_GH_REPO_ID));
  }

  private static CreateApplicationImportRequestBody existingImportId_appImport() {
    return valid_appImport().id(EXISTING_IMPORT_ID);
  }

  private static CreateApplicationImportRequestBody nullImportId_appImport() {
    return valid_appImport().id(null);
  }

  private static CreateApplicationImportRequestBody blankImportId_appImport() {
    return valid_appImport().id("");
  }

  private static CreateApplicationImportRequestBody nullGhRepoId_appImport() {
    var appImport = valid_appImport();
    var ghRepo = appImport.getGithubRepository();
    return appImport.githubRepository(ghRepo.id(null));
  }

  private static CreateApplicationImportRequestBody blankGhRepoId_appImport() {
    var appImport = valid_appImport();
    var ghRepo = appImport.getGithubRepository();
    return appImport.githubRepository(ghRepo.id(""));
  }

  private static CreateApplicationImportRequestBody invalidGhRepo_appImport() {
    var appImport = valid_appImport();
    var ghRepo = appImport.getGithubRepository();
    return appImport.githubRepository(
        ghRepo.isEmpty(true).installationId(null).htmlUrl(null).name(null));
  }

  private static CreateApplicationImportRequestBody installation404_githubRepository_appImport() {
    var appImport = valid_appImport();
    var ghRepo = appImport.getGithubRepository();
    return appImport.githubRepository(ghRepo.installationId(NON_EXISTENT_INSTALLATION_ID));
  }
}
