package api.poja.io.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Service
@Slf4j
public class ProjectGeneratorService {

  private static final String OUTPUT_DIR = "generated/";

  private static final String[] CRITICAL_FILES = {
          "build.gradle",
          "settings.gradle"
  };

  private static final String[] OPTIONAL_FILES = {
          "gradlew",
          "gradlew.bat",
          "lombok.config",
          "dummy.pem"
  };

  private static final String[] FOLDERS_TO_COPY = {
          ".github",
          ".scripts",
          ".shell",
          "cf-stacks",
          "doc",
          "gradle"
  };

  @Value("${poja.template.source-dir:.}")
  private String templateSourceDir;

  public String generate(PromptAnalysis analysis) throws IOException {
    String appName = analysis.getApplicationName();
    String outputPath = OUTPUT_DIR + appName + "/";

    Path sourceRoot = Paths.get(templateSourceDir).toAbsolutePath().normalize();
    log.info("Generating project structure for: {} (template source: {})", appName, sourceRoot);

    checkCriticalFilesExist(sourceRoot);

    Path outputDir = Paths.get(outputPath);
    if (Files.exists(outputDir)) {
      deleteDirectory(outputDir);
    }
    Files.createDirectories(outputDir);

    copyEssentialFiles(sourceRoot, appName);

    return outputPath;
  }

  private void checkCriticalFilesExist(Path sourceRoot) throws IOException {
    for (String file : CRITICAL_FILES) {
      Path source = sourceRoot.resolve(file);
      if (!Files.exists(source)) {
        throw new IOException(
                "Critical bootstrap file missing: " + source + ". "
                        + "Set poja.template.source-dir to the poja-api-plat repository root "
                        + "if the application is not started from there."
        );
      }
    }
  }

  private void copyEssentialFiles(Path sourceRoot, String appName) throws IOException {
    String targetDir = OUTPUT_DIR + appName + "/";

    for (String folder : FOLDERS_TO_COPY) {
      Path source = sourceRoot.resolve(folder);
      Path target = Paths.get(targetDir + folder);
      if (Files.exists(source)) {
        copyDirectory(source, target);
        log.info("Copied folder: {}", folder);
      } else {
        log.warn("Optional folder not found, skipping: {}", source);
      }
    }

    for (String file : CRITICAL_FILES) {
      Path source = sourceRoot.resolve(file);
      Path target = Paths.get(targetDir + file);
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
      log.info("Copied critical file: {}", file);
    }

    for (String file : OPTIONAL_FILES) {
      Path source = sourceRoot.resolve(file);
      Path target = Paths.get(targetDir + file);
      if (Files.exists(source)) {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Copied optional file: {}", file);
      } else {
        log.warn("Optional file not found, skipping: {}", source);
      }
    }

    String packagePath = PackageResolver.toBasePath(appName);
    Files.createDirectories(Paths.get(packagePath));
    log.info("Created package structure: {}", packagePath);

    String resourcesPath = PackageResolver.toResourcesPath(appName);
    Files.createDirectories(Paths.get(resourcesPath));
  }

  private void copyDirectory(Path source, Path target) throws IOException {
    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path targetDir = target.resolve(source.relativize(dir));
        if (!Files.exists(targetDir)) {
          Files.createDirectories(targetDir);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
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
    return path.contains(".git") ||
            path.contains("build" + java.io.File.separator) ||
            path.contains(".gradle" + java.io.File.separator) ||
            path.contains("generated" + java.io.File.separator) ||
            path.contains("downloads" + java.io.File.separator) ||
            path.endsWith(".iml") ||
            path.contains(".idea" + java.io.File.separator) ||
            path.contains("node_modules" + java.io.File.separator) ||
            path.contains(".vscode" + java.io.File.separator);
  }

  private void deleteDirectory(Path dir) throws IOException {
    if (Files.exists(dir)) {
      Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(@NotNull Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory( @NotNull Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }
}