package api.poja.io.service.prompt;

import java.util.List;

public record CodeGenerationResult(List<GeneratedFile> files) {
}