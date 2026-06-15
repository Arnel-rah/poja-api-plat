package api.poja.io.endpoint.event;

import static api.poja.io.endpoint.event.utils.TestMocks.applicationCreated;
import static api.poja.io.endpoint.event.utils.TestMocks.applicationUpdated;
import static api.poja.io.endpoint.event.utils.TestMocks.setUpGithubServiceMock;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.repository.jpa.ApplicationRepository;
import api.poja.io.service.AppInstallationService;
import api.poja.io.service.event.ApplicationCrupdatedService;
import api.poja.io.service.github.GithubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ApplicationCrupdatedServiceIT extends MockedThirdParties {
  @MockBean GithubService githubService;
  @Autowired AppInstallationService installationService;
  @Autowired ApplicationRepository repository;
  @Autowired ObjectMapper om;
  @Autowired ApplicationCrupdatedService subject;

  @BeforeEach
  void setup() throws URISyntaxException {
    setUpGithubServiceMock(githubService);
  }

  @Test
  void repo_crupdate_is_triggered() {
    assertDoesNotThrow(() -> subject.accept(applicationUpdated()));
    assertDoesNotThrow(() -> subject.accept(applicationCreated()));
  }
}
