package api.poja.io.model.importer.format;

import static java.nio.file.Files.readString;

import api.poja.io.model.importer.model.ApplicationImportLog;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CodeFormatter implements Function<Path, CodeFormattingResult> {
  private final Formatter formatter = new Formatter();

  @Override
  public CodeFormattingResult apply(Path projectRoot) {
    List<ApplicationImportLog> logs = new ArrayList<>();
    try {
      var javaFiles =
          Files.walk(projectRoot).filter(path -> path.toString().endsWith(".java")).toList();

      for (var javaFile : javaFiles) {
        formatFile(javaFile, logs);
      }
    } catch (IOException e) {
      var errorMesssage = "Failed to walk project root " + projectRoot + ": " + e.getMessage();
      log.error(errorMesssage);
      logs.add(ApplicationImportLog.error(errorMesssage));
    }

    return new CodeFormattingResult(projectRoot, logs);
  }

  private void formatFile(Path javaFile, List<ApplicationImportLog> logs) {
    try {
      var source = readString(javaFile);
      var formatted = formatter.formatSourceAndFixImports(source);
      if (!source.equals(formatted)) {
        Files.writeString(javaFile, formatted);
      }
    } catch (IOException | FormatterException e) {
      var errorMesssage = "Failed to format file " + javaFile + ": " + e.getMessage();
      log.error(errorMesssage);
      logs.add(ApplicationImportLog.error(errorMesssage));
    }
  }
}
