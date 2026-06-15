package api.poja.io.integration;

import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.HTTP_API;
import static api.poja.io.endpoint.rest.model.EnvironmentType.PROD;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf6;
import static api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency.BASIC_USER_CONCURRENCY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.GET;

import api.poja.io.conf.FacadeIT;
import api.poja.io.endpoint.rest.security.github.GithubComponent;
import api.poja.io.service.appEnvConfigurer.mapper.PojaConfMapper;
import api.poja.io.service.github.model.GhSecret;
import api.poja.io.service.jwt.JwtGenerator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Disabled
@Slf4j
@SpringBootTest(properties = {"github.appid=123456", "github.api.baseuri=https://api.github.com"})
public class ConfRepositorySecretsIT extends FacadeIT {

  @Autowired PojaConfMapper pojaConfMapper;

  @Autowired private GithubComponent githubComponent;

  @MockBean private JwtGenerator jwtGenerator;

  @MockBean(name = "appPemLoader")
  private Object appPemLoader;

  @Value("${github.token:dummy}")
  private String token;

  private final String owner = "TiavinaNath";
  private final String repo = "secrets-test";
  public static final String USER_ID = "9f7332d1-778b-425b-828f-b165660259f5";

  @Test
  void configureSecrets_fromPojaConf6_realGithub_ok() {
    var pojaConf6 =
        pojaConfMapper.toDomainPojaConf6(
            getValidPojaConf6(HTTP_API), BASIC_USER_CONCURRENCY, USER_ID);

    var envVars = pojaConf6.general().customJavaEnvVars();
    var environmentType = PROD;

    assertNotNull(envVars);
    assertFalse(envVars.isEmpty());
    envVars.stream()
        .filter(envVar -> envVar.getTestValue() != null)
        .map(
            envVar -> new GhSecret(environmentType + "_" + envVar.getName(), envVar.getTestValue()))
        .forEach(
            ghSecret -> {
              log.info("Creating secret {} in {}/{}", ghSecret.secretName(), owner, repo);
              githubComponent.crupdateSecret(owner, repo, token, ghSecret);

              OffsetDateTime createdAt = fetchCreatedAt(ghSecret.secretName());
              assertNotNull(
                  createdAt, "created_at should not be null for " + ghSecret.secretName());
              log.info("Secret {} created at {}", ghSecret.secretName(), createdAt);
            });
  }

  private OffsetDateTime fetchCreatedAt(String secretName) {
    var restTemplate = new RestTemplate();
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github.v3+json")));
    var entity = new HttpEntity<>(headers);

    var getUrl =
        String.format(
            "https://api.github.com/repos/%s/%s/actions/secrets/%s", owner, repo, secretName);

    ResponseEntity<Map<String, Object>> response =
        restTemplate.exchange(getUrl, GET, entity, new ParameterizedTypeReference<>() {});

    var responseBody = response.getBody();
    if (responseBody != null && responseBody.containsKey("created_at")) {
      return OffsetDateTime.parse(responseBody.get("created_at").toString());
    }
    return null;
  }
}
