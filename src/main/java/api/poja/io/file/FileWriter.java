package api.poja.io.file;

import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.UUID.randomUUID;

import api.poja.io.model.exception.ApiException;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileWriter implements BiFunction<byte[], File, File> {
  private final ExtensionGuesser extensionGuesser;

  @Override
  public File apply(byte[] bytes, @Nullable File directory) {
    try {
      String name = randomUUID().toString();
      String suffix = "." + extensionGuesser.apply(bytes);
      File tempFile = File.createTempFile(name, suffix, directory);
      return Files.write(tempFile.toPath(), bytes).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public File write(byte[] bytes, File directory, String filename) {
    if (directory != null && directory.getPath().contains("..")) {
      throw new IllegalArgumentException(
          "name must not contain .. but received: " + directory.getName());
    }
    if (filename == null) {
      throw new IllegalArgumentException("filename must not be null");
    }
    try {
      File newFile = new File(directory, filename);
      Files.createDirectories(newFile.toPath().getParent());
      if (!newFile.exists()) {
        newFile.createNewFile();
      }
      return Files.write(newFile.toPath(), bytes, WRITE, TRUNCATE_EXISTING).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
