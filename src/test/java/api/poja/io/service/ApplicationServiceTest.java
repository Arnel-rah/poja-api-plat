package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PREPROD;
import static api.poja.io.integration.conf.utils.TestMocks.HELLO_WORLD_TEMPLATE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_EMAIL;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_GITHUB_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_MAIN_ORG_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_STRIPE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.NOOBIE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.ORG_1_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_GITHUB_REPOSITORY_WITHOUT_ID;
import static api.poja.io.integration.conf.utils.TestMocks.POJA_APPLICATION_NAME;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf3;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_FAILED;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_IN_PROGRESS;
import static api.poja.io.repository.model.enums.AppSetupStateEnum.ENV_CREATION_SUCCESS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.model.ApplicationBase;
import api.poja.io.endpoint.rest.model.CloneApplicationTemplateRequestBody;
import api.poja.io.endpoint.rest.model.CloneTemplateApplication;
import api.poja.io.endpoint.rest.model.CrupdateEnvironment;
import api.poja.io.endpoint.rest.model.EnvConf;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.model.User;
import api.poja.io.model.exception.ApiException;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.EnvDeploymentConf;
import api.poja.io.repository.model.Environment;
import api.poja.io.repository.model.Organization;
import api.poja.io.service.organization.OrganizationService;
import api.poja.io.service.validator.UserAppThresholdValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class ApplicationServiceTest extends MockedThirdParties {
  @MockBean private EnvironmentService environmentService;
  @MockBean private EnvDeploymentConfService envDeploymentConfService;
  @MockBean private ExtendedBucketComponent bucketComponent;
  @MockBean private UserService userService;
  @MockBean private OrganizationService organizationService;
  @MockBean private ApplicationRepository applicationRepository;
  @MockBean private AppSetupStateService appSetupStateService;
  @MockBean private UserAppThresholdValidator appThresholdValidator;
  @Autowired private ApplicationService subject;

  private static final String BUCKET_KEY = "orgs/org_id/apps/app_id/envs/conf_id/poja-files/";
  private static final String ORG_ID = "org_id";
  private static final String APP_ID = "app_id";
  private static final String CONF_ID = "conf_id";
  private static final String ENV_ID = "env_id";

  @Test
  void should_call_bucketComponent_deleteFile() {
    when(environmentService.getByApplicationId(APP_ID)).thenReturn(environmentCreated());
    when(envDeploymentConfService.getByAppEnvDeplId(CONF_ID))
        .thenReturn(envDeploymentConfCreated());

    subject.deleteAppEnvDeplByAppId(ORG_ID, APP_ID);

    verify(bucketComponent).deleteFile(BUCKET_KEY);
  }

  @Test
  void should_throw_illegal_state_exception_when_createAppEnv_fails() {
    doNothing().when(appThresholdValidator).accept(any(), any());
    when(userService.getUserById(JOE_DOE_ID)).thenReturn(user());
    when(organizationService.getById(ORG_1_ID)).thenReturn(org());
    when(applicationRepository.save(any(Application.class))).thenReturn(appCreated());
    when(environmentService.createAndConfigureEnv(
            eq(ORG_1_ID),
            any(Application.class),
            any(Environment.class),
            any(EnvConf.class),
            anyBoolean()))
        .thenThrow(new IllegalStateException("error"));

    var exception =
        assertThrows(
            ApiException.class,
            () -> subject.createAndDeployApp(ORG_1_ID, app(), env(), envConf()));
    var verifier = inOrder(appSetupStateService);

    assertEquals("Failed to set up the application", exception.getMessage());
    verifier
        .verify(appSetupStateService)
        .save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_IN_PROGRESS));
    verifier.verify(appSetupStateService).save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_FAILED));
  }

  @Test
  void createAndDeploy_should_not_throw_exception_when_createAppEnv_succeeds() {
    doNothing().when(appThresholdValidator).accept(any(), any());
    when(userService.getUserById(JOE_DOE_ID)).thenReturn(user());
    when(organizationService.getById(ORG_1_ID)).thenReturn(org());
    when(applicationRepository.save(any(Application.class))).thenReturn(appCreated());
    when(environmentService.createAndConfigureEnv(
            eq(ORG_1_ID),
            any(Application.class),
            any(Environment.class),
            any(EnvConf.class),
            anyBoolean()))
        .thenReturn(environmentCreated());
    var verifier = inOrder(appSetupStateService);

    assertDoesNotThrow(() -> subject.createAndDeployApp(ORG_1_ID, app(), env(), envConf()));
    verifier
        .verify(appSetupStateService)
        .save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_IN_PROGRESS));
    verifier.verify(appSetupStateService).save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_SUCCESS));
  }

  @Test
  void cloneTemplate_should_save_envCreationFailed_when_createAppEnv_fails() {
    final String appId = randomUUID().toString();

    doNothing().when(appThresholdValidator).accept(any(), any());
    when(userService.getUserById(JOE_DOE_ID)).thenReturn(user());
    when(organizationService.getById(ORG_1_ID)).thenReturn(org());
    when(applicationRepository.save(any(Application.class))).thenReturn(appCreated());
    when(environmentService.createAndConfigureEnv(
            eq(ORG_1_ID),
            any(Application.class),
            any(Environment.class),
            any(EnvConf.class),
            anyBoolean()))
        .thenThrow(new IllegalStateException("error"));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                subject.cloneTemplate(
                    ORG_1_ID,
                    HELLO_WORLD_TEMPLATE_ID,
                    new CloneApplicationTemplateRequestBody()
                        .envConf(envConf())
                        .environment(env())
                        .application(
                            new CloneTemplateApplication()
                                .id(APP_ID)
                                .name("honeypot")
                                .installationId("installation_1_id")
                                .userId(JOE_DOE_ID)
                                .isPrivate(false)
                                .repositoryName("honeypot"))));
    assertEquals("Failed to set up the application", exception.getMessage());
    var verifier = inOrder(appSetupStateService);

    verifier
        .verify(appSetupStateService)
        .save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_IN_PROGRESS));
    verifier.verify(appSetupStateService).save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_FAILED));
  }

  @Test
  void cloneTemplate_should_not_throw_exception_when_createApEnv_succeeds() {
    doNothing().when(appThresholdValidator).accept(any(), any());
    when(userService.getUserById(JOE_DOE_ID)).thenReturn(user());
    when(organizationService.getById(ORG_1_ID)).thenReturn(org());
    when(applicationRepository.save(any(Application.class))).thenReturn(appCreated());
    when(environmentService.createAndConfigureEnv(
            eq(ORG_1_ID),
            any(Application.class),
            any(Environment.class),
            any(EnvConf.class),
            anyBoolean()))
        .thenReturn(environmentCreated());
    var verifier = inOrder(appSetupStateService);

    assertDoesNotThrow(
        () ->
            subject.cloneTemplate(
                ORG_1_ID,
                HELLO_WORLD_TEMPLATE_ID,
                new CloneApplicationTemplateRequestBody()
                    .envConf(envConf())
                    .environment(env())
                    .application(
                        new CloneTemplateApplication()
                            .id(randomUUID().toString())
                            .name("honeypot")
                            .installationId("installation_1_id")
                            .userId(JOE_DOE_ID)
                            .isPrivate(false)
                            .repositoryName("honeypot"))));

    verifier
        .verify(appSetupStateService)
        .save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_IN_PROGRESS));
    verifier.verify(appSetupStateService).save(eq(ORG_1_ID), eq(APP_ID), eq(ENV_CREATION_SUCCESS));
  }

  private static ApplicationBase app() {
    return new ApplicationBase()
        .id(APP_ID)
        .name(POJA_APPLICATION_NAME + "-55")
        .githubRepository(POJA_APPLICATION_GITHUB_REPOSITORY_WITHOUT_ID)
        .userId(NOOBIE_ID)
        .orgId(ORG_1_ID)
        .archived(false);
  }

  private static User user() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email(JOE_DOE_EMAIL)
        .username("JoeDoe")
        .firstName("Joe")
        .lastName("Doe")
        .githubId(JOE_DOE_GITHUB_ID)
        .stripeId(JOE_DOE_STRIPE_ID)
        .build();
  }

  private static Organization org() {
    return Organization.builder().id(ORG_1_ID).name("org_1_name").ownerId(JOE_DOE_ID).build();
  }

  private static CrupdateEnvironment env() {
    return new CrupdateEnvironment().id(ENV_ID).environmentType(PREPROD).archived(false);
  }

  private static EnvConf envConf() {
    return new EnvConf().id(CONF_ID).conf(new OneOfPojaConf(getValidPojaConf3(LAMBDA_URL)));
  }

  private static Application appCreated() {
    return Application.builder()
        .id(APP_ID)
        .name(POJA_APPLICATION_NAME + "-55")
        .orgId(JOE_DOE_MAIN_ORG_ID)
        .archived(false)
        .userId(JOE_DOE_ID)
        .build();
  }

  private static Environment environmentCreated() {
    return Environment.builder().id(ENV_ID).applicationId(APP_ID).currentConfId(CONF_ID).build();
  }

  private static EnvDeploymentConf envDeploymentConfCreated() {
    return EnvDeploymentConf.builder().id(CONF_ID).build();
  }
}
