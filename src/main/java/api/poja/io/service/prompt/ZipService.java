
package api.poja.io.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class ZipService {

    private static final String GENERATED_DIR = "generated/";
    private static final String DOWNLOADS_DIR = "downloads/";

    public String createZip(String appName) throws IOException {
        String sourceDir = GENERATED_DIR + appName;
        String zipFileName = appName + ".zip";
        String zipPath = DOWNLOADS_DIR + zipFileName;

        log.info("Creating ZIP for: {}", appName);

        Files.createDirectories(Paths.get(DOWNLOADS_DIR));

        Path sourcePath = Paths.get(sourceDir);
        Path zipPathObj = Paths.get(zipPath);

        Files.deleteIfExists(zipPathObj);

        try (FileOutputStream fos = new FileOutputStream(zipPathObj.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String entryName = sourcePath.relativize(path).toString();
                            ZipEntry zipEntry = new ZipEntry(entryName);
                            zos.putNextEntry(zipEntry);
                            zos.write(Files.readAllBytes(path));
                            zos.closeEntry();
                        } catch (IOException e) {
                            log.error("Error adding file to ZIP: {}", e.getMessage());
                        }
                    });
        }

        log.info("ZIP created: {}", zipPath);
        return zipPath;
    }
}