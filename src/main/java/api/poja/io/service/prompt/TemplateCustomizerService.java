package api.poja.io.service.prompt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TemplateCustomizerService {

  private static final String TEMPLATE_PATH = "template.yml";

  public String customize(PromptAnalysis analysis) throws IOException {
    log.info("Customizing template for: {}", analysis.getApplicationName());

    String template = readTemplate();
    String appName = analysis.getApplicationName();
    String packageName = "com." + appName.replace("-", ".");

    String customized =
        template
            .replace("poja-plat-10f4a86e-api", appName)
            .replace("jcloudify-api", appName)
            .replace("api.poja.io", packageName)
            .replace(
                "Description: poja-plat-10f4a86e-api", "Description: " + analysis.getDescription());

    if (analysis.isRequiresDatabase()) {
      customized = addDatabaseConfig(customized, analysis);
    }
    if (analysis.isRequiresAuth()) {
      customized = addAuthConfig(customized, analysis);
    }

    return customized;
  }

  private String readTemplate() throws IOException {
    Path path = Paths.get(TEMPLATE_PATH);
    return new String(Files.readAllBytes(path));
  }

  private String addDatabaseConfig(String template, PromptAnalysis analysis) {
    String dbConfig =
        """

# Database configuration
SPRING_DATASOURCE_URL: !Sub '{{resolve:ssm:/${Env}/db/url}}'
SPRING_DATASOURCE_USERNAME: !Sub '{{resolve:ssm:/${Env}/db/user/username}}'
SPRING_DATASOURCE_PASSWORD: !Sub '{{resolve:ssm:/${Env}/db/user/password}}'
""";
    return template + dbConfig;
  }

  private String addAuthConfig(String template, PromptAnalysis analysis) {
    String authConfig =
        """

# Auth configuration
AUTH_TYPE: %s
JWT_SECRET: !Sub '{{resolve:ssm:/${Env}/jwt/secret}}'
"""
            .formatted(analysis.getAuthType());
    return template + authConfig;
  }
}
