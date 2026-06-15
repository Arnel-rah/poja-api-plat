package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.HELLO_WORLD_TEMPLATE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.THYMELEAF_TEMPLATE_ID;
import static api.poja.io.integration.conf.utils.TestMocks.applicationTemplates;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf1;
import static api.poja.io.integration.conf.utils.TestMocks.helloWorldTemplate;
import static api.poja.io.integration.conf.utils.TestMocks.thymeleafTemplate;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsNotFoundException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.ApplicationApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.integration.conf.utils.TestUtils;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

class ApplicationTemplateIT extends MockedThirdParties {

  @MockBean private ExtendedBucketComponent extendedBucketComponent;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    setUpGithub(githubComponentMock);

    doReturn(new ClassPathResource("files/poja_1.yml").getFile())
        .when(extendedBucketComponent)
        .download(String.format("templates/%s/config.yml", HELLO_WORLD_TEMPLATE_ID));
  }

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  @SneakyThrows
  @Test
  void applicationTemplates_canBe_retrieved() {
    var api = new ApplicationApi(anApiClient());

    var data = api.getApplicationTemplates().getData();
    assertEquals(applicationTemplates(), data);
    assertEquals(2, data.size());
  }

  @SneakyThrows
  @Test
  void applicationTemplate_canBe_retrieved_by_id() {
    var api = new ApplicationApi(anApiClient());

    var hw = api.getApplicationTemplateById(HELLO_WORLD_TEMPLATE_ID);
    var thy = api.getApplicationTemplateById(THYMELEAF_TEMPLATE_ID);

    assertEquals(helloWorldTemplate(), hw);
    assertEquals(thymeleafTemplate(), thy);
  }

  @SneakyThrows
  @Test
  void nonexistent_applicationTemplate_cannotBe_retrieved() {
    var api = new ApplicationApi(anApiClient());
    var nonexistentId = randomUUID().toString();

    assertThrowsNotFoundException(
        () -> api.getApplicationTemplateById(nonexistentId),
        "ApplicationTemplate with id " + nonexistentId + " not found");
  }

  @SneakyThrows
  @Test
  void applicationTemplateConfig_canBe_read() {
    var api = new ApplicationApi(anApiClient());
    var expected = new OneOfPojaConf(getValidPojaConf1());

    var actual = api.getApplicationTemplateConfigById(HELLO_WORLD_TEMPLATE_ID);

    assertEquals(expected, actual);
  }

  @SneakyThrows
  @Test
  void applicationTemplateWithNoCustomConfig_config_cannotBe_retrieved() {
    var api = new ApplicationApi(anApiClient());

    assertThrowsBadRequestException(
        () -> api.getApplicationTemplateConfigById(THYMELEAF_TEMPLATE_ID),
        "Template.id=" + THYMELEAF_TEMPLATE_ID + " has no custom config.");
  }
}
