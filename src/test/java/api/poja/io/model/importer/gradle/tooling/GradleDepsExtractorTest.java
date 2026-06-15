package api.poja.io.model.importer.gradle.tooling;

import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_DEPS_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_DEPS;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestUtils.fileWriter;
import static api.poja.io.model.importer.TestUtils.mockGradleProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleDependenciesExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GradleDepsExtractorTest {

  private final GradleDependenciesExtractor subject = new GradleDependenciesExtractor(fileWriter);

  @Test
  void declaredGradleDeps_canBe_extracted() {
    mockGradleProject(
        REAL_WORLD_GRADLE_PROJECT_PATH,
        project -> {
          var result = subject.extract(project);

          List<GradleDependency> dependencies = result.value();
          assertEquals(REAL_WORLD_GRADLE_DEPS.length, dependencies.size());
          assertTrue(
              dependencies.stream()
                  .map(GradleDependency::toString)
                  .toList()
                  .containsAll(List.of(REAL_WORLD_GRADLE_DEPS)));
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {EMPTY_GRADLE_PROJECT_PATH, EMPTY_GRADLE_DEPS_PROJECT_PATH})
  void emptyGradleDeps_shouldBe_empty_whenExtracted(String path) {
    mockGradleProject(
        path,
        project -> {
          var result = subject.extract(project);

          List<GradleDependency> dependencies = result.value();
          assertTrue(dependencies.isEmpty());
        });
  }
}
