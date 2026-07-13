package api.poja.io.service.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGeneratorService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BASE_DELAY_MS = 1500L;
    private static final Pattern INVALID_CHAR_LITERAL = Pattern.compile("'([^'\\\\]{2,})'");

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public void generateApplicationCode(PromptAnalysis analysis) {
        String appName = analysis.getApplicationName();
        String packageName = PackageResolver.toPackageName(appName);
        String basePath = PackageResolver.toBasePath(appName);

        log.info("Generating application code for: {}", appName);

        writeMainApplicationClass(appName, packageName, basePath);
        writeApplicationProperties(appName);

        CodeGenerationResult result = generateWithRetries(analysis);
        int written = writeFiles(result, basePath, packageName);

        log.info("Generated {} LLM files (+ bootstrap files) for {}", written, appName);
    }

    private void writeMainApplicationClass(String appName, String packageName, String basePath) {
        String className = PackageResolver.toMainClassName(appName);
        String content = """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class %s {
                    public static void main(String[] args) {
                        SpringApplication.run(%s.class, args);
                    }
                }
                """.formatted(packageName, className, className);

        saveFile(basePath + className + ".java", content);
    }

    private void writeApplicationProperties(String appName) {
        String dbName = appName.replace("-", "_");
        String content = """
                spring.application.name=%s
                spring.datasource.url=jdbc:postgresql://localhost:5432/%s_db
                spring.datasource.username=postgres
                spring.datasource.password=postgres
                spring.jpa.hibernate.ddl-auto=update
                spring.jpa.show-sql=true
                server.port=8080
                """.formatted(appName, dbName);

        saveFile(PackageResolver.toResourcesPath(appName) + "application.properties", content);
    }

    private CodeGenerationResult generateWithRetries(PromptAnalysis analysis) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.info("LLM code generation attempt {}/{}", attempt, MAX_ATTEMPTS);
                String rawResponse = llmClient.generateCode(analysis);
                String jsonPayload = LlmJsonExtractor.extractJson(rawResponse);
                CodeGenerationResult result = objectMapper.readValue(jsonPayload, CodeGenerationResult.class);

                validate(result);
                return result;
            } catch (Exception e) {
                lastError = e;
                log.warn("Attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw new CodeGenerationException(
                "LLM code generation failed after " + MAX_ATTEMPTS + " attempts for application "
                        + analysis.getApplicationName(),
                lastError
        );
    }

    private void validate(CodeGenerationResult result) {
        if (result == null || result.files() == null || result.files().isEmpty()) {
            throw new CodeGenerationException("LLM response contained no files");
        }

        Set<String> seenPaths = new HashSet<>();
        for (GeneratedFile file : result.files()) {
            if (file.path() == null || file.path().isBlank()) {
                throw new CodeGenerationException("Generated file has an empty path");
            }
            if (file.content() == null || file.content().isBlank()) {
                throw new CodeGenerationException("Generated file has empty content: " + file.path());
            }
            if (!seenPaths.add(file.path())) {
                throw new CodeGenerationException("Duplicate file path returned by LLM: " + file.path());
            }
        }
    }

    private int writeFiles(CodeGenerationResult result, String basePath, String packageName) {
        int written = 0;
        for (GeneratedFile file : result.files()) {
            String fileName = file.path().substring(file.path().lastIndexOf('/') + 1);

            if (isBootstrapFile(fileName)) {
                log.warn("Skipping LLM-generated bootstrap file (already generated deterministically): {}", fileName);
                continue;
            }

            String adaptedPath = basePath + fileName;
            String adaptedContent = file.content().replace("com.example", packageName);
            adaptedContent = autoFixInvalidCharLiterals(adaptedPath, adaptedContent);

            saveFile(adaptedPath, adaptedContent);
            written++;
        }
        return written;
    }

    private String autoFixInvalidCharLiterals(String path, String content) {
        if (!path.endsWith(".java")) {
            return content;
        }
        Matcher matcher = INVALID_CHAR_LITERAL.matcher(content);
        if (matcher.find()) {
            String fixed = matcher.replaceAll("\"$1\"");
            log.warn("Auto-fixed invalid single-quoted string literal(s) in {}", path);
            return fixed;
        }
        return content;
    }

    private boolean isBootstrapFile(String fileName) {
        return fileName.equals("application.properties") || fileName.endsWith("Application.java");
    }

    private void saveFile(String path, String content) {
        try {
            Path filePath = Paths.get(path);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content.getBytes());
            log.info("Created: {}", path);
        } catch (IOException e) {
            throw new CodeGenerationException("Failed to write file: " + path, e);
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_BASE_DELAY_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}