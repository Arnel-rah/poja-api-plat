package api.poja.io.service.gradle;

import static api.poja.io.file.ExtendedBucketComponent.getGradleDistBucketKey;

import api.poja.io.file.ExtendedBucketComponent;
import api.poja.io.file.FileUnzipper;
import api.poja.io.model.gradle.GradleDist;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.zip.ZipFile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class GradleDistDownloader implements Function<GradleDist, File> {
  private final ExtendedBucketComponent bucket;
  private final FileUnzipper unzipper;

  public File apply(GradleDist dist) {
    String key = getGradleDistBucketKey(dist);
    log.info("Downloading gradle dist {} ...", dist);
    File file = bucket.download(key);
    log.info("Gradle dist {} downloaded!", dist);

    try (var zipFile = new ZipFile(file)) {
      Path tempDir = Files.createTempDirectory("gradle-dist");
      unzipper.apply(zipFile, tempDir);

      var distPath = tempDir.resolve(dist.filename());
      return distPath.toFile();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
