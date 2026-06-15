package api.poja.io.model.importer.gradle.tooling;

import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_DEPS_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_REPOSITORIES;
import static api.poja.io.model.importer.TestUtils.fileWriter;
import static api.poja.io.model.importer.TestUtils.mockGradleProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradleRepositoriesExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GradleReposExtractorTest {

  private final GradleRepositoriesExtractor subject = new GradleRepositoriesExtractor(fileWriter);

  @ParameterizedTest
  @ValueSource(strings = {EMPTY_GRADLE_PROJECT_PATH, EMPTY_GRADLE_DEPS_PROJECT_PATH})
  void emptyGradleRepos_shouldBe_empty_whenExtracted(String path) {
    mockGradleProject(
        path,
        project -> {
          var result = subject.extract(project);

          List<GradleRepository> dependencies = result.value();
          assertTrue(dependencies.isEmpty());
        });
  }

  @Test
  void declaredGradleRepos_canBe_extracted() {
    mockGradleProject(
        REAL_WORLD_GRADLE_PROJECT_PATH,
        project -> {
          var result = subject.extract(project);

          List<GradleRepository> repos = result.value();
          assertEquals(REAL_WORLD_GRADLE_REPOSITORIES.length, repos.size());
          var actualRepoDeclarations =
              repos.stream().map(GradleRepository::formatDeclaration).toList();
          assertTrue(actualRepoDeclarations.containsAll(List.of(REAL_WORLD_GRADLE_REPOSITORIES)));
        });
  }
}
