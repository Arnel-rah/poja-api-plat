package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.RepositoryType.MAVEN_LOCAL;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class MavenLocalRepository extends MavenRepository {
  public MavenLocalRepository(String name, String url, Map<String, Object> attributes) {
    super(name, url, attributes);
    this.type = MAVEN_LOCAL;
  }

  @Override
  public String formatDeclaration() {
    if (isDefaultMavenLocal()) {
      return "mavenLocal()";
    }
    var sj = new StringJoiner("\n", "mavenLocal {\n", "\n}");
    if (name() != null) {
      sj.add(formatName());
    }
    sj.add(formatUrl());
    return sj.toString();
  }

  private boolean isDefaultMavenLocal() {
    return Objects.equals(DEFAULT_MAVEN_LOCAL_REPO_NAME, name())
        && (url().endsWith(DEFAULT_MAVEN_LOCAL_URL_END)
            || url().endsWith(DEFAULT_MAVEN_LOCAL_URL_END + "/"));
  }
}
