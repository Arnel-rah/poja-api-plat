package api.poja.io.file;

import static java.nio.file.Files.createTempDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class TempFile {
  public static Path createTempDir(String prefix) {
    try {
      return createTempDirectory(prefix);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
