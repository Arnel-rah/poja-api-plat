package api.poja.io.file;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class FileFinder implements BiFunction<Path, Set<String>, List<Path>> {
  @Override
  public List<Path> apply(Path sourceDir, Set<String> targetFilenames) {
    List<Path> files = new ArrayList<>();
    try {
      Files.walkFileTree(
          sourceDir,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
              if (targetFilenames.contains(filePath.getFileName().toString())) {
                files.add(filePath);
                return FileVisitResult.CONTINUE;
              }
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      return List.of();
    }
    return files;
  }
}
