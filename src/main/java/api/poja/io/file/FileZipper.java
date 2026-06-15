package api.poja.io.file;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileZipper implements BiFunction<Path, Path, Path> {

  @Override
  public Path apply(Path directoryToZip, Path zipFilePath) {
    try {
      createDirectories(zipFilePath.getParent());

      try (ZipOutputStream zos = new ZipOutputStream(newOutputStream(zipFilePath))) {
        File folder = directoryToZip.toFile();
        if (!folder.isDirectory()) {
          throw new IllegalArgumentException("Path to zip must be a directory: " + directoryToZip);
        }
        zipFolder(folder, folder, zos);
      }
      return zipFilePath;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Path apply(Path zipFilePath, Collection<Path> filesToZip) {
    try {
      createDirectories(zipFilePath.getParent());

      try (var zos = new ZipOutputStream(newOutputStream(zipFilePath))) {
        for (var file : filesToZip) {
          if (Files.isDirectory(file)) {
            // treat directory as root and zip its contents
            zipFolder(file.toFile(), file.toFile(), zos);
            continue;
          }

          if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Path must be a file or directory: " + file);
          }

          addFileToZip(file.getParent().toFile(), file.toFile(), zos);
        }
      }

      return zipFilePath;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void zipFolder(File rootFolder, File currentFolder, ZipOutputStream zos)
      throws IOException {
    File[] files = currentFolder.listFiles();
    if (files == null) return;

    for (File file : files) {
      if (file.isDirectory()) {
        zipFolder(rootFolder, file, zos);
      } else {
        addFileToZip(rootFolder, file, zos);
      }
    }
  }

  private static void addFileToZip(File rootFolder, File file, ZipOutputStream zos)
      throws IOException {
    Path relativePath = rootFolder.toPath().relativize(file.toPath()).normalize();

    validatePath(relativePath);

    ZipEntry zipEntry = new ZipEntry(relativePath.toString().replace(File.separatorChar, '/'));
    zos.putNextEntry(zipEntry);

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] buffer = new byte[4096];
      int length;
      while ((length = fis.read(buffer)) >= 0) {
        zos.write(buffer, 0, length);
      }
    }

    zos.closeEntry();
  }

  private static void validatePath(Path normalizedPath) {
    if (normalizedPath.startsWith("..")) {
      throw new IllegalArgumentException("Path traversal attempt detected");
    }
  }
}
