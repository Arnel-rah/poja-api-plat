package api.poja.io.service.prompt;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProjectGeneratorService {

  private static final String OUTPUT_DIR = "generated/";

  public String generate(PromptAnalysis analysis) throws IOException {
    String appName = analysis.getApplicationName();
    String outputPath = OUTPUT_DIR + appName + "/";

    log.info("Generating project structure for: {}", appName);

    Files.createDirectories(Paths.get(outputPath));

    copyProjectStructure(outputPath);

    customizeFiles(outputPath, analysis);

    generateSourceFiles(outputPath, analysis);

    return outputPath;
  }

  private void copyProjectStructure(String outputPath) throws IOException {
    String[] folders = {".github", ".scripts", ".shell", "cf-stacks", "gradle"};
    for (String folder : folders) {
      Path source = Paths.get(folder);
      if (Files.exists(source)) {
        copyDirectory(source, Paths.get(outputPath + folder));
      }
    }
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<Path>() {
          @Override
          public @NotNull FileVisitResult preVisitDirectory(
              @NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
            Path targetDir = target.resolve(source.relativize(dir));
            if (!Files.exists(targetDir)) {
              Files.createDirectories(targetDir);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public @NotNull FileVisitResult visitFile(
              @NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
            if (shouldIgnore(file)) {
              return FileVisitResult.CONTINUE;
            }
            Path targetFile = target.resolve(source.relativize(file));
            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private boolean shouldIgnore(Path file) {
    String path = file.toString();
    return path.contains(".git")
        || path.contains("build/")
        || path.contains(".gradle/")
        || path.contains("generated/")
        || path.endsWith(".iml")
        || path.contains(".idea/");
  }

  private void customizeFiles(String projectPath, PromptAnalysis analysis) throws IOException {
    Map<String, String> replacements = new HashMap<>();
    String appName = analysis.getApplicationName();
    String packageName = "com." + appName.replace("-", ".");

    replacements.put("api.poja.io", packageName);
    replacements.put("poja-plat-10f4a86e-api", appName);
    replacements.put(
        "Description: poja-plat-10f4a86e-api", "Description: " + analysis.getDescription());

    // Personnaliser les fichiers
    customizeFile(projectPath + "build.gradle", replacements);
    customizeFile(projectPath + "settings.gradle", replacements);
    customizeFile(projectPath + "gradle.properties", replacements);

    // Renommer les packages
    renamePackage(projectPath + "src/main/java/api/poja/io", packageName);
  }

  private void customizeFile(String filePath, Map<String, String> replacements) throws IOException {
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      return;
    }
    String content = new String(Files.readAllBytes(path));
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      content = content.replace(entry.getKey(), entry.getValue());
    }
    Files.write(path, content.getBytes());
  }

  private void renamePackage(String sourcePath, String targetPackage) throws IOException {
    Path source = Paths.get(sourcePath);
    if (!Files.exists(source)) {
      return;
    }
    String[] parts = targetPackage.split("\\.");
    Path current = source;
    for (String part : parts) {
      Path newPath = current.resolveSibling(part);
      if (!Files.exists(newPath)) {
        Files.move(current, newPath);
      }
      current = newPath;
    }
  }

  private void generateSourceFiles(String projectPath, PromptAnalysis analysis) throws IOException {
    // TODO: Générer les fichiers source de base
    String packagePath =
        projectPath + "src/main/java/" + analysis.getApplicationName().replace("-", "/") + "/";
    Files.createDirectories(Paths.get(packagePath + "controller"));
    Files.createDirectories(Paths.get(packagePath + "service"));
    Files.createDirectories(Paths.get(packagePath + "repository"));
    Files.createDirectories(Paths.get(packagePath + "model"));
  }
}
