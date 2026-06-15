package api.poja.io.model.importer.analyzer.lang;

import static java.nio.file.Files.exists;
import static java.nio.file.Files.readString;
import static java.nio.file.Files.walk;

import api.poja.io.model.importer.analyzer.Analyzer;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.UnknownApplication;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AppLanguageAnalyzer implements Analyzer {
  private static final String JAVA_MAIN_SRC_PATH = "src/main/java";
  private static final Pattern MAIN_METHOD_PATTERN =
      Pattern.compile(
          "\\b(?=(?:[a-z ]* )?public(?:[a-z ]* )?)(?=(?:[a-z ]* )?static(?:[a-z ]*"
              + " )?)(?:public|static|final|synchronized|strictfp)"
              + " +(?:public|static|final|synchronized|strictfp)"
              + " *(?:(?:public|static|final|synchronized|strictfp) )*void"
              + " main\\(String(?:\\[\\]|\\.\\.\\.) \\w+\\)");

  @Override
  public AppLangAnalyzerResult analyze(UnknownApplication unknownApplication) {
    var root = unknownApplication.file().toPath();
    var mainSrcPath = root.resolve(JAVA_MAIN_SRC_PATH);

    if (!exists(mainSrcPath)) {
      return resultWithError("src/main/java not found");
    }

    return checkJavaFiles(root, mainSrcPath);
  }

  private static AppLangAnalyzerResult checkJavaFiles(Path root, Path mainSrcPath) {
    try (var paths = walk(mainSrcPath)) {
      var mainMethodPath =
          paths
              .filter(Files::isRegularFile)
              .filter(e -> e.toString().endsWith(".java"))
              .filter(
                  f -> {
                    try {
                      return hasMainMethod(f);
                    } catch (IOException e) {
                      throw new UncheckedIOException(e);
                    }
                  })
              .map(root::relativize)
              .findFirst()
              .orElse(null);

      return new AppLangAnalyzerResult(mainMethodPath);
    } catch (UncheckedIOException | IOException e) {
      return resultWithError("Error while analyzing Java files: " + e.getMessage());
    }
  }

  private static boolean hasMainMethod(Path javaFile) throws IOException {
    var content =
        readString(javaFile)
            .replaceAll("(?s)/\\*.*?\\*/", "")
            .replaceAll("(?m)//.*$", "")
            .replaceAll("(?s)@[A-Za-z0-9_$.]+(?:\\s*\\([^)]*\\))?", "")
            .replaceAll("\\s+", " ");
    return MAIN_METHOD_PATTERN.matcher(content).find();
  }

  private static AppLangAnalyzerResult resultWithError(String message) {
    return new AppLangAnalyzerResult(null, ApplicationImportLog.error(message));
  }
}
