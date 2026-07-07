package api.poja.io.service.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptAnalysis {
  private String applicationName;
  private String description;
  private String techStack;
  private String databaseType;
  private String authType;
  private boolean requiresDatabase;
  private boolean requiresAuth;
  private String originalPrompt;
}
