package api.poja.io.model.importer.transformer.mvn;

import static api.poja.io.model.importer.model.FallibleResult.ofFallible;
import static java.util.Objects.requireNonNullElse;
import static java.util.function.Predicate.not;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradlePlugin;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.JavaConfig;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.MavenRepository;
import api.poja.io.model.importer.model.FallibleResult;
import api.poja.io.model.importer.transformer.mvn.MavenToGradleConverter.Result.Value;
import api.poja.io.model.importer.transformer.utils.Xmls;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Slf4j
@Component
public class MavenToGradleConverter implements Function<File, MavenToGradleConverter.Result> {

  private static final String POM_XML_ROOT_ELEMENT_TAG_NAME = "project";
  private static final String APACHE_MVN_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
  private static final String MVN_COMPILER_PLUGIN_ARTIFACT_ID = "maven-compiler-plugin";

  @Override
  public MavenToGradleConverter.Result apply(File file) {
    try {
      Document xml = Xmls.parseXml(file);

      if (!isValidPom(xml)) {
        return new Result(
            null, List.of(), List.of(new InvalidPomXml(file, "missing <project> root element")));
      }

      Element root = xml.getDocumentElement();

      // note(!): Failure occurs when a Gradle component property validations fail
      var javaResult = ofFallible(() -> findJavaConfig(root));
      var dependenciesResult = ofFallible(() -> findDependencies(root));
      var repositoriesResult = ofFallible(() -> findRepositories(root));

      var pluginsResult = ofFallible(List::<GradlePlugin>of);
      log.warn("maven plugin cannot be converted to gradle plugin");

      var findErrors =
          Stream.of(javaResult, dependenciesResult, repositoriesResult, pluginsResult)
              .flatMap(e -> e.errors().stream())
              .toList();

      if (!findErrors.isEmpty()) {
        var message = String.join("\n", findErrors.stream().map(Exception::getMessage).toList());
        return new Result(
            null,
            List.of(),
            findErrors.stream()
                .<MvnReadError>map(e -> new InvalidComponentError(file, message))
                .toList());
      }

      var gradleBuild =
          new GradleBuild(
              file,
              requireNonNullElse(dependenciesResult.value(), List.of()),
              requireNonNullElse(repositoriesResult.value(), List.of()),
              requireNonNullElse(pluginsResult.value(), List.of()),
              javaResult.value());

      List<String> unhandledTags =
          Xmls.retainDeepestTagPaths(Xmls.collectTagPaths(root)).stream().toList();

      String postConversionXml = Xmls.printXml(root);

      return new Result(
          new Value(gradleBuild, unhandledTags, postConversionXml), List.of(), List.of());
    } catch (RuntimeException e) {
      return new Result(null, List.of(), List.of(new MvnReadError(file)));
    }
  }

  /**
   * Resolves the effective Java version from Maven properties.
   *
   * <p>All properties are typically defined under the <properties> section. The evaluation order
   * is: 1. maven.compiler.release — highest priority, defines both source and target. 2.
   * maven.compiler.source and maven.compiler.target — define language level and bytecode version.
   * 3. java.version — lowest priority, used only as a fallback or general metadata. The result
   * represents the Java version actually used during compilation.
   */
  private static JavaConfig findJavaConfig(Element root) {
    var groupId = Xmls.text(root, "groupId");
    var propertiesOpt = Xmls.findElement(root, "properties");
    if (propertiesOpt.isEmpty()) {
      return new JavaConfig(groupId, null, null);
    }

    var properties = propertiesOpt.get();
    var jvmRelease = Xmls.text(properties, "maven.compiler.release");
    var jvmSource = Xmls.text(properties, "maven.compiler.source");
    var jvmTarget = Xmls.text(properties, "maven.compiler.target");
    var javaVersion = Xmls.text(properties, "java.version");
    Xmls.removeIfEmpty(properties);

    if (jvmRelease != null && !jvmRelease.isBlank()) {
      return new JavaConfig(groupId, jvmRelease, jvmRelease);
    }

    if ((jvmSource != null && !jvmSource.isBlank())
        || (jvmTarget != null && !jvmTarget.isBlank())) {
      return new JavaConfig(groupId, jvmSource, jvmTarget);
    }

    return new JavaConfig(groupId, javaVersion, javaVersion);
  }

  private static List<GradleDependency> findDependencies(Element root) {
    List<GradleDependency> dependencies = new ArrayList<>(getRegularDependencies(root));

    var annotationProcessorsAsDependencies = getAnnotationProcessorsAsDependencies(root);
    dependencies.addAll(annotationProcessorsAsDependencies);
    dependencies.addAll(
        annotationProcessorsAsDependencies.stream()
            .map(MavenToGradleConverter::mapAnnotationProcessorDependencyToRegular)
            .toList());

    return dependencies;
  }

  private static GradleDependency mapAnnotationProcessorDependencyToRegular(
      GradleDependency annotationProcessorDependency) {
    var configuration =
        annotationProcessorDependency.configuration().startsWith("test")
            ? "testCompileOnly"
            : "compileOnly";
    return annotationProcessorDependency.toBuilder().configuration(configuration).build();
  }

