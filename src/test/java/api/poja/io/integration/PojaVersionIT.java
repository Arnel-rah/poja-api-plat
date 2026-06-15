package api.poja.io.integration;

import static api.poja.io.integration.conf.utils.TestMocks.ADMIN_TOKEN;
import static api.poja.io.integration.conf.utils.TestMocks.JOE_DOE_TOKEN;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static api.poja.io.integration.conf.utils.TestUtils.assertThrowsNotFoundException;
import static api.poja.io.integration.conf.utils.TestUtils.setUpCloudformationComponent;
import static api.poja.io.integration.conf.utils.TestUtils.setUpGithub;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.api.PojaVersionsApi;
import api.poja.io.endpoint.rest.client.ApiClient;
import api.poja.io.endpoint.rest.client.ApiException;
import api.poja.io.endpoint.rest.model.CrupdatePojaVersionChangelogRequestBody;
import api.poja.io.endpoint.rest.model.PojaVersion;
import api.poja.io.endpoint.rest.model.PojaVersionsResponse;
import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class PojaVersionIT extends MockedThirdParties {
  @MockBean private ExtendedBucketComponent extendedBucketComponentMock;

  @BeforeEach
  void setup() throws IOException {
    setUpGithub(githubComponentMock);
    setUpCloudformationComponent(cloudformationComponentMock);

    doAnswer(
            e -> {
              var version = e.getArgument(0, String.class);
              return "poja-versions/1.2.0.md".equals(version);
            })
        .when(extendedBucketComponentMock)
        .doesExist(anyString());

    when(extendedBucketComponentMock.presignGetObject(
            eq("poja-versions/1.2.0.md"), any(Duration.class)))
        .thenReturn(
            URI.create("https://my-public-bucket.s3.us-east-1.amazonaws.com/poja-1.2.0.md"));
  }

  private ApiClient anAdminApiClient() {
    return TestUtils.anApiClient(ADMIN_TOKEN, port);
  }

  private ApiClient aUserApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, port);
  }

  private PojaVersion pojaConf1() {
    var from = api.poja.io.model.PojaVersion.POJA_1;
    return createFrom(from);
  }

  private static PojaVersion createFrom(api.poja.io.model.PojaVersion from) {
    return new PojaVersion()
        .major(from.getMajor())
        .minor(from.getMinor())
        .patch(from.getPatch())
        .humanReadableValue(from.toHumanReadableValue())
        .changelogUrl(
            URI.create(
                "https://my-public-bucket.s3.us-east-1.amazonaws.com/poja-%s.md"
                    .formatted(from.toHumanReadableValue())));
  }

  @Test
  void read_all_versions_ok() throws ApiException {
    var apiClient = aUserApiClient();
    var api = new PojaVersionsApi(apiClient);

    PojaVersionsResponse pojaVersions = api.getPojaVersions();
    var data = Objects.requireNonNull(pojaVersions.getData());

    assertTrue(data.contains(pojaConf1()));
    assertTrue(
        data.stream()
            .filter(e -> !e.getHumanReadableValue().equals("1.2.0"))
            .allMatch(e -> Objects.isNull(e.getChangelogUrl())));
  }

  @SneakyThrows
  @Test
  void existing_version_canBe_crupdated() {
    var apiClient = anAdminApiClient();
    var api = new PojaVersionsApi(apiClient);

    api.crupdatePojaVersionChangelog(
        "3.0.0", new CrupdatePojaVersionChangelogRequestBody().changelogMd("ito"));
  }

  @Test
  void nonExistent_version_cannotBe_crupdated() {
    var apiClient = anAdminApiClient();
    var api = new PojaVersionsApi(apiClient);

    assertThrowsNotFoundException(
        () ->
            api.crupdatePojaVersionChangelog(
                "100.0.0", new CrupdatePojaVersionChangelogRequestBody().changelogMd("ito")),
        "Poja version 100.0.0 not found.");
  }

  @Test
  void nonAdmin_cannot_crupdate_pojaVersion() {
    var apiClient = aUserApiClient();
    var api = new PojaVersionsApi(apiClient);

    assertThrowsForbiddenException(
        () ->
            api.crupdatePojaVersionChangelog(
                "1.2.0", new CrupdatePojaVersionChangelogRequestBody().changelogMd("ito")),
        "Access Denied");
  }
}
