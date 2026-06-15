package api.poja.io.model.importer.gradle.tooling;

import static api.poja.io.model.importer.TestMocks.INCORRECT_GRADLE_PROJECT_DIR;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_DEPS;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_JAVA_CONFIG;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PLUGINS;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_REPOSITORIES;
import static api.poja.io.model.importer.TestUtils.mockGradleProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.CouldNotReadJavaGroupError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.CouldNotReadJavaSourceVersionError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.CouldNotReadJavaTargetVersionWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleBuildExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradleBuildExtractorTest {
  private final GradleBuildExtractor subject = GradleBuildExtractor.DEFAULT;

  @Test
  void declaredComponents_canBe_extracted() {
    mockGradleProject(
        REAL_WORLD_GRADLE_PROJECT_PATH,
        project -> {
          var result = subject.extract(project);

          assertTrue(result.errors().isEmpty());
          assertTrue(result.warnings().isEmpty());
          assertTrue(result.isSuccess());

          var build = result.value();
          assertEquals(REAL_WORLD_GRADLE_DEPS.length, build.dependencies().size());
          assertEquals(REAL_WORLD_GRADLE_PLUGINS.length, build.plugins().size());
          assertEquals(REAL_WORLD_GRADLE_REPOSITORIES.length, build.repositories().size());
          assertEquals(REAL_WORLD_GRADLE_JAVA_CONFIG, build.javaConfig());
        });
  }

  @Test
  void errorsAndWarnings_shouldBe_reported() {
    mockGradleProject(
        INCORRECT_GRADLE_PROJECT_DIR,
        project -> {
          var result = subject.extract(project);

          var build = result.value();
          assertFalse(result.isSuccess());
          assertTrue(build.dependencies().isEmpty());
          assertEquals(1, build.plugins().size());
          assertEquals(
              List.of("id 'java'"), build.plugins().stream().map(Object::toString).toList());
          assertTrue(build.repositories().isEmpty());
          assertNull(build.javaConfig());

          var errors = result.errors();
          var warnings = result.warnings();
          assertEquals(2, errors.size());
          assertEquals(1, warnings.size());
          assertTrue(
              errors.stream().anyMatch(e -> e instanceof CouldNotReadJavaSourceVersionError));
          assertTrue(errors.stream().anyMatch(e -> e instanceof CouldNotReadJavaGroupError));
          assertTrue(
              warnings.stream().anyMatch(e -> e instanceof CouldNotReadJavaTargetVersionWarning));
        });
  }
  // TODO: test: ko?
}
