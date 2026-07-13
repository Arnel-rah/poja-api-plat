package api.poja.io.endpoint.rest.controller;

import api.poja.io.model.PromptToDeployRequest;
import api.poja.io.model.PromptToDeployResponse;
import api.poja.io.service.prompt.PromptDeployService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/orgs/{orgId}/applications")
@RequiredArgsConstructor
@Slf4j
public class PromptToDeployController {

  private final PromptDeployService promptDeployService;

  @PostMapping("/from-prompt")
  public PromptToDeployResponse createFromPrompt(
          @PathVariable String orgId,
          @RequestHeader("userId") String userId,
          @RequestBody PromptToDeployRequest request) {
    log.info("Prompt to Deploy request from user {} in org {}", userId, orgId);
    return promptDeployService.process(orgId, userId, request);
  }

  @GetMapping("/prompt-requests/{promptRequestId}")
  public PromptToDeployResponse getPromptStatus(
          @PathVariable String orgId,
          @PathVariable String promptRequestId) {
    log.info("Polling status for request: {}", promptRequestId);
    return promptDeployService.getStatus(promptRequestId, orgId);
  }

  @GetMapping("/downloads/{fileName}")
  public ResponseEntity<Resource> downloadZip(@PathVariable String orgId, @PathVariable String fileName) {
    if (!fileName.matches("^[a-zA-Z0-9._-]+\\.zip$")) {
      log.warn("Rejected suspicious download filename: {}", fileName);
      return ResponseEntity.badRequest().build();
    }

    try {
      Path downloadsDir = Paths.get("downloads").normalize();
      Path filePath = downloadsDir.resolve(fileName).normalize();
      if (!filePath.startsWith(downloadsDir)) {
        log.warn("Rejected path traversal attempt: {}", fileName);
        return ResponseEntity.badRequest().build();
      }

      Resource resource = new FileSystemResource(filePath);

      if (!resource.exists()) {
        return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
              .header(HttpHeaders.CONTENT_TYPE, "application/zip")
              .body(resource);
    } catch (Exception e) {
      log.error("Download error: {}", e.getMessage());
      return ResponseEntity.internalServerError().build();
    }
  }
}