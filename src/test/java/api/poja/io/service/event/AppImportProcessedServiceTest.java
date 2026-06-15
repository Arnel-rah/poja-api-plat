package api.poja.io.service.event;

import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_SUCCESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_DEPLOYMENT_INITIATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_IN_PROGRESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.APPLICATION_SET_UP_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStatus.SUCCESSFUL;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.event.model.AppImportProcessed;
import api.poja.io.endpoint.rest.mapper.EnvironmentMapper;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.Environment;
import api.poja.io.repository.model.mapper.ApplicationMapper;
import api.poja.io.service.AppSetupStateService;
import api.poja.io.service.ApplicationImportService;
import api.poja.io.service.ApplicationImportStateService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfFileMapper;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class AppImportProcessedServiceTest {

  static final String FAILED_APP_IMPORT_ID = "failed_application_import_id";
  static final String ALREADY_SET_UP_APP_IMPORT_ID = "already_set_up_application_import_id";

  static final String PROCESSED_APP_IMPORT_ID = "processed_application_import_id";
  static final String PROCESSED_APP_ID = "processed_application_id";
  static final String PROCESSED_APP_IMPORT_ENV_ID = "processed_application_import_env_id";

  static final String TO_FAIL_APP_IMPORT_ID = "to_fail_application_import_id";
  static final String TO_FAIL_APP_ID = "to_fail_application_id";
  static final String TO_FAIL_APP_IMPORT_ENV_ID = "to_fail_application_import_env_id";

  static final ApplicationMapper applicationMapper = new ApplicationMapper();
  static final EnvironmentMapper environmentMapper = new EnvironmentMapper();

  final ApplicationImportService applicationImportServiceMock = mock();
  final ApplicationImportStateService applicationImportStateServiceMock = mock();
  final EnvironmentService environmentServiceMock = mock();
  final ApplicationService applicationServiceMock = mock();
  final AppSetupStateService applicationStateServiceMock = mock();
  final PojaConfFileMapper pojaConfFileMapperMock = mock();

  final AppImportProcessedService subject =
      new AppImportProcessedService(
          applicationImportServiceMock,
          applicationImportStateServiceMock,
          environmentServiceMock,
          applicationServiceMock,
          applicationStateServiceMock,
          pojaConfFileMapperMock,
          environmentMapper);

  {
    when(applicationImportServiceMock.findById(FAILED_APP_IMPORT_ID))
        .thenReturn(Optional.of(failed_applicationImport()));
    when(applicationImportServiceMock.findById(PROCESSED_APP_IMPORT_ID))
        .thenReturn(Optional.of(processed_applicationImport()));
    when(applicationImportServiceMock.findById(ALREADY_SET_UP_APP_IMPORT_ID))
        .thenReturn(Optional.of(alreadySetUp_applicationImport()));
    when(applicationImportServiceMock.findById(TO_FAIL_APP_IMPORT_ID))
        .thenReturn(Optional.of(toFail_applicationImport()));

    when(applicationServiceMock.findByImportId(PROCESSED_APP_IMPORT_ID))
        .thenReturn(Optional.of(processed_application()));
    when(applicationServiceMock.findByImportId(TO_FAIL_APP_IMPORT_ID))
        .thenReturn(Optional.of(toFail_application()));
    when(applicationServiceMock.findByImportId(FAILED_APP_IMPORT_ID)).thenReturn(Optional.empty());
    when(applicationServiceMock.findByImportId(ALREADY_SET_UP_APP_IMPORT_ID))
        .thenReturn(Optional.empty());

    when(environmentServiceMock.createAndConfigureEnv(
            eq(ORG_1_ID),
            argThat(e -> PROCESSED_APP_ID.equals(e.getId())),
            any(),
            any(),
            eq(false)))
        .thenReturn(processed_applicationEnvironment());

    when(environmentServiceMock.createAndConfigureEnv(
            eq(ORG_1_ID), argThat(e -> TO_FAIL_APP_ID.equals(e.getId())), any(), any(), eq(false)))
        .thenReturn(toFail_applicationEnvironment());

    when(environmentServiceMock.deployEnvWithConf(
            eq(ORG_1_ID),
            eq(TO_FAIL_APP_ID),
            eq(TO_FAIL_APP_IMPORT_ENV_ID),
            eq("import-" + TO_FAIL_APP_IMPORT_ID + "-main"),
            any()))
        .thenThrow(IllegalStateException.class);
  }

  @Test
  void envDeploymentInitiationFailedState_shouldBe_saved_when_environmentDeployment_fails() {
    var importId = TO_FAIL_APP_IMPORT_ID;
    var applicationId = TO_FAIL_APP_ID;
    var environmentId = TO_FAIL_APP_IMPORT_ENV_ID;
    var application = toFail_application();
    var event = new AppImportProcessed(ORG_1_ID, importId);

    subject.accept(event);

    verify(applicationImportServiceMock, times(1)).findById(importId);
    verify(applicationServiceMock, times(1)).findByImportId(importId);
    verify(applicationImportStateServiceMock, times(1))
        .updateState(importId, APPLICATION_SET_UP_IN_PROGRESS);

    verify(applicationStateServiceMock, times(1))
        .save(ORG_1_ID, applicationId, ENV_CREATION_IN_PROGRESS);
    verify(applicationImportServiceMock, times(1)).downloadAppImportPojaConf(ORG_1_ID, importId);
    verify(environmentServiceMock, times(1))
        .createAndConfigureEnv(
            eq(ORG_1_ID),
            argThat(e -> application.getId().equals(e.getId())),
            argThat(e -> PREPROD.equals(e.getEnvironmentType())),
            any(),
            eq(false));
    verify(applicationStateServiceMock, times(1))
        .saveAll(
            ORG_1_ID,
            applicationId,
            List.of(ENV_CREATION_SUCCESS, ENV_DEPLOYMENT_INITIATION_IN_PROGRESS));
    verify(environmentServiceMock, times(1))
        .deployEnvWithConf(
            eq(ORG_1_ID),
            eq(applicationId),
            eq(environmentId),
            eq("import-" + importId + "-main"),
            any());
    verify(applicationStateServiceMock, times(1))
        .save(ORG_1_ID, applicationId, ENV_DEPLOYMENT_INITIATION_FAILED);
    verify(applicationImportStateServiceMock, times(1))
        .updateState(importId, APPLICATION_SET_UP_FAILED);
    verifyNoMoreInteractions(
        applicationImportServiceMock,
        environmentServiceMock,
        applicationServiceMock,
        applicationStateServiceMock);
  }

  @Test
  void nonExistentApplicationImportId_shouldBe_skipped() {
    var applicationImportId = "NonExistentApplicationImportId";
    var event = new AppImportProcessed(ORG_1_ID, applicationImportId);

    subject.accept(event);

    verify(applicationImportServiceMock, times(1)).findById(applicationImportId);
    verifyNoMoreInteractions(
        applicationImportServiceMock,
        environmentServiceMock,
        applicationServiceMock,
        applicationStateServiceMock);
  }

  @Test
  void applicationSetUp_shouldBe_skipped_when_createdAppId_exists() {
    var event = new AppImportProcessed(ORG_1_ID, ALREADY_SET_UP_APP_IMPORT_ID);

    subject.accept(event);

    verify(applicationImportServiceMock, times(1)).findById(ALREADY_SET_UP_APP_IMPORT_ID);
    verify(applicationServiceMock, times(1)).findByImportId(ALREADY_SET_UP_APP_IMPORT_ID);
    verifyNoMoreInteractions(
        applicationImportServiceMock,
        environmentServiceMock,
        applicationServiceMock,
        applicationStateServiceMock);
  }

  @Test
  void applicationSetup_shouldBe_skipped_when_applicationStatus_isFailed() {
    var event = new AppImportProcessed(ORG_1_ID, FAILED_APP_IMPORT_ID);

    subject.accept(event);

    verify(applicationImportServiceMock, times(1)).findById(FAILED_APP_IMPORT_ID);
    verify(applicationServiceMock, times(1)).findByImportId(FAILED_APP_IMPORT_ID);
    verifyNoMoreInteractions(
        applicationImportServiceMock,
        environmentServiceMock,
        applicationServiceMock,
        applicationStateServiceMock);
  }

  @Test
  void applicationId_shouldBe_updated_when_triggered() {
    var importId = PROCESSED_APP_IMPORT_ID;
    var applicationId = PROCESSED_APP_ID;
    var environmentId = PROCESSED_APP_IMPORT_ENV_ID;
    var application = processed_application();
    var event = new AppImportProcessed(ORG_1_ID, importId);

    subject.accept(event);

    verify(applicationImportServiceMock, times(1)).findById(importId);
    verify(applicationServiceMock, times(1)).findByImportId(importId);
    verify(applicationImportStateServiceMock, times(1))
        .updateState(importId, APPLICATION_SET_UP_IN_PROGRESS);

    verify(applicationStateServiceMock, times(1))
        .save(ORG_1_ID, applicationId, ENV_CREATION_IN_PROGRESS);
    verify(applicationImportServiceMock, times(1)).downloadAppImportPojaConf(ORG_1_ID, importId);
    verify(environmentServiceMock, times(1))
        .createAndConfigureEnv(
            eq(ORG_1_ID),
            argThat(e -> application.getName().equals(e.getName())),
            argThat(e -> PREPROD.equals(e.getEnvironmentType())),
            any(),
            eq(false));
    verify(applicationStateServiceMock, times(1))
        .saveAll(
            ORG_1_ID,
            applicationId,
            List.of(ENV_CREATION_SUCCESS, ENV_DEPLOYMENT_INITIATION_IN_PROGRESS));
    verify(environmentServiceMock, times(1))
        .deployEnvWithConf(
            eq(ORG_1_ID),
            eq(applicationId),
            eq(environmentId),
            eq("import-" + importId + "-main"),
            any());
    verify(applicationImportStateServiceMock, times(1))
        .updateState(importId, APPLICATION_SET_UP_SUCCESSFUL);
    verify(applicationStateServiceMock, times(1))
        .save(ORG_1_ID, applicationId, ENV_DEPLOYMENT_INITIATED);
    verifyNoMoreInteractions(
        applicationImportServiceMock,
        environmentServiceMock,
        applicationServiceMock,
        applicationStateServiceMock);
  }

  static ApplicationImport anApplicationImport(String applicationName) {
    var userId = "anyUser_id";
    return ApplicationImport.builder()
        .id(null)
        .status(null)
        .appName(applicationName)
        .userId(userId)
        .orgId(ORG_1_ID)
        .githubRepositoryName(applicationName)
        .githubRepositoryHttpUrl(String.join("/", "https://github.com", userId, applicationName))
        .githubRepositoryId(randomUUID().toString())
        .githubRepositoryDescription("dummy description")
        .githubRepositoryPrivate(false)
        .appInstallationId(randomUUID().toString())
        .build();
  }

  static ApplicationImport failed_applicationImport() {
    return anApplicationImport("application_name").toBuilder()
        .id(FAILED_APP_IMPORT_ID)
        .status(FAILED)
        .build();
  }

  static ApplicationImport alreadySetUp_applicationImport() {
    return anApplicationImport("already_set_up_application_name").toBuilder()
        .id(ALREADY_SET_UP_APP_IMPORT_ID)
        .createdAppId("an_application_id")
        .status(SUCCESSFUL)
        .build();
  }

  static ApplicationImport toFail_applicationImport() {
    return anApplicationImport("toFail_application_name").toBuilder()
        .id(TO_FAIL_APP_IMPORT_ID)
        .status(SUCCESSFUL)
        .build();
  }

  static Application toFail_application() {
    return applicationMapper.toDomain(toFail_applicationImport(), TO_FAIL_APP_ID);
  }

  static Environment toFail_applicationEnvironment() {
    var environment =
        new CrupdateEnvironment()
            .id(TO_FAIL_APP_IMPORT_ENV_ID)
            .archived(false)
            .environmentType(PREPROD);
    return environmentMapper.toDomain(TO_FAIL_APP_ID, environment);
  }

  static ApplicationImport processed_applicationImport() {
    return anApplicationImport("processed_application_name").toBuilder()
        .id(PROCESSED_APP_IMPORT_ID)
        .status(SUCCESSFUL)
        .build();
  }

  static Application processed_application() {
    return applicationMapper.toDomain(processed_applicationImport(), PROCESSED_APP_ID);
  }

  static Environment processed_applicationEnvironment() {
    var environment =
        new CrupdateEnvironment()
            .id(PROCESSED_APP_IMPORT_ENV_ID)
            .archived(false)
            .environmentType(PREPROD);
    return environmentMapper.toDomain(PROCESSED_APP_ID, environment);
  }

  // todo: test ENV set up error handling
}
