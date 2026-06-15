package api.poja.io.endpoint.event;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.event.model.ApplicationCreationFailed;
import api.poja.io.service.ApplicationService;
import api.poja.io.service.event.ApplicationCreationFailedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ApplicationCreationFailedServiceTest extends MockedThirdParties {
  @MockBean private ApplicationService applicationService;
  @Autowired private ApplicationCreationFailedService subject;

  @Test
  void should_call_delete_app_env_depl() {
    subject.accept(applicationCreationFailed());
    verify(applicationService, times(1)).deleteAppEnvDeplByAppId("org_id", "app_id");
  }

  private static ApplicationCreationFailed applicationCreationFailed() {
    return ApplicationCreationFailed.builder().orgId("org_id").appId("app_id").build();
  }
}
