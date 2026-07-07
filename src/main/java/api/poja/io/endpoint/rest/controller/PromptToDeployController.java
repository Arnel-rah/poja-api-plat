package api.poja.io.endpoint.rest.controller;

import api.poja.io.model.PromptToDeployRequest;
import api.poja.io.model.PromptToDeployResponse;
import api.poja.io.service.prompt.PromptDeployService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
      @PathVariable String orgId, @PathVariable String promptRequestId) {
    log.info("Polling status for request: {}", promptRequestId);
    return promptDeployService.getStatus(promptRequestId, orgId);
  }
}
