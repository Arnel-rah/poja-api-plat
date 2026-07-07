package api.poja.io.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptToDeployResponse {
  private String promptRequestId;
  private String status;
  private ApplicationSummary application;
  private String errorMessage;
  private String createdAt;
  private String updatedAt;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApplicationSummary {
    private String id;
    private String name;
    private String status;
    private String repositoryUrl;
    private String deploymentUrl;
  }
}
