package api.poja.io.model.importer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;

public class UncheckedFileContentReader implements Function<File, String> {
  @Override
  public String apply(File file) {
    try {
      return new String(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
