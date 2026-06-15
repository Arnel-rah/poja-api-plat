package api.poja.io.service.github.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GhSearchReposResponse(
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("items") List<GhListAppInstallationReposResponse.Repository> repositories) {}
