package api.poja.io.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptToDeployRequest {
  private String prompt;
  private String repositoryName;
  private Boolean isPrivate;
}
