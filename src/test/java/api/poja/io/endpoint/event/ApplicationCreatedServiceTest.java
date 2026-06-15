package api.poja.io.endpoint.event;

import static api.poja.io.endpoint.event.utils.TestMocks.createRepoResponse;
import static java.time.Instant.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.ApplicationCreated;
import api.poja.io.endpoint.event.model.EnvironmentDeploymentRequested;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.repository.model.AppInstallation;
import api.poja.io.repository.model.Application;
import api.poja.io.repository.model.Environment;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.AppSetupStateService;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.EnvironmentService;
import api.poja.io.service.event.ApplicationCreatedService;
import api.poja.io.service.github.GithubService;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ApplicationCreatedServiceTest extends MockedThirdParties {
  @MockBean private ApplicationService applicationService;
  @MockBean private EnvironmentService environmentService;
  @MockBean private ApplicationRepository applicationRepository;
  @MockBean private GithubService githubService;
  @MockBean private AppInstallationService installationService;
  @MockBean private AppSetupStateService stateService;
  @Autowired private ApplicationCreatedService subject;

  private static final String ORG_ID = "org_id";
  private static final String APP_ID = "app_id";
  private static final String INSTALLATION_ID = "installation_id";
  private static final String ENV_ID = "env_id";
  private static final String CONF_ID = "conf_id";
  private static final String APP_REPO_NAME = "app_repo_name";
  private static final String REPOSITORY_URL = "repository_url";
  private static final String OWNER_ID = "owner_id";

  @Test
  void repo_creation_and_env_deployment_request_is_triggered() throws URISyntaxException {
    when(applicationService.getById(APP_ID, ORG_ID)).thenReturn(application());
    when(installationService.getById(INSTALLATION_ID)).thenReturn(appInstallation());
    when(environmentService.getByApplicationId(APP_ID)).thenReturn(environmentCreated());
    when(githubService.createRepoFor(any(), any())).thenReturn(createRepoResponse());
    when(applicationService.getById(APP_ID)).thenReturn(application());

    subject.accept(applicationCreated());
    verify(applicationRepository)
        .updateApplicationRepoUrl(
            APP_ID, String.valueOf(createRepoResponse().htmlUrl()), createRepoResponse().id());

    var expected = new EnvironmentDeploymentRequested(ORG_ID, APP_ID, ENV_ID, CONF_ID);
    verify(eventProducerMock).accept(argThat(events -> events.equals(List.of(expected))));
  }

  private static ApplicationCreated applicationCreated() {
    return ApplicationCreated.builder()
        .orgId(ORG_ID)
        .appId(APP_ID)
        .installationId(INSTALLATION_ID)
        .repoPrivate(true)
        .appRepoName(APP_REPO_NAME)
        .description("")
        .importId(null)
        .build();
  }

  private static Application application() {
    return Application.builder()
        .id(APP_ID)
        .installationId(INSTALLATION_ID)
        .orgId(ORG_ID)
        .creationDatetime(now())
        .environments(List.of(new Environment()))
        .githubRepositoryUrl(REPOSITORY_URL)
        .build();
  }

  private static AppInstallation appInstallation() {
    return AppInstallation.builder()
        .id(INSTALLATION_ID)
        .ghId(123L)
        .ownerGithubLogin(OWNER_ID)
        .build();
  }

  private static Environment environmentCreated() {
    return Environment.builder().id(ENV_ID).currentConfId(CONF_ID).build();
  }
}
