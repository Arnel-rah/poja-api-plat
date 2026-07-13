package api.poja.io.service.prompt;

import api.poja.io.config.LlmConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmClient {

    private static final int DEFAULT_MAX_TOKENS = 2000;
    private static final int CODE_GENERATION_MAX_TOKENS = 8000;

    private final RestTemplate restTemplate;
    private final LlmConfig llmConfig;
    private final ObjectMapper objectMapper;

    public String analyzePrompt(String prompt) {
        log.info("Calling OpenRouter to analyze prompt");

        String systemPrompt = """
                You are an expert software architect.
                Respond with **ONLY** valid JSON, nothing else. No markdown, no explanations, no extra text.
                """;

        String userPrompt = """
                Analyze the following user request and return a JSON object with this exact structure:

                {
                    "applicationName": "name-in-kebab-case",
                    "description": "clear description",
                    "techStack": "spring-boot",
                    "databaseType": "postgresql",
                    "authType": "jwt",
                    "requiresDatabase": true,
                    "requiresAuth": true,
                    "entities": [
                        {
                            "name": "Task",
                            "fields": [
                                {"name": "title", "type": "String", "required": true}
                            ]
                        }
                    ],
                    "features": ["CRUD", "JWT Auth"]
                }

                User request: %s
                """.formatted(prompt);

        return callOpenRouter(systemPrompt, userPrompt, DEFAULT_MAX_TOKENS);
    }

    public String generateCode(PromptAnalysis analysis) {
        log.info("Calling OpenRouter to generate code for {}", analysis.getApplicationName());

        String packageName = PackageResolver.toPackageName(analysis.getApplicationName());

        String systemPrompt = """
                You are a senior Spring Boot expert.
                Generate **only** complete, compilable Java code. No TODOs, no placeholders, no comments.
                
                Return **ONLY** a valid JSON object, no markdown, no extra text.
                The JSON must follow this exact schema:
                {
                    "files": [
                        {
                            "path": "controller/XController.java",
                            "content": "full java code as a single JSON string"
                        }
                    ]
                }

                Strict rules:
                - Use package %s for all classes.
                - For each entity, generate exactly 4 files: Entity (JPA @Entity), Repository, Service, and REST Controller.
                - Path format: "model/", "repository/", "service/", or "controller/" followed by the filename.
                - Respect field types and nullability (@Column(nullable = false) when required = true).
                - Never generate @SpringBootApplication or application.properties.
                - Use double quotes for all strings in Java code. Never use single quotes for String literals.
                - Escape newlines in JSON content as \\n.
                """.formatted(packageName);

        String userPrompt = buildCodeGenerationPrompt(analysis, packageName);

        return callOpenRouter(systemPrompt, userPrompt, CODE_GENERATION_MAX_TOKENS);
    }

    private String buildCodeGenerationPrompt(PromptAnalysis analysis, String packageName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Application: ").append(analysis.getApplicationName()).append("\n");

        if (analysis.getDescription() != null) {
            sb.append("Description: ").append(analysis.getDescription()).append("\n");
        }

        sb.append("Root package: ").append(packageName).append("\n");

        if (analysis.getDatabaseType() != null) {
            sb.append("Database: ").append(analysis.getDatabaseType()).append("\n");
        }

        if (analysis.getAuthType() != null) {
            sb.append("Authentication: ").append(analysis.getAuthType())
                    .append(analysis.isRequiresAuth() ? " (required on sensitive endpoints)" : " (not required)")
                    .append("\n");
        }

        sb.append("Database persistence required: ").append(analysis.isRequiresDatabase()).append("\n");

        List<Map<String, Object>> entities = analysis.getEntities();
        if (entities != null && !entities.isEmpty()) {
            sb.append("Entities to generate (create 4 files for each):\n");
            for (Map<String, Object> entity : entities) {
                sb.append("- ").append(entity.get("name")).append(": ");
                Object fieldsObj = entity.get("fields");
                if (fieldsObj instanceof List<?> fields) {
                    for (Object f : fields) {
                        if (f instanceof Map<?, ?> field) {
                            sb.append(field.get("name"))
                                    .append("(")
                                    .append(field.get("type"));
                            if (Boolean.TRUE.equals(field.get("required"))) {
                                sb.append(", required");
                            }
                            sb.append(") ");
                        }
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("No explicit entities provided. Infer a relevant main entity from the description.\n");
        }

        if (analysis.getFeatures() != null && !analysis.getFeatures().isEmpty()) {
            sb.append("Expected features: ").append(String.join(", ", analysis.getFeatures())).append("\n");
        }

        sb.append("\nReturn only the requested JSON with exactly 4 files per entity.");
        return sb.toString();
    }

    public String customizeTemplate(PromptAnalysis analysis, String originalTemplate) {
        log.info("Calling OpenRouter to customize SAM template");

        String systemPrompt = """
                You are an expert in AWS SAM.
                Return **ONLY** the modified template.yml, nothing else.
                """;

        String userPrompt = """
                Customize this template.yml for the application "%s".

                Original template:
                %s
                """.formatted(analysis.getApplicationName(), originalTemplate);

        return callOpenRouter(systemPrompt, userPrompt, DEFAULT_MAX_TOKENS);
    }

    private String callOpenRouter(String systemPrompt, String userPrompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
            headers.set("HTTP-Referer", "http://localhost:8080");
            headers.set("X-Title", "POJA Prompt to Deploy");

            Map<String, Object> body = Map.of(
                    "model", llmConfig.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.1,
                    "max_tokens", maxTokens,
                    "response_format", Map.of("type", "json_object")
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Sending request to OpenRouter (max_tokens={})", maxTokens);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    llmConfig.getUrl(),
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("OpenRouter returned error: " + response.getStatusCode());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode choice = jsonResponse.path("choices").path(0);
            String finishReason = choice.path("finish_reason").asText("");
            String content = choice.path("message").path("content").asText().trim();

            if ("length".equals(finishReason)) {
                throw new RuntimeException("Response truncated. Increase max_tokens.");
            }

            if (content.isBlank()) {
                throw new RuntimeException("Empty response from OpenRouter");
            }

            log.info("OpenRouter response received ({} chars)", content.length());
            return content;

        } catch (HttpStatusCodeException e) {
            log.error("OpenRouter error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenRouter call failed", e);
        } catch (Exception e) {
            log.error("OpenRouter call failed", e);
            throw new RuntimeException("OpenRouter call failed", e);
        }
    }
}