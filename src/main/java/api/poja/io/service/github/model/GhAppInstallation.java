package api.poja.io.service.github.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;

public record GhAppInstallation(
    long appId,
    String ownerGithubLogin,
    String type,
    String avatarUrl,
    RepositorySelection repositorySelection) {
  @AllArgsConstructor
  public enum RepositorySelection {
    ALL,
    SELECTED;

    @JsonCreator
    public static RepositorySelection fromValue(String value) {
      for (var b : RepositorySelection.values()) {
        if (b.name().equalsIgnoreCase(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unknown repository selection: '" + value + "'");
    }
  }
}
