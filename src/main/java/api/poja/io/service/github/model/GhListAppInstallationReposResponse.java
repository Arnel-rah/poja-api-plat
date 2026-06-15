package api.poja.io.service.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GhListAppInstallationReposResponse(
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("repositories") List<Repository> repositories) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Repository(
      @JsonProperty("id") long id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("private") boolean isPrivate,
      @JsonProperty("html_url") String htmlUrl,
      @JsonProperty("default_branch") String defaultBranch,
      @JsonProperty("size") int size) {}
}
