package api.poja.io.service.github.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateRepoFromTemplateRequestBody(
    @JsonIgnore String templateRepo,
    String owner,
    String name,
    String description,
    @JsonProperty("private") boolean isPrivate) {}
