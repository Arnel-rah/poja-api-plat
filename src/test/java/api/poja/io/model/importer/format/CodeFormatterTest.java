package api.poja.io.model.importer.format;

import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.file.ExtensionGuesser;
import api.poja.io.file.FileUnzipper;
import api.poja.io.file.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Slf4j
class CodeFormatterTest {
  public static final String UNFORMATTED_CODE = "files/import/format/unformatted-code.zip";
  public static final String FORMATTED_CODE = "files/import/format/formatted-code.zip";

  final FileWriter fileWriter = new FileWriter(new ExtensionGuesser());
  final FileUnzipper fileUnzipper = new FileUnzipper(fileWriter);
  final CodeFormatter subject = new CodeFormatter();

  @Test
  void shouldBe_Formatted(@TempDir Path tempDir) throws IOException {
    var codeZip = new ZipFile(getResource(UNFORMATTED_CODE).getFile().getPath());
    var code = fileUnzipper.apply(codeZip, tempDir.resolve("code")).resolve("src");

    var expectedZip = new ZipFile(getResource(FORMATTED_CODE).getFile().getPath());
    var expected = fileUnzipper.apply(expectedZip, tempDir.resolve("expected")).resolve("src");

    subject.apply(code);

    try (var actualStream = Files.walk(code);
        var expectedStream = Files.walk(expected)) {
      var actualFiles = actualStream.filter(Files::isRegularFile).sorted().toList();
      var expectedFiles = expectedStream.filter(Files::isRegularFile).sorted().toList();

      assertEquals(expectedFiles.size(), actualFiles.size());

      for (int i = 0; i < actualFiles.size(); i++) {
        assertEquals(-1, Files.mismatch(actualFiles.get(i), expectedFiles.get(i)));
      }
    }
  }

  @Test
  void should_fail_when_java_syntax_is_invalid(@TempDir Path tempDir) throws IOException {
    var invalidJavaFile = tempDir.resolve("InvalidCode.java");
    Files.writeString(
        invalidJavaFile,
        """
        public class InvalidCode {
          public void brokenMethod( {
            System.out.println("This won't compile");
          }
        """);

    var result = subject.apply(tempDir);

    assertTrue(result.failed());
    assertEquals(
        "Failed to format file " + invalidJavaFile + ": 2:30: error: illegal start of type",
        result.logs().getFirst().getMessage());
  }
}
