package api.poja.io.endpoint.rest.security.github;

import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import api.poja.io.endpoint.rest.model.Token;
import api.poja.io.model.BoundedPageSize;
import api.poja.io.model.PageFromOne;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.service.github.model.CreateRepoFromTemplateRequestBody;
import api.poja.io.service.github.model.CreateRepoRequestBody;
import api.poja.io.service.github.model.CreateRepoResponse;
import api.poja.io.service.github.model.GhAppInstallation;
import api.poja.io.service.github.model.GhAppInstallation.RepositorySelection;
import api.poja.io.service.github.model.GhAppInstallationRepositoriesResponse;
import api.poja.io.service.github.model.GhAppInstallationResponse;
import api.poja.io.service.github.model.GhGetCommitResponse;
import api.poja.io.service.github.model.GhListAppInstallationReposResponse;
import api.poja.io.service.github.model.GhPublicKey;
import api.poja.io.service.github.model.GhSearchReposResponse;
import api.poja.io.service.github.model.GhSecret;
import api.poja.io.service.github.model.GhWorkflowRunRequestBody;
import api.poja.io.service.github.model.UpdateRepoRequestBody;
import api.poja.io.service.github.model.UpdateRepoResponse;
import api.poja.io.service.github.model.exception.WorkflowRunFailed;
import api.poja.io.service.jwt.JwtGenerator;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class GithubComponent {
  private final GithubConf conf;
  private final RestTemplate restTemplate;
  private final UriComponents githubApiBaseUri;
  private final JwtGenerator jwtGenerator;
  private final int githubAppId;
  private final GithubSecretEncryptor githubSecretEncryptor;

  public GithubComponent(
      GithubConf conf,
      RestTemplate restTemplate,
      @Value("${github.api.baseuri}") String githubApiBaseUri,
      JwtGenerator jwtGenerator,
      @Value("${github.appid}") int githubAppId,
      GithubSecretEncryptor githubSecretEncryptor) {
    this.conf = conf;
    this.restTemplate = restTemplate;
    this.githubApiBaseUri = UriComponentsBuilder.fromHttpUrl(githubApiBaseUri).build();
    this.jwtGenerator = jwtGenerator;
    this.githubAppId = githubAppId;
    this.githubSecretEncryptor = githubSecretEncryptor;
  }

  public Optional<String> getGithubUserId(String token) {
    try {
      GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
      return Optional.of(String.valueOf(gitHub.getMyself().getId()));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public Optional<GHMyself> getCurrentUserByToken(String token) {
    try {
      GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
      return Optional.of(gitHub.getMyself());
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public Token exchangeCodeToToken(String code) {
    HttpHeaders headers = getHttpHeaders();

    Map<String, String> body = getBody();
    body.put("code", code);

    var responseBody = sendTokenRequest(body, headers);
    if (responseBody != null && !responseBody.containsKey("error")) {
      return extractToken(responseBody);
    }
    assert responseBody != null;
    throw new BadRequestException((String) responseBody.get("error_description"));
  }

  private Map<String, String> getBody() {
    Map<String, String> body = new HashMap<>();
    body.put("client_id", conf.getClientId());
    body.put("client_secret", conf.getClientSecret());
    body.put("redirect_uri", conf.getRedirectUri());
    return body;
  }

  private static HttpHeaders getHttpHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    headers.setAccept(List.of(APPLICATION_JSON));
    return headers;
  }

  public Token refreshToken(String tokenToRefresh) {
    HttpHeaders headers = getHttpHeaders();

    Map<String, String> body = getBody();
    body.put("grant_type", "refresh_token");
    body.put("refresh_token", tokenToRefresh);

    var responseBody = sendTokenRequest(body, headers);

    if (responseBody != null && !responseBody.containsKey("error")) {
      return extractToken(responseBody);
    }
    assert responseBody != null;
    throw new BadRequestException((String) responseBody.get("error_description"));
  }

  public GhListAppInstallationReposResponse listInstallationRepositories(
      long installationId, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    var jwtToken = getAppInstallationToken(installationId, Duration.ofMinutes(1));
    HttpHeaders headers = getGithubHttpHeaders(jwtToken);
    HttpEntity<GhListAppInstallationReposResponse> entity = new HttpEntity<>(headers);

    ParameterizedTypeReference<GhListAppInstallationReposResponse> typeRef =
        new ParameterizedTypeReference<>() {};
    var response =
        restTemplate
            .exchange(
                getListAppInstallationRepositoriesUri(pageFromOne, boundedPageSize).toUriString(),
                GET,
                entity,
                typeRef)
            .getBody();

    return response;
  }

  public GhSearchReposResponse searchReposWithToken(
      String token, String q, PageFromOne pageFromOne, BoundedPageSize boundedPageSize) {
    HttpHeaders headers = getGithubHttpHeaders(token);
    var response =
        restTemplate.exchange(
            getSearchRepositoriesUri(q, pageFromOne, boundedPageSize).toUriString(),
            GET,
            new HttpEntity<>(headers),
            GhSearchReposResponse.class);

    return response.getBody();
  }

  private static Token extractToken(Map<String, Object> responseBody) {
    String accessToken = (String) responseBody.get("access_token");
    String tokenType = (String) responseBody.get("token_type");
    String refreshToken = (String) responseBody.get("refresh_token");
    return new Token().accessToken(accessToken).refreshToken(refreshToken).tokenType(tokenType);
  }

  private Map<String, Object> sendTokenRequest(Map<String, String> body, HttpHeaders headers) {
    HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
    ParameterizedTypeReference<Map<String, Object>> typeReference =
        new ParameterizedTypeReference<>() {};
    var response = restTemplate.exchange(conf.getTokenUrl(), POST, entity, typeReference);
    var responseBody = response.getBody();
    return responseBody;
  }

  public Optional<String> getRepositoryIdByAppToken(String token) {
    HttpHeaders headers = getGithubHttpHeaders(token);
    HttpEntity<GhAppInstallationRepositoriesResponse> entity = new HttpEntity<>(headers);
    try {
      var response =
          restTemplate.exchange(
              getAppInstallationRepositoriesUri().toUriString(),
              GET,
              entity,
              GhAppInstallationRepositoriesResponse.class);
      var responseBody = response.getBody();

      if (responseBody == null || response.getStatusCode().is4xxClientError()) {
        return Optional.empty();
      }
      log.info("Installation repositories: {}", responseBody);
      return Optional.of(String.valueOf(responseBody.repositories().getFirst().id()));
    } catch (ApiException e) {
      return Optional.empty();
    }
  }

  public CreateRepoResponse createRepoFor(CreateRepoRequestBody requestBody, String token) {
    HttpHeaders headers = getGithubHttpHeaders(token);
    HttpEntity<CreateRepoRequestBody> entity = new HttpEntity<>(requestBody, headers);
    URI createRepoUri = getCreateRepoUri();
    log.info("creating repo for {}, {}", createRepoUri, requestBody);
    return restTemplate.exchange(createRepoUri, POST, entity, CreateRepoResponse.class).getBody();
  }

  private static String removeGithubPrefix(String url) {
    return url.replace("https://github.com/", "");
  }

  public CreateRepoResponse createRepoFromTemplateFor(
      CreateRepoFromTemplateRequestBody requestBody, String token) {
    HttpHeaders headers = getGithubHttpHeaders(token);
    HttpEntity<CreateRepoFromTemplateRequestBody> entity = new HttpEntity<>(requestBody, headers);

    String[] slugs = removeGithubPrefix(requestBody.templateRepo()).split("/");
    if (slugs.length != 2) {
      throw new IllegalArgumentException("Invalid templateRepo format, expected 'owner/repo'.");
    }

    URI createRepoUri = getCreateRepoFromTemplateUrl(slugs[0], slugs[1]);

    log.info("creating repo for {}, {}", createRepoUri, requestBody);

    return restTemplate.exchange(createRepoUri, POST, entity, CreateRepoResponse.class).getBody();
  }

  public UpdateRepoResponse updateRepoFor(
      UpdateRepoRequestBody requestBody,
      String repositoryName,
      String token,
      String repoOwnerUsername) {
    HttpHeaders headers = getGithubHttpHeaders(token);
    HttpEntity<UpdateRepoRequestBody> entity = new HttpEntity<>(requestBody, headers);
    String uriString = getUpdateRepoUri(repositoryName, repoOwnerUsername).toUriString();
    log.info("updating repo for {}, {}", uriString, requestBody);
    return restTemplate.exchange(uriString, PATCH, entity, UpdateRepoResponse.class).getBody();
  }

  private UriComponents getUpdateRepoUri(String repositoryName, String githubUsername) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/repos/{owner}/{repositoryName}")
        .buildAndExpand(githubUsername, repositoryName);
  }

  private UriComponents getListAppInstallationUri() {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/app/installations")
        .queryParam("per_page", 100)
        .build();
  }

  private UriComponents getListAppInstallationRepositoriesUri(
      PageFromOne page, BoundedPageSize pageSize) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/installation/repositories")
        .queryParam("page", page.getValue())
        .queryParam("per_page", pageSize.getValue())
        .build();
  }

  public UriComponents getSearchRepositoriesUri(
      String q, PageFromOne page, BoundedPageSize pageSize) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/search/repositories")
        .queryParam("q", q)
        .queryParam("page", page.getValue())
        .queryParam("per_page", pageSize.getValue())
        .build();
  }

  private UriComponents getAppInstallationByIdUri(long installationId) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/app/installations/{id}")
        .buildAndExpand(installationId);
  }

  private UriComponents getAppInstallationRepositoriesUri() {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/installation/repositories")
        .build();
  }

  private static HttpHeaders getGithubHttpHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(APPLICATION_JSON));
    headers.set("Authorization", "Bearer " + token);
    headers.set("X-GitHub-Api-Version", "2022-11-28");
    return headers;
  }

  private URI getCreateRepoUri() {
    return getCreateRepoFromTemplateUrl("poja-app", "poja-starter-template");
  }

  private URI getCreateRepoFromTemplateUrl(String owner, String repo) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/repos/{owner}/{repo}/generate")
        .buildAndExpand(owner, repo)
        .toUri();
  }

  public String getAppInstallationToken(long installationId, Duration expiration) {
    var jwtToken = jwtGenerator.createJwt(githubAppId, expiration);
    try {
      var gitHubApp = new GitHubBuilder().withJwtToken(jwtToken).build();
      return gitHubApp
          .getApp()
          .getInstallationById(installationId)
          .createToken()
          .create()
          .getToken();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Set<GhAppInstallation> listInstallations() {
    var jwtToken = jwtGenerator.createJwt(githubAppId, Duration.ofSeconds(60));
    HttpHeaders headers = getGithubHttpHeaders(jwtToken);
    HttpEntity<GhAppInstallationResponse> entity = new HttpEntity<>(headers);

    ParameterizedTypeReference<List<GhAppInstallationResponse>> typeRef =
        new ParameterizedTypeReference<>() {};
    var response =
        restTemplate
            .exchange(getListAppInstallationUri().toUriString(), GET, entity, typeRef)
            .getBody();

    return response.stream().map(GithubComponent::toDomain).collect(Collectors.toSet());
  }

  public GhAppInstallation getInstallationById(long id) {
    var jwtToken = jwtGenerator.createJwt(githubAppId, Duration.ofSeconds(30));
    HttpHeaders headers = getGithubHttpHeaders(jwtToken);
    HttpEntity<GhAppInstallationResponse> entity = new HttpEntity<>(headers);

    var response =
        restTemplate
            .exchange(
                getAppInstallationByIdUri(id).toUriString(),
                GET,
                entity,
                GhAppInstallationResponse.class)
            .getBody();
    return toDomain(response);
  }

  public GhGetCommitResponse getCommitInfo(
      String owner, long installationId, String repoName, String sha) {
    var jwtToken = getAppInstallationToken(installationId, Duration.ofMinutes(1));
    HttpHeaders headers = getGithubHttpHeaders(jwtToken);
    HttpEntity<GhGetCommitResponse> entity = new HttpEntity<>(headers);

    ParameterizedTypeReference<GhGetCommitResponse> typeRef = new ParameterizedTypeReference<>() {};
    return restTemplate
        .exchange(getGetCommitInfoUri(owner, repoName, sha).toUriString(), GET, entity, typeRef)
        .getBody();
  }

  private UriComponents getGetCommitInfoUri(String owner, String repoName, String sha) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/repos/{owner}/{repo}/commits/{ref}")
        .buildAndExpand(owner, repoName, sha);
  }

  public void runWorkflowDispatch(
      String owner,
      String repoName,
      String jwtToken,
      String workflowId,
      GhWorkflowRunRequestBody requestBody) {
    HttpHeaders headers = getGithubHttpHeaders(jwtToken);
    HttpEntity<GhWorkflowRunRequestBody> entity = new HttpEntity<>(requestBody, headers);
    ResponseEntity<String> response =
        restTemplate.exchange(
            getWorkflowDispatchRunUri(owner, repoName, workflowId).toUriString(),
            POST,
            entity,
            String.class);
    HttpStatusCode statusCode = response.getStatusCode();
    if (UNPROCESSABLE_ENTITY.isSameCodeAs(statusCode)) {
      throw new WorkflowRunFailed("workflow run failed");
    }
    if (!NO_CONTENT.isSameCodeAs(statusCode)) {
      throw new ApiException(SERVER_EXCEPTION, response.getBody());
    }
  }

  private UriComponents getWorkflowDispatchRunUri(
      String owner, String repoName, String workflowId) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
        .buildAndExpand(owner, repoName, workflowId);
  }

  @SneakyThrows
  private static GhAppInstallation toDomain(GhAppInstallationResponse installation) {
    var account = installation.account();
    var ownerLogin = account.login();
    String type = account.type();
    String avatarUrl = account.avatarUrl();
    return new GhAppInstallation(
        installation.id(),
        ownerLogin,
        type,
        avatarUrl,
        RepositorySelection.fromValue(installation.repositorySelection().toString()));
  }

  private GhPublicKey fetchPublicKey(String owner, String repo, HttpHeaders headers) {
    var entity = new HttpEntity<>(headers);
    var publicKeyUrl = buildPublicKeyUrl(owner, repo);

    try {
      ResponseEntity<Map<String, String>> response =
          restTemplate.exchange(publicKeyUrl, GET, entity, new ParameterizedTypeReference<>() {});
      return parsePublicKeyResponse(response, owner, repo);
    } catch (Exception e) {
      var errorMessage =
          String.format(
              "[GitHub API] Failed to fetch public key for repository %s/%s", owner, repo);
      log.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  private void sendCrupdateSecretRequest(
      String owner,
      String repo,
      String secretName,
      String encryptedValue,
      String keyId,
      HttpHeaders headers) {
    var requestBody =
        Map.of(
            "encrypted_value", encryptedValue,
            "key_id", keyId);

    var requestEntity = new HttpEntity<>(requestBody, headers);
    var putUrl = buildSecretUrl(owner, repo, secretName);

    try {
      restTemplate.exchange(putUrl, PUT, requestEntity, Void.class);
    } catch (Exception e) {
      var errorMessage =
          String.format(
              "[GitHub API] Failed to create/update secret '%s' for repository %s/%s",
              secretName, owner, repo);
      log.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  private String encryptSecret(String secretValue, String publicKey, String owner, String repo) {
    try {
      return githubSecretEncryptor.encryptSecretValue(secretValue, publicKey);
    } catch (Exception e) {
      var errorMessage =
          String.format(
              "[GitHub API] Failed to encrypt secret '%s' for repository %s/%s", owner, repo);
      log.error(errorMessage, e);
      throw new RuntimeException(errorMessage, e);
    }
  }

  private static GhPublicKey parsePublicKeyResponse(
      ResponseEntity<Map<String, String>> response, String owner, String repo) {
    var body = response.getBody();
    if (body == null) {
      var errorMessage =
          String.format("[GitHub API] Empty response for repository %s/%s", owner, repo);
      log.error(errorMessage);
      throw new IllegalStateException(errorMessage);
    }

    var key = body.get("key");
    var keyId = body.get("key_id");

    return new GhPublicKey(key, keyId);
  }

  private String buildPublicKeyUrl(String owner, String repo) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/repos/{owner}/{repo}/actions/secrets/public-key")
        .buildAndExpand(owner, repo)
        .toUriString();
  }

  private String buildSecretUrl(String owner, String repo, String secretName) {
    return UriComponentsBuilder.fromUri(githubApiBaseUri.toUri())
        .path("/repos/{owner}/{repo}/actions/secrets/{secret_name}")
        .buildAndExpand(owner, repo, secretName)
        .toUriString();
  }

  public void crupdateSecret(String owner, String repoName, String token, GhSecret ghSecret) {
    var headers = getGithubHttpHeaders(token);
    var publicKey = fetchPublicKey(owner, repoName, headers);
    var encryptedValue = encryptSecret(ghSecret.secretValue(), publicKey.key(), owner, repoName);
    sendCrupdateSecretRequest(
        owner, repoName, ghSecret.secretName(), encryptedValue, publicKey.keyId(), headers);
  }
}
