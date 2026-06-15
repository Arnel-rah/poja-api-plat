package api.poja.io.model.importer.mvn;

import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.model.importer.TestMocks.ANNOTATION_PROCESSOR_DEPS_FROM_MVN;
import static api.poja.io.model.importer.TestMocks.REAL_WORLD_GRADLE_DEPS;
import static api.poja.io.model.importer.TestMocks.REPOSITORIES_FROM_MVN;
import static api.poja.io.model.importer.TestMocks.SIMPLE_GRADLE_DEPS_FROM_MVN;
import static api.poja.io.model.importer.TestMocks.UNHANDLED_MAVEN_TAG_PATHS;
import static java.nio.file.Files.readString;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.JavaConfig;
import api.poja.io.model.importer.transformer.mvn.InvalidComponentError;
import api.poja.io.model.importer.transformer.mvn.InvalidPomXml;
import api.poja.io.model.importer.transformer.mvn.MavenToGradleConverter;
import api.poja.io.model.importer.transformer.mvn.MavenToGradleConverter.Result;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class MavenToGradleConverterTest {
  final MavenToGradleConverter subject = new MavenToGradleConverter();

  @SneakyThrows
  @Test
  void invalid_pom_xml() {
    var pom = getResource("files/import/mvn/invalid.xml").getFile();

    var result = subject.apply(pom);

    assertNull(result.value());
    assertTrue(result.warnings().isEmpty());

    var errors = result.errors();
    assertEquals(1, errors.size());

    var expectedError = new InvalidPomXml(pom, "missing <project> root element");
    var actualError = errors.getFirst();
    assertInstanceOf(expectedError.getClass(), actualError);
    assertEquals("missing <project> root element", actualError.message());
  }

  @SneakyThrows
  @Test
  void java_incorrect_groupId_cannotBe_converted() {
    var pom = getResource("files/import/mvn/java/incorrect-group.xml").getFile();

    var result = subject.apply(pom);

    assertNull(result.value());
    assertTrue(result.warnings().isEmpty());
    assertEquals(1, result.errors().size());
    var error = result.errors().getFirst();
    assertInstanceOf(InvalidComponentError.class, error);
    assertEquals(
        "groupId must include at least 2 lowercase alphanumeric segments", error.message());
  }

  @SneakyThrows
  @Test
  void java_canBe_converted() {
    var pom = getResource("files/import/mvn/java/correct.xml").getFile();

    var result = subject.apply(pom);

    assertTrue(result.errors().isEmpty());
    assertTrue(result.warnings().isEmpty());
    assertNotNull(result.value());
    assertEquals(correct_javaConfig(), result.value().gradleBuild().javaConfig());
  }

  @SneakyThrows
  @Test
  void javaOptionalProperties_canBe_omitted() {
    var pom = getResource("files/import/mvn/java/no-target.xml").getFile();

    var result = subject.apply(pom);

    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
    assertNotNull(result.value());
    var jc = result.value().gradleBuild().javaConfig();
    assertEquals(noTarget_javaConfig(), jc);
    assertEquals(noTargetJavaConfig_formatDeclaration(), jc.formatDeclaration());
  }

  @SneakyThrows
  @Test
  void mvnCompilerRelease_shouldBe_0() {
    var pom = getResource("files/import/mvn/java/release.xml").getFile();

    Result result = subject.apply(pom);

    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
    var java = result.value().gradleBuild().javaConfig();
    assertEquals("11", java.sourceCompatibility());
    assertEquals(java.sourceCompatibility(), java.targetCompatibility());
  }

  @SneakyThrows
  @Test
  void mvnSourceAndTarget_shouldBe_1() {
    var pom = getResource("files/import/mvn/java/target_and_source.xml").getFile();

    Result result = subject.apply(pom);

    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
    var java = result.value().gradleBuild().javaConfig();
    assertEquals("21", java.sourceCompatibility());
    assertEquals("17", java.targetCompatibility());
  }

  @SneakyThrows
  @Test
  void javaVersion_shouldBe_2() {
    var pom = getResource("files/import/mvn/java/java_version.xml").getFile();

    Result result = subject.apply(pom);

    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
    var java = result.value().gradleBuild().javaConfig();
    assertEquals("17", java.sourceCompatibility());
    assertEquals(java.sourceCompatibility(), java.targetCompatibility());
  }

  @SneakyThrows
  @Test
  void javaRequiredProperties_cannotBe_omitted() {
    var noGroupPom = getResource("files/import/mvn/java/no-group.xml").getFile();
    var noSourcePom = getResource("files/import/mvn/java/no-source.xml").getFile();

    Result noGroupResult = subject.apply(noGroupPom);
    Result noSourceResult = subject.apply(noSourcePom);

    assertNull(noGroupResult.value());
    assertNull(noSourceResult.value());
    assertTrue(noGroupResult.warnings().isEmpty());
    assertTrue(noSourceResult.warnings().isEmpty());
    assertEquals(1, noGroupResult.errors().size());
    assertEquals(1, noSourceResult.errors().size());

    var actualNoGroupError = noGroupResult.errors().getFirst();
    var actualNoSourceError = noSourceResult.errors().getFirst();

    assertInstanceOf(InvalidComponentError.class, actualNoGroupError);
    assertEquals("group cannot be null", actualNoGroupError.message());

    assertInstanceOf(InvalidComponentError.class, actualNoSourceError);
    assertEquals("sourceCompatibility cannot be null", actualNoSourceError.message());
  }

  @SneakyThrows
  @Test
  void simpleDeps_canBe_converted() {
    var pom = getResource("files/import/mvn/deps/simple-deps.xml").getFile();

    Result result = subject.apply(pom);

    assertNotNull(result.value());
    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());

    Set<String> actualDependencies =
        result.value().gradleBuild().dependencies().stream()
            .map(Object::toString)
            .collect(toUnmodifiableSet());
    assertEquals(SIMPLE_GRADLE_DEPS_FROM_MVN.length, actualDependencies.size());
    assertTrue(actualDependencies.containsAll(List.of(SIMPLE_GRADLE_DEPS_FROM_MVN)));
  }

  @SneakyThrows
  @Test
  void annotationProcessorDeps_canBe_converted() {
    var pom = getResource("files/import/mvn/deps/annotation-processor-deps.xml").getFile();

    Result result = subject.apply(pom);

    assertNotNull(result.value());
    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());

    Set<String> actualDependencies =
        result.value().gradleBuild().dependencies().stream()
            .map(Object::toString)
            .collect(toUnmodifiableSet());
    assertEquals(ANNOTATION_PROCESSOR_DEPS_FROM_MVN.length, actualDependencies.size());
    assertTrue(actualDependencies.containsAll(List.of(ANNOTATION_PROCESSOR_DEPS_FROM_MVN)));
  }

  @SneakyThrows
  @Test
  void repos_canBe_converted() {
    var pom = getResource("files/import/mvn/repos/full-repos.xml").getFile();

    Result result = subject.apply(pom);

    assertNotNull(result.value());
    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());

    Set<String> actualRepositories =
        result.value().gradleBuild().repositories().stream()
            .map(Object::toString)
            .collect(toUnmodifiableSet());
    assertEquals(REPOSITORIES_FROM_MVN.length, actualRepositories.size());
    assertTrue(actualRepositories.containsAll(List.of(REPOSITORIES_FROM_MVN)));
  }

  @SneakyThrows
  @Test
  void emptyDeps_canBe_converted() {
    var pom = getResource("files/import/mvn/empty-ext-pom.xml").getFile();

    Result result = subject.apply(pom);
    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
    assertNotNull(result.value());

    GradleBuild gradle = result.value().gradleBuild();
    assertTrue(gradle.dependencies().isEmpty());
    assertTrue(gradle.repositories().isEmpty());
    assertTrue(gradle.plugins().isEmpty());
  }

  @SneakyThrows
  @Test
  void realWorldPom_canBe_converted() {
    var pom = getResource("files/import/mvn/real-world-pom.xml").getFile();

    Result result = subject.apply(pom);
    assertTrue(result.warnings().isEmpty());
    assertTrue(result.errors().isEmpty());
    assertNotNull(result.value());

    GradleBuild gradle = result.value().gradleBuild();

    assertEquals(correct_javaConfig(), gradle.javaConfig());
    assertTrue(
        gradle.plugins().isEmpty()); // We are not able to convert maven plugin -> gradle plugin

    Set<String> actualDependencies =
        gradle.dependencies().stream().map(Object::toString).collect(toUnmodifiableSet());
    assertEquals(REAL_WORLD_GRADLE_DEPS.length, actualDependencies.size());
    assertTrue(actualDependencies.containsAll(List.of(REAL_WORLD_GRADLE_DEPS)));

    Set<String> actualRepositories =
        gradle.repositories().stream().map(Object::toString).collect(toUnmodifiableSet());
    assertEquals(REPOSITORIES_FROM_MVN.length, actualRepositories.size());
    assertTrue(actualRepositories.containsAll(List.of(REPOSITORIES_FROM_MVN)));
  }

  @SneakyThrows
  @Test
  void unhandledTags_shouldBe_reported() {
    var pom = getResource("files/import/mvn/real-world-pom.xml").getFile();

    Result result = subject.apply(pom);

    assertNotNull(result.value());
    List<String> actualUnhandledTagPaths =
        result.value().unhandledTagPaths().stream().sorted().toList();
    assertEquals(UNHANDLED_MAVEN_TAG_PATHS.length, actualUnhandledTagPaths.size());
    assertTrue(actualUnhandledTagPaths.containsAll(List.of(UNHANDLED_MAVEN_TAG_PATHS)));
  }

  @SneakyThrows
  @Test
  void postConversionXml_shouldBe_reported() {
    var pom = getResource("files/import/mvn/real-world-pom.xml").getFile();

    Result result = subject.apply(pom);

    assertNotNull(result.value());
    var expected =
        readString(getResource("files/import/mvn/real-world-leftover-pom.xml").getFile().toPath())
            .trim();
    assertEquals(expected, result.value().postConversionXml());
  }

  private static JavaConfig correct_javaConfig() {
    return new JavaConfig("api.poja.io", "21", "17");
  }

  private static JavaConfig noTarget_javaConfig() {
    // falls back to source if not present
    return new JavaConfig("api.poja.io", "21", null);
  }

  private static String noTargetJavaConfig_formatDeclaration() {
    return new JavaConfig("api.poja.io", "21", "21").toString();
  }
}
