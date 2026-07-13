
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
  private String requestId;
  private String status;
  private String downloadUrl;
  private String fileName;
  private String message;
  private String createdAt;
  private String updatedAt;
  private ApplicationSummary application;
  private String errorMessage;

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