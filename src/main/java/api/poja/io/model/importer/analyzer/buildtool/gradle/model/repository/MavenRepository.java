package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.RepositoryType.MAVEN;
import static api.poja.io.model.importer.util.StringFormatUtils.formatAssign;
import static api.poja.io.model.importer.util.StringFormatUtils.formatDoubleQuoted;
import static api.poja.io.model.importer.util.StringFormatUtils.formatUri;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public sealed class MavenRepository extends AbstractGradleRepository permits MavenLocalRepository {
  // TODO: credentials
  private final String name;
  private final String url;

  public MavenRepository(String name, String url, Map<String, Object> attributes) {
    super(MAVEN, attributes);
    this.name = name;
    this.url = url;
  }

  public MavenRepository(String name, String url) {
    super(MAVEN, Map.of());
    this.name = name;
    this.url = url;
  }

  @Override
  public String formatDeclaration() {
    if (isMavenCentral()) {
      return "mavenCentral()";
    }
    if (isGradlePluginPortal()) {
      return "gradlePluginPortal()";
    }
    if (isGoogle()) {
      return "google()";
    }
    var sj = new StringJoiner("\n", "maven {\n", "\n}");
    if (name != null && !Objects.equals(MAVEN_REPO_DEFAULT_NAME, name)) {
      sj.add(formatName());
    }
    sj.add(formatUrl());
    return sj.toString();
  }

  protected final String formatName() {
    return formatAssign("name", formatDoubleQuoted(name));
  }

  protected final String formatUrl() {
    return formatAssign("url", formatUri(url));
  }

  private boolean isGradlePluginPortal() {
    return Objects.equals(GRADLE_PLUGIN_PORTAL_REPO_NAME, name)
        && (Objects.equals(GRADLE_PLUGIN_PORTAL_URL, url)
            || Objects.equals(GRADLE_PLUGIN_PORTAL_URL + "/", url));
  }

  private boolean isGoogle() {
    return Objects.equals(GOOGLE_REPO_NAME, name)
        && (Objects.equals(GOOGLE_URL, url) || Objects.equals(GOOGLE_URL + "/", url));
  }

  private boolean isMavenCentral() {
    return Objects.equals(DEFAULT_MAVEN_CENTRAL_REPO_NAME, name)
        && (Objects.equals(MAVEN_CENTRAL_URL, url) || Objects.equals(MAVEN_CENTRAL_URL + "/", url));
  }
}
