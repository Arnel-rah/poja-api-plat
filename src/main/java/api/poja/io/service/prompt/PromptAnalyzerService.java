package api.poja.io.service.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptAnalyzerService {

  private final LlmClient llmClient;
  private final ObjectMapper objectMapper;

  @Value("${llm.enabled:true}")
  private boolean llmEnabled;

  @Value("${llm.fallback.enabled:true}")
  private boolean fallbackEnabled;

  public PromptAnalysis analyze(String prompt) {
    log.info("Analyzing prompt: {}", prompt);

    if (llmEnabled) {
      try {
        String llmResponse = llmClient.analyzePrompt(prompt);
        log.debug("LLM Response: {}", llmResponse);
        return parseLlmResponse(llmResponse, prompt);
      } catch (Exception e) {
        log.warn("LLM failed: {}. Using fallback.", e.getMessage());
        if (fallbackEnabled) {
          return fallbackAnalysis(prompt);
        }
        throw new RuntimeException("LLM analysis failed and fallback disabled", e);
      }
    }

    return fallbackAnalysis(prompt);
  }

  @SuppressWarnings("unchecked")
  private PromptAnalysis parseLlmResponse(String response, String originalPrompt) {
    try {
      Map<String, Object> parsed = objectMapper.readValue(response, Map.class);

      return PromptAnalysis.builder()
              .applicationName((String) parsed.getOrDefault("applicationName", "my-app"))
              .description((String) parsed.getOrDefault("description", originalPrompt))
              .techStack((String) parsed.getOrDefault("techStack", "spring-boot"))
              .databaseType((String) parsed.getOrDefault("databaseType", "postgresql"))
              .authType((String) parsed.getOrDefault("authType", "none"))
              .requiresDatabase((Boolean) parsed.getOrDefault("requiresDatabase", true))
              .requiresAuth((Boolean) parsed.getOrDefault("requiresAuth", false))
              .entities((List<Map<String, Object>>) parsed.getOrDefault("entities", List.of()))
              .features((List<String>) parsed.getOrDefault("features", List.of()))
              .originalPrompt(originalPrompt)
              .build();

    } catch (Exception e) {
      log.error("Failed to parse LLM response: {}", e.getMessage());
      return fallbackAnalysis(originalPrompt);
    }
  }

  private PromptAnalysis fallbackAnalysis(String prompt) {
    log.warn("Using fallback analysis (LLM failed)");
    String lowerPrompt = prompt.toLowerCase();

    return PromptAnalysis.builder()
            .applicationName(extractName(prompt))
            .description(prompt)
            .techStack(lowerPrompt.contains("spring") ? "spring-boot" :
                    lowerPrompt.contains("react") ? "react" : "spring-boot")
            .databaseType(lowerPrompt.contains("postgres") ? "postgresql" :
                    lowerPrompt.contains("mongodb") ? "mongodb" : "postgresql")
            .authType(lowerPrompt.contains("jwt") ? "jwt" :
                    lowerPrompt.contains("oauth") ? "oauth-github" : "none")
            .requiresDatabase(!lowerPrompt.contains("no database"))
            .requiresAuth(lowerPrompt.contains("auth") || lowerPrompt.contains("jwt"))
            .entities(List.of())
            .features(List.of())
            .originalPrompt(prompt)
            .build();
  }

  private String extractName(String prompt) {
    String[] words = prompt.split(" ");
    if (words.length > 2) {
      return words[0].toLowerCase() + "-" + words[1].toLowerCase();
    }
    return "my-app-" + System.currentTimeMillis();
  }
}