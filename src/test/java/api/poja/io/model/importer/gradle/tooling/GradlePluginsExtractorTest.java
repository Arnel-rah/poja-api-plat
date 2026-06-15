package api.poja.io.model.importer.gradle.tooling;

import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_DEPS_PLUGINS;
import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_DEPS_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.EMPTY_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PLUGINS;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_PROJECT_PATH;
import static api.poja.io.model.importer.TestUtils.fileWriter;
import static api.poja.io.model.importer.TestUtils.mockGradleProject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradlePlugin;
import api.poja.io.model.importer.analyzer.buildtool.gradle.tooling.GradlePluginsExtractor;
import java.util.List;
import org.junit.jupiter.api.Test;

class GradlePluginsExtractorTest {

  private final GradlePluginsExtractor subject = new GradlePluginsExtractor(fileWriter);

  @Test
  void emptyGradlePlugins_shouldBe_empty_whenExtracted() {
    mockGradleProject(
        EMPTY_GRADLE_PROJECT_PATH,
        project -> {
          var result = subject.extract(project);

          List<GradlePlugin> plugins = result.value();
          assertTrue(plugins.isEmpty());
        });
  }

  @Test
  void declaredGradlePlugins_canBe_extracted() {
    mockGradleProject(
        REAL_WORLD_GRADLE_PROJECT_PATH,
        project -> {
          var result = subject.extract(project);

          List<GradlePlugin> plugins = result.value();
          assertEquals(REAL_WORLD_GRADLE_PLUGINS.length, plugins.size());
          assertTrue(
              plugins.stream()
                  .map(GradlePlugin::toString)
                  .toList()
                  .containsAll(List.of(REAL_WORLD_GRADLE_PLUGINS)));
        });

    mockGradleProject(
        EMPTY_GRADLE_DEPS_PROJECT_PATH,
        project -> {
          var result = subject.extract(project);

          List<GradlePlugin> plugins = result.value();
          assertEquals(EMPTY_GRADLE_DEPS_PLUGINS.length, plugins.size());
          assertTrue(
              plugins.stream()
                  .map(GradlePlugin::toString)
                  .toList()
                  .containsAll(List.of(EMPTY_GRADLE_DEPS_PLUGINS)));
        });
  }
}
