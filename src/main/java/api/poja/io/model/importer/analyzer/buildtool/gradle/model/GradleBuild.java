package api.poja.io.model.importer.analyzer.buildtool.gradle.model;

import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradlePlugin.JAVA_PLUGIN;
import static api.poja.io.model.importer.util.StringFormatUtils.formatBlock;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder(toBuilder = true)
public record GradleBuild(
    File file,
    @Nonnull List<GradleDependency> dependencies,
    @Nonnull List<GradleRepository> repositories,
    @Nonnull List<GradlePlugin> plugins,
    @Nullable JavaConfig javaConfig) {
  public GradleBuild(
      @Nonnull List<GradleDependency> dependencies,
      @Nonnull List<GradleRepository> repositories,
      @Nonnull List<GradlePlugin> plugins,
      @Nullable JavaConfig javaConfig) {
    this(null, dependencies, repositories, plugins, javaConfig);
  }

  /**
   * Returns the {@code build.gradle} string of this GradleBuild. This is just the string returned
   * by the {@link #formatDeclaration} method
   */
  @Override
  public String toString() {
    return formatDeclaration();
  }

  /**
   * Returns the {@code build.gradle} string of this GradleBuild, including plugins, dependencies,
   * repositories, and Java configuration if present.
   *
   * <p>note(!): Maven plugin conversion is not supported. The Java plugin is returned if javaConfig
   * is present and not already declared.
   */
  public String formatDeclaration() {
    var sj = new StringJoiner("\n\n");

    boolean hasJavaConfig = javaConfig != null;

    var pluginsBlock = formatBlock("plugins");
    if (hasJavaConfig) {
      var javaPluginOpt = plugins.stream().filter(e -> JAVA_PLUGIN.id().equals(e.id())).findFirst();
      if (javaPluginOpt.isEmpty()) {
        pluginsBlock.add(JAVA_PLUGIN.toString());
      }
    }
    plugins.stream().map(Objects::toString).forEach(pluginsBlock::add);
    sj.add(pluginsBlock.toString());

    if (javaConfig != null) {
      sj.add(javaConfig.toString());
    }

    var repositoriesBlock = formatBlock("repositories");
    repositories.stream().map(Object::toString).forEach(repositoriesBlock::add);
    sj.add(repositoriesBlock.toString());

    var dependenciesBlock = formatBlock("dependencies");
    dependencies.stream().map(Object::toString).distinct().forEach(dependenciesBlock::add);
    sj.add(dependenciesBlock.toString());

    return sj.toString();
  }
}
