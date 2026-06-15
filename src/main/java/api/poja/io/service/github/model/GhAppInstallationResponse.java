package api.poja.io.service.github.model;

import api.poja.io.service.github.model.GhAppInstallation.RepositorySelection;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GhAppInstallationResponse(
    @JsonProperty("id") long id,
    @JsonProperty("repository_selection") RepositorySelection repositorySelection,
    @JsonProperty("account") GhAppInstallationAccount account) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GhAppInstallationAccount(
      @JsonProperty("login") String login,
      @JsonProperty("type") String type,
      @JsonProperty("avatar_url") String avatarUrl) {}
}
