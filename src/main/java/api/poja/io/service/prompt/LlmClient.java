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
                Tu es un expert en architecture logicielle.
                Retourne UNIQUEMENT du JSON valide, rien d'autre.
                Pas de markdown, pas de texte explicatif.
                """;

        String userPrompt = """
                Analyse cette demande et retourne le JSON suivant :
                {
                    "applicationName": "nom-en-kebab-case",
                    "description": "description",
                    "techStack": "spring-boot",
                    "databaseType": "postgresql",
                    "authType": "jwt",
                    "requiresDatabase": true,
                    "requiresAuth": true,
                    "entities": [
                        {"name": "Task", "fields": [{"name": "title", "type": "String", "required": true}]}
                    ],
                    "features": ["CRUD", "JWT Auth"]
                }

                Demande : %s
                """.formatted(prompt);

        return callOpenRouter(systemPrompt, userPrompt, DEFAULT_MAX_TOKENS);
    }

    public String generateCode(PromptAnalysis analysis) {
        log.info("Calling OpenRouter to generate code for {}", analysis.getApplicationName());

        String packageName = PackageResolver.toPackageName(analysis.getApplicationName());

        String systemPrompt = """
                Tu es un expert Spring Boot senior.
                Tu génères uniquement du code Java complet et compilable, sans TODO, sans pseudo-code, sans commentaires.
                Tu retournes UNIQUEMENT un objet JSON valide, sans balises markdown, sans texte avant ou après.
                Le JSON doit respecter exactement ce schéma :
                {"files":[{"path":"controller/XController.java","content":"code java complet en une seule ligne de chaîne JSON"}]}
                Règles impératives :
                - Utilise exactement le package %s pour toutes les classes (déclaration package en première ligne).
                - Pour CHAQUE entité listée ci-dessous, génère 4 fichiers : le Model (@Entity JPA), le Repository (JpaRepository<Entity, Long>), le Service, et le Controller REST (GET all, GET by id, POST, PUT, DELETE, mapping /api/<entite-au-pluriel-minuscule>).
                - Le champ "path" doit être relatif et commencer par le sous-dossier du type de classe : "model/", "repository/", "service/" ou "controller/", suivi du nom de fichier, par exemple "model/Task.java".
                - Respecte le type et la nullabilité de chaque champ (required=true => @Column(nullable=false) côté modèle).
                - N'inclus JAMAIS de classe @SpringBootApplication et n'inclus JAMAIS application.properties : ils existent déjà.
                - IMPORTANT syntaxe Java : utilise TOUJOURS des guillemets doubles pour les chaînes de caractères (ex: "tasks"). Les apostrophes simples sont réservées exclusivement à un unique caractère de type char (ex: 'a'). N'utilise jamais 'texte' avec plusieurs caractères, ce n'est pas du Java valide.
                - Le contenu de chaque fichier doit être une chaîne JSON valide avec les retours à la ligne échappés en \\n.
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
        sb.append("Package racine: ").append(packageName).append("\n");
        if (analysis.getDatabaseType() != null) {
            sb.append("Base de donnees: ").append(analysis.getDatabaseType()).append("\n");
        }
        if (analysis.getAuthType() != null) {
            sb.append("Authentification: ").append(analysis.getAuthType())
                    .append(analysis.isRequiresAuth() ? " (requise sur les endpoints sensibles)" : " (non requise)")
                    .append("\n");
        }
        sb.append("Persistance base de donnees requise: ").append(analysis.isRequiresDatabase()).append("\n");

        List<Map<String, Object>> entities = analysis.getEntities();
        if (entities != null && !entities.isEmpty()) {
            sb.append("Entites a generer (genere les 4 fichiers pour chacune) :\n");
            for (Map<String, Object> entity : entities) {
                sb.append("- ").append(entity.get("name")).append(" : ");
                Object fieldsObj = entity.get("fields");
                if (fieldsObj instanceof List<?> fields) {
                    for (Object f : fields) {
                        if (f instanceof Map<?, ?> field) {
                            sb.append(field.get("name")).append("(").append(field.get("type"));
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
            sb.append("Aucune entite explicite fournie, deduis une entite principale pertinente a partir de la description.\n");
        }

        if (analysis.getFeatures() != null && !analysis.getFeatures().isEmpty()) {
            sb.append("Fonctionnalites attendues: ").append(String.join(", ", analysis.getFeatures())).append("\n");
        }

        sb.append("Retourne uniquement le JSON demande, avec exactement 4 fichiers par entite listee ci-dessus.");
        return sb.toString();
    }

    public String customizeTemplate(PromptAnalysis analysis, String originalTemplate) {
        log.info("Calling OpenRouter to customize template");

        String systemPrompt = """
                Tu es un expert en AWS SAM.
                Retourne UNIQUEMENT le template modifié, rien d'autre.
                """;

        String userPrompt = """
                Personnalise ce template.yml pour l'application %s.

                Template original:
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
                    "temperature", 0.2,
                    "max_tokens", maxTokens
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Sending request to OpenRouter (max_tokens={})", maxTokens);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    llmConfig.getUrl(),
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("OpenRouter returned error status: " + response.getStatusCode());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode choice = jsonResponse.path("choices").path(0);
            String finishReason = choice.path("finish_reason").asText("");
            String content = choice.path("message").path("content").asText();

            if ("length".equals(finishReason)) {
                throw new RuntimeException("OpenRouter response truncated (max_tokens=" + maxTokens + " too low)");
            }

            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenRouter returned empty content");
            }

            log.info("OpenRouter response received ({} chars)", content.length());
            return content;

        } catch (HttpStatusCodeException e) {
            log.error("OpenRouter call failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenRouter call failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("OpenRouter call failed: {}", e.getMessage());
            throw new RuntimeException("OpenRouter call failed: " + e.getMessage(), e);
        }
    }
}