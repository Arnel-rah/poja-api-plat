package api.poja.io.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PromptAnalyzerService {

  public PromptAnalysis analyze(String prompt) {
    log.info("Analyzing prompt: {}", prompt);

    String lowerPrompt = prompt.toLowerCase();

    String name = extractName(prompt);

    String techStack = extractTechStack(lowerPrompt);

    String databaseType = extractDatabaseType(lowerPrompt);

    String authType = extractAuthType(lowerPrompt);

    boolean requiresDatabase =
        lowerPrompt.contains("database")
            || lowerPrompt.contains("db")
            || lowerPrompt.contains("postgres")
            || lowerPrompt.contains("sql");

    boolean requiresAuth =
        lowerPrompt.contains("auth")
            || lowerPrompt.contains("jwt")
            || lowerPrompt.contains("login")
            || lowerPrompt.contains("oauth");

    return PromptAnalysis.builder()
        .applicationName(name)
        .description(prompt)
        .techStack(techStack)
        .databaseType(databaseType)
        .authType(authType)
        .requiresDatabase(requiresDatabase)
        .requiresAuth(requiresAuth)
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

  private String extractTechStack(String lowerPrompt) {
    if (lowerPrompt.contains("spring") || lowerPrompt.contains("java")) {
      return "spring-boot";
    } else if (lowerPrompt.contains("node") || lowerPrompt.contains("express")) {
      return "nodejs";
    } else if (lowerPrompt.contains("react") || lowerPrompt.contains("vue")) {
      return "react";
    }
    return "spring-boot";
  }

  private String extractDatabaseType(String lowerPrompt) {
    if (lowerPrompt.contains("postgres")) {
      return "postgresql";
    } else if (lowerPrompt.contains("mongodb") || lowerPrompt.contains("mongo")) {
      return "mongodb";
    } else if (lowerPrompt.contains("mysql")) {
      return "mysql";
    }
    return "postgresql";
  }

  private String extractAuthType(String lowerPrompt) {
    if (lowerPrompt.contains("jwt")) {
      return "jwt";
    } else if (lowerPrompt.contains("oauth") || lowerPrompt.contains("github")) {
      return "oauth-github";
    } else if (lowerPrompt.contains("keycloak")) {
      return "keycloak";
    }
    return "none";
  }
}
