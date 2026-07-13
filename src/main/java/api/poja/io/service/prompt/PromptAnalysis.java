
package api.poja.io.service.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@Data
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
  private List<Map<String, Object>> entities;
  private List<String> features;
  private String originalPrompt;
}