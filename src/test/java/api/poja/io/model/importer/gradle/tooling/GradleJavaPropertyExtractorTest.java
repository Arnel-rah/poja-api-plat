package api.poja.io.model.importer.gradle.tooling;

import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.JAVA_PLUGIN_PROJECT_DIR;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestUtils.getMockProjectPath;
import static api.poja.io.model.importer.analyzer.buildtool.BuildToolFile.BUILD_GRADLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.CouldNotReadJavaGroupError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.CouldNotReadJavaSourceVersionError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadJavaError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.CouldNotReadJavaTargetVersionWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleJavaPropertyExtractor;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Slf4j
class GradleJavaPropertyExtractorTest {

  private static final String GROUP = "api.poja.io";
  private static final String SOURCE_VERSION = "21";
  private static final String TARGET_VERSION = "21";

  private final GradleJavaPropertyExtractor subject = new GradleJavaPropertyExtractor();

  @Test
  void emptyGbf_should_fail() {
    var gbf = getMockProjectPath(Path.of(EMPTY_GRADLE_PROJECT_PATH, BUILD_GRADLE)).toFile();

    var result = subject.extract(gbf);

    assertNull(result.value());
    assertTrue(result.warnings().isEmpty());
    assertEquals(1, result.errors().size());
    assertInstanceOf(GBFReadJavaError.class, result.errors().getFirst());
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-java-block.gradle", "no-java-props.gradle"})
  void missingJavaProps_should_fail() {
    var gbf = getMockProjectPath(Path.of(JAVA_PLUGIN_PROJECT_DIR, "no-java-props.gradle")).toFile();

    var result = subject.extract(gbf);

    assertNull(result.value());
    var warnings = result.warnings();
    var errors = result.errors();
    assertTrue(warnings.isEmpty());
    assertEquals(1, errors.size());
    assertInstanceOf(GBFReadJavaError.class, errors.getFirst());
  }

  @Test
  void javaConfig_canBe_extracted() {
    var gbf = getMockProjectPath(Path.of(REAL_WORLD_GRADLE_PROJECT_PATH, BUILD_GRADLE)).toFile();

    var result = subject.extract(gbf);

    var java = result.value();
    assertEquals(GROUP, java.group());
    assertEquals(SOURCE_VERSION, java.sourceCompatibility());
    assertEquals(TARGET_VERSION, java.targetCompatibility());
    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void missingRequiredProps_should_fail() {
    var gbf =
        getMockProjectPath(Path.of(JAVA_PLUGIN_PROJECT_DIR, "no-required-props.gradle")).toFile();

    var result = subject.extract(gbf);

    assertNull(result.value());
    assertTrue(result.warnings().isEmpty());
    var errors = result.errors();
    assertEquals(2, errors.size());
    assertTrue(errors.stream().anyMatch(e -> e instanceof CouldNotReadJavaGroupError));
    assertTrue(errors.stream().anyMatch(e -> e instanceof CouldNotReadJavaSourceVersionError));
  }

  @Test
  void missingOptionalProps_shouldBe_warned() {
    var gbf = getMockProjectPath(Path.of(JAVA_PLUGIN_PROJECT_DIR, "no-target.gradle")).toFile();

    var result = subject.extract(gbf);

    var java = result.value();
    assertEquals(GROUP, java.group());
    assertEquals(SOURCE_VERSION, java.sourceCompatibility());
    assertEquals(SOURCE_VERSION, java.targetCompatibility());
    assertTrue(result.errors().isEmpty());
    assertEquals(1, result.warnings().size());
    assertInstanceOf(CouldNotReadJavaTargetVersionWarning.class, result.warnings().getFirst());
  }

  @Test
  void emptyJavaProps_should_fail() {
    var gbf =
        getMockProjectPath(Path.of(JAVA_PLUGIN_PROJECT_DIR, "empty-java-props.gradle")).toFile();

    var result = subject.extract(gbf);

    assertNull(result.value());
    var warnings = result.warnings();
    var errors = result.errors();
    assertEquals(1, warnings.size());
    assertInstanceOf(CouldNotReadJavaTargetVersionWarning.class, warnings.getFirst());
    assertEquals(2, errors.size());
    assertTrue(errors.stream().anyMatch(e -> e instanceof CouldNotReadJavaGroupError));
    assertTrue(errors.stream().anyMatch(e -> e instanceof CouldNotReadJavaSourceVersionError));
  }
}
