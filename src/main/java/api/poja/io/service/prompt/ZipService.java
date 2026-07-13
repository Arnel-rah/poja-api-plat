package api.poja.io.service.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ZipService {

    @Value("${app.generated-dir:generated/}")
    private String generatedDir;

    @Value("${app.downloads-dir:downloads/}")
    private String downloadsDir;

    /**
     * Creates a ZIP archive of the generated application.
     *
     * @param appName the name of the application
     * @return the absolute path to the created ZIP file
     * @throws IOException if an I/O error occurs
     */
    public String createZip(String appName) throws IOException {
        if (appName == null || appName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        Path sourcePath = Paths.get(generatedDir, appName).normalize();
        String zipFileName = appName + ".zip";
        Path zipPath = Paths.get(downloadsDir, zipFileName).normalize();

        log.info("Creating ZIP for application: {} → {}", appName, zipPath);

        Files.createDirectories(zipPath.getParent());
        Files.createDirectories(sourcePath);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Source directory not found: " + sourcePath);
        }

        Files.deleteIfExists(zipPath);

        try (var fos = Files.newOutputStream(zipPath);
             var zos = new ZipOutputStream(fos)) {

            zos.setLevel(6);

            Files.walk(sourcePath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> addFileToZip(sourcePath, path, zos));
        }

        long size = Files.size(zipPath);
        log.info("ZIP created successfully: {} ({} bytes)", zipPath, size);

        return zipPath.toString();
    }

    private void addFileToZip(Path sourcePath, Path filePath, ZipOutputStream zos) {
        try {
            String entryName = sourcePath.relativize(filePath).toString().replace("\\", "/");
            zos.putNextEntry(new ZipEntry(entryName));
            Files.copy(filePath, zos);
            zos.closeEntry();

            log.debug("Added: {}", entryName);
        } catch (IOException e) {
            log.error("Failed to add file to ZIP: {}", filePath, e);
        }
    }
}