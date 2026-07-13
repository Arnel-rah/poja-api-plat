package api.poja.io.model;

import api.poja.io.repository.model.PromptAppRequest;
import api.poja.io.repository.model.enums.PromptAppRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PromptToDeployMapper {

  public PromptAppRequest toEntity(PromptToDeployRequest request, String orgId, String userId) {
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

  public PromptToDeployResponse toResponse(PromptAppRequest entity) {
    return PromptToDeployResponse.builder()
            .requestId(entity.getId())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt().toString())
            .updatedAt(entity.getUpdatedAt().toString())
            .build();
  }

  public PromptToDeployResponse toResponseWithApplication(
          PromptAppRequest entity, String repositoryUrl, String deploymentUrl) {

    PromptToDeployResponse.ApplicationSummary appSummary =
            PromptToDeployResponse.ApplicationSummary.builder()
                    .id(entity.getApplicationId())
                    .name(entity.getApplicationName())
                    .status(entity.getStatus().name())
                    .repositoryUrl(repositoryUrl)
                    .deploymentUrl(deploymentUrl)
                    .build();

    return PromptToDeployResponse.builder()
            .requestId(entity.getId())
            .status(entity.getStatus().name())
            .application(appSummary)
            .errorMessage(entity.getErrorMessage())
            .createdAt(entity.getCreatedAt().toString())
            .updatedAt(entity.getUpdatedAt().toString())
            .build();
  }
}