package api.poja.io.service.prompt;

import api.poja.io.model.PromptToDeployMapper;
import api.poja.io.model.PromptToDeployRequest;
import api.poja.io.model.PromptToDeployResponse;
import api.poja.io.repository.PromptAppRequestRepository;
import api.poja.io.repository.model.PromptAppRequest;
import api.poja.io.repository.model.enums.PromptAppRequestStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromptDeployService {

  private final PromptAppRequestRepository requestRepository;
  private final PromptToDeployMapper mapper;
  private final PromptAnalyzerService promptAnalyzer;
  private final TemplateCustomizerService templateCustomizer;
  private final ProjectGeneratorService projectGenerator;

  @Transactional
  public PromptToDeployResponse process(
      String orgId, String userId, PromptToDeployRequest request) {
    log.info("Processing prompt deploy for user {} in org {}", userId, orgId);

    try {
      PromptAnalysis analysis = promptAnalyzer.analyze(request.getPrompt());
      log.info("Analysis: {}", analysis);

      PromptAppRequest appRequest = createRequest(orgId, userId, request);
      appRequest.setStatus(PromptAppRequestStatus.IN_PROGRESS);
      requestRepository.save(appRequest);

      String templateContent = templateCustomizer.customize(analysis);
      log.info("Template customized");

      String projectPath = projectGenerator.generate(analysis);
      log.info("Project generated at: {}", projectPath);
      // TODO: Sauvegarder le template dans le dossier généré

      String appId = "app-" + System.currentTimeMillis();
      appRequest.setApplicationId(appId);
      appRequest.setApplicationName(analysis.getApplicationName());
      appRequest.setStatus(PromptAppRequestStatus.COMPLETED);
      appRequest.setUpdatedAt(Instant.now());
      requestRepository.save(appRequest);

      return buildSuccessResponse(appRequest);

    } catch (Exception e) {
      log.error("Deployment failed: {}", e.getMessage(), e);
      return buildErrorResponse(request, e.getMessage());
    }
  }

  public PromptToDeployResponse getStatus(String requestId, String orgId) {
    log.info("Polling status for request: {}", requestId);

    PromptAppRequest request =
        requestRepository
            .findByIdAndOrgId(requestId, orgId)
            .orElseThrow(() -> new RuntimeException("Request not found"));

    return mapper.toResponse(request);
  }

  private PromptAppRequest createRequest(
      String orgId, String userId, PromptToDeployRequest request) {
    return PromptAppRequest.builder()
        .id(UUID.randomUUID().toString())
        .orgId(orgId)
        .userId(userId)
        .prompt(request.getPrompt())
        .status(PromptAppRequestStatus.PENDING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private PromptToDeployResponse buildSuccessResponse(PromptAppRequest request) {
    return PromptToDeployResponse.builder()
        .promptRequestId(request.getId())
        .status("COMPLETED")
        .application(
            PromptToDeployResponse.ApplicationSummary.builder()
                .id(request.getApplicationId())
                .name(request.getApplicationName())
                .status("ACTIVE")
                .repositoryUrl("https://github.com/poja/" + request.getApplicationName())
                .deploymentUrl("https://" + request.getApplicationName() + ".poja.io")
                .build())
        .createdAt(request.getCreatedAt().toString())
        .updatedAt(request.getUpdatedAt().toString())
        .build();
  }

  private PromptToDeployResponse buildErrorResponse(
      PromptToDeployRequest request, String errorMessage) {
    return PromptToDeployResponse.builder()
        .promptRequestId(UUID.randomUUID().toString())
        .status("FAILED")
        .errorMessage(errorMessage)
        .createdAt(Instant.now().toString())
        .updatedAt(Instant.now().toString())
        .build();
  }
}
