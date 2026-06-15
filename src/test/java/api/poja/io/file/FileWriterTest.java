package api.poja.io.file;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.integration.conf.utils.TestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FileWriterTest extends MockedThirdParties {
  @Autowired FileWriter subject;

  @Test
  void apply() throws IOException {
    var originalContent = "ping";
    var bytes = originalContent.getBytes();

    var noParentResult = subject.apply(bytes, null);
    var withParentResult = subject.apply(bytes, Files.createTempDirectory("lol").toFile());

    assertEquals(originalContent, Files.readString(noParentResult.toPath()));
    assertEquals(originalContent, Files.readString(withParentResult.toPath()));
  }

  @Test
  void write_ok() throws IOException {
    var originalContent = "ping";
    var bytes = originalContent.getBytes();
    String fileName = randomUUID().toString();

    var file = subject.write(bytes, TestUtils.getResource("files").getFile(), fileName);

    assertEquals(originalContent, Files.readString(file.toPath()));
  }

  @Test
  void write_ko() {
    File pathTraversingDirectoryFilename = new File("../../mock");

    var nullFilenameException =
        assertThrows(
            IllegalArgumentException.class,
            () -> subject.write(new byte[0], TestUtils.getResource("files").getFile(), null));
    var pathTraversingDirectoryFilenameException =
        assertThrows(
            IllegalArgumentException.class,
            () -> subject.write(new byte[0], pathTraversingDirectoryFilename, "mock"));

    assertEquals("filename must not be null", nullFilenameException.getMessage());
    assertEquals(
        "name must not contain .. but received: " + pathTraversingDirectoryFilename.getName(),
        pathTraversingDirectoryFilenameException.getMessage());
  }
}
