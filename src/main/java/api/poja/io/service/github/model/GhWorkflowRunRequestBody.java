package api.poja.io.service.github.model;

import java.util.Map;

public record GhWorkflowRunRequestBody(String ref, Map<String, String> inputs) {}
