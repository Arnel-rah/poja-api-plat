
package api.poja.io.service.prompt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateCustomizerService {

  private static final String TEMPLATE_PATH = "template.yml";
  private final LlmClient llmClient;

  public String customize(PromptAnalysis analysis) throws IOException {
    log.info("Customizing template for: {}", analysis.getApplicationName());

    String originalTemplate = readTemplate();

    try {
      String customized = llmClient.customizeTemplate(analysis, originalTemplate);
      log.info("Template customized by LLM");
      return customized;
    } catch (Exception e) {
      log.warn("LLM customization failed, using fallback: {}", e.getMessage());
      return fallbackCustomization(analysis, originalTemplate);
    }
  }

  private String readTemplate() throws IOException {
    Path path = Paths.get(TEMPLATE_PATH);
    return new String(Files.readAllBytes(path));
  }

  private String fallbackCustomization(PromptAnalysis analysis, String template) {
    String appName = analysis.getApplicationName();
    String packageName = "com." + appName.replace("-", ".");

    return template
            .replace("poja-plat-10f4a86e-api", appName)
            .replace("jcloudify-api", appName)
            .replace("api.poja.io", packageName)
            .replace("Description: poja-plat-10f4a86e-api",
                    "Description: " + analysis.getDescription());
  }
}