  private static List<GradleDependency> getRegularDependencies(Element root) {
    var parentOpt = Xmls.findElement(root, "dependencies");
    if (parentOpt.isEmpty()) {
      return List.of();
    }
    var parent = parentOpt.get();

    var dependencyElements = Xmls.getChildElements(parent, "dependency");
    var dependencies =
        dependencyElements.stream()
            .map(
                element -> {
                  var scope = Xmls.textOrDefault(element, "scope", "compile");
                  return new GradleDependency(
                      fromMvnScopeToGradleConfigName(scope),
                      Xmls.text(element, "groupId"),
                      Xmls.text(element, "artifactId"),
                      Xmls.text(element, "version"));
                })
            .toList();
    dependencyElements.forEach(Xmls::removeIfEmpty);
    Xmls.removeIfEmpty(parent);
    return dependencies;
  }

  private static List<GradleDependency> getAnnotationProcessorsAsDependencies(Element root) {
    List<GradleDependency> dependencies = new ArrayList<>();

    var buildPluginsOpt =
        Xmls.findElement(root, "build").flatMap(b -> Xmls.findElement(b, "plugins"));
    if (buildPluginsOpt.isEmpty()) {
      return dependencies;
    }

    var pluginElements = Xmls.getChildElements(buildPluginsOpt.get(), "plugin");

    var mvnCompilerPluginConfigurationOpt =
        pluginElements.stream()
            .filter(MavenToGradleConverter::isMavenCompilerPlugin)
            .findFirst()
            .flatMap(mcp -> Xmls.findElement(mcp, "configuration"));
    if (mvnCompilerPluginConfigurationOpt.isEmpty()) {
      return dependencies;
    }

    dependencies.addAll(
        getAnnotationProcessorPathsAsDependencies(
            mvnCompilerPluginConfigurationOpt.get(), "annotationProcessor"));

    dependencies.addAll(
        getAnnotationProcessorPathsAsDependencies(
            mvnCompilerPluginConfigurationOpt.get(), "testAnnotationProcessor"));

    return dependencies;
  }

  private static List<GradleDependency> getAnnotationProcessorPathsAsDependencies(
      Element mvnCompilerConfigurationElement, String annotationProcessorPropertyName) {
    var annotationProcessorPathsOpt =
        Xmls.findElement(
            mvnCompilerConfigurationElement, annotationProcessorPropertyName + "Paths");
    return annotationProcessorPathsOpt
        .map(
            annotationProcessorPaths -> {
              var paths = Xmls.getChildElements(annotationProcessorPaths, "path");
              var dependencies =
                  paths.stream()
                      .map(
                          e ->
                              new GradleDependency(
                                  annotationProcessorPropertyName,
                                  Xmls.text(e, "groupId"),
                                  Xmls.text(e, "artifactId"),
                                  Xmls.text(e, "version")))
                      .toList();
              paths.forEach(Xmls::removeIfEmpty);
              Xmls.removeIfEmpty(annotationProcessorPaths);
              return dependencies;
            })
        .orElseGet(List::of);
  }

  private static boolean isMavenCompilerPlugin(Element plugin) {
    var groupId = Xmls.text(plugin, "groupId");
    var artifactId = Xmls.text(plugin, "artifactId");
    Xmls.removeElement(plugin, "version");
    if (groupId == null || artifactId == null) {
      return false;
    }
    return APACHE_MVN_PLUGIN_GROUP_ID.equals(groupId)
        && MVN_COMPILER_PLUGIN_ARTIFACT_ID.equals(artifactId);
  }

  private static List<GradleRepository> findRepositories(Element root) {
    var parentOpt = Xmls.findElement(root, "repositories");
    if (parentOpt.isEmpty()) {
      return List.of();
    }
    var parent = parentOpt.get();

    var repositoryElements = Xmls.getChildElements(parent, "repository");
    return repositoryElements.stream()
        .<GradleRepository>map(
            element -> new MavenRepository(Xmls.text(element, "name"), Xmls.text(element, "url")))
        .toList();
  }

  /** note(/!\): maven plugins are not convertible to gradle as they are not compatible */
  private static List<GradlePlugin> findPlugins(Element root) {
    var parentOpt = Xmls.findElement(root, "plugins");
    if (parentOpt.isEmpty()) {
      return List.of();
    }
    var parent = parentOpt.get();

    var pluginElements =
        Xmls.getChildElements(parent, "plugin").stream()
            .filter(not(MavenToGradleConverter::isMavenCompilerPlugin))
            .toList();
    return pluginElements.stream()
        .map(
            element ->
                new GradlePlugin(Xmls.text(element, "artifactId"), Xmls.text(element, "version")))
        .toList();
  }

  private static String fromMvnScopeToGradleConfigName(String scope) {
    return switch (scope) {
      case "compile" -> "implementation";
      case "provided" -> "compileOnly";
      case "runtime" -> "runtimeOnly";
      case "test" -> "testImplementation";
      case "system" -> "compileOnly"; // note: approx.
      case "testRuntime" -> "testRuntimeOnly";
      default -> "implementation";
    };
  }

  private static boolean isValidPom(Document doc) {
    if (doc == null) {
      return false;
    }
    Element root = doc.getDocumentElement();
    return root != null && POM_XML_ROOT_ELEMENT_TAG_NAME.equals(root.getTagName());
  }

  public static final class Result extends FallibleResult<Value, MvnReadWarning, MvnReadError> {
    // TODO: maybe Result.Value should keep the post-conversion XML for a better reporting (store in
    //       s3 or something)
    public record Value(
        GradleBuild gradleBuild, List<String> unhandledTagPaths, String postConversionXml) {}

    public Result(@Nullable Value value, List<MvnReadWarning> warnings, List<MvnReadError> errors) {
      super(value, warnings, errors);
    }
  }
}
