
package api.poja.io.service.prompt;

import api.poja.io.model.PromptToDeployRequest;
import api.poja.io.model.PromptToDeployResponse;
import api.poja.io.repository.PromptAppRequestRepository;
import api.poja.io.repository.model.PromptAppRequest;
import api.poja.io.repository.model.enums.PromptAppRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptDeployService {

  private final PromptAppRequestRepository requestRepository;
  private final PromptAnalyzerService promptAnalyzer;
  private final TemplateCustomizerService templateCustomizer;
  private final ProjectGeneratorService projectGenerator;
  private final CodeGeneratorService codeGenerator;
  private final ZipService zipService;

  @Value("${server.base-url:http://localhost:8080}")
  private String baseUrl;

  @Transactional
  public PromptToDeployResponse process(String orgId, String userId, PromptToDeployRequest request) {
    log.info("Processing prompt deploy for user {} in org {}", userId, orgId);

    String requestId = UUID.randomUUID().toString();

    try {
      PromptAnalysis analysis = promptAnalyzer.analyze(request.getPrompt());
      log.info("LLM Analysis: {}", analysis);

      String projectPath = projectGenerator.generate(analysis);
      log.info("Project structure generated at: {}", projectPath);

      String templateContent = templateCustomizer.customize(analysis);
      java.nio.file.Files.write(
              java.nio.file.Paths.get(projectPath + "/template.yml"),
              templateContent.getBytes()
      );
      log.info("Template customized");
      codeGenerator.generateApplicationCode(analysis);
      log.info("Application code generated successfully");

      String zipPath = zipService.createZip(analysis.getApplicationName());
      log.info("ZIP created: {}", zipPath);
      PromptAppRequest appRequest = PromptAppRequest.builder()
              .id(requestId)
              .orgId(orgId)
              .userId(userId)
              .prompt(request.getPrompt())
              .status(PromptAppRequestStatus.COMPLETED)
              .applicationName(analysis.getApplicationName())
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build();
      requestRepository.save(appRequest);

      return PromptToDeployResponse.builder()
              .requestId(requestId)
              .status("COMPLETED")
              .downloadUrl(baseUrl + "/downloads/" + analysis.getApplicationName() + ".zip")
              .fileName(analysis.getApplicationName() + ".zip")
              .message("Projet généré avec succès !\n" +
                      "Entités détectées: " + analysis.getEntities().size() + "\n" +
                      "Fonctionnalités: " + String.join(", ", analysis.getFeatures()))
              .createdAt(Instant.now().toString())
              .updatedAt(Instant.now().toString())
              .build();

    } catch (Exception e) {
      log.error("Generation failed: {}", e.getMessage(), e);
      return PromptToDeployResponse.builder()
              .requestId(requestId)
              .status("FAILED")
              .message("Erreur : " + e.getMessage())
              .createdAt(Instant.now().toString())
              .updatedAt(Instant.now().toString())
              .build();
    }
  }

  public PromptToDeployResponse getStatus(String requestId, String orgId) {
    var request = requestRepository.findByIdAndOrgId(requestId, orgId)
            .orElseThrow(() -> new RuntimeException("Request not found"));

    return PromptToDeployResponse.builder()
            .requestId(request.getId())
            .status(request.getStatus().name())
            .createdAt(request.getCreatedAt().toString())
            .updatedAt(request.getUpdatedAt().toString())
            .build();
  }
}