package api.poja.io.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FileFinderTest {
  FileFinder subject = new FileFinder();
  Path mockFilePath =
      Path.of(
          Objects.requireNonNull(this.getClass().getClassLoader().getResource("files/mock"))
              .getPath());

  @Test
  void apply() {
    File expectedFile = mockFilePath.resolve("b").resolve("file_1.txt").toFile();

    List<Path> findExistingFileResult = subject.apply(mockFilePath, Set.of("file_1.txt"));
    List<Path> findNonExistingFileResult = subject.apply(mockFilePath, Set.of("nope.txt"));

    assertFalse(findExistingFileResult.isEmpty());
    Path existingFileResultPath = findExistingFileResult.getFirst();
    assertEquals("b", existingFileResultPath.getParent().getFileName().toString());
    assertEquals(expectedFile, existingFileResultPath.toFile());
    assertTrue(findNonExistingFileResult.isEmpty());
  }
}
