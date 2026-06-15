package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GradleRepositoryFactory {
  public static GradleRepository create(
      RepositoryType repositoryType, Map<String, Object> attributes) {
    return switch (repositoryType) {
      case MAVEN -> maven(attributes);
      case MAVEN_LOCAL -> mavenLocal(attributes);
      case FLAT_DIR -> flatDir(attributes);
      case IVY -> ivy(attributes);
    };
  }

  private static GradleRepository maven(Map<String, Object> attributes) {
    var name = (String) attributes.get("name");
    var url = (String) attributes.get("url");
    return new MavenRepository(name, url, attributes);
  }

  private static GradleRepository mavenLocal(Map<String, Object> attributes) {
    var name = (String) attributes.get("name");
    var url = (String) attributes.get("url");
    return new MavenLocalRepository(name, url, attributes);
  }

  private static GradleRepository flatDir(Map<String, Object> attributes) {
    Object dirs = attributes.getOrDefault("dirs", List.of());
    if (dirs instanceof List) {
      return new FlatDirRepository((List<String>) dirs);
    } else if (dirs instanceof String dir) {
      return new FlatDirRepository(List.of(dir));
    }
    throw new IllegalStateException("Unexpected type for dirs " + dirs);
  }

  private static GradleRepository ivy(Map<String, Object> attributes) {
    assert attributes != null;
    throw new UnsupportedOperationException("ivy is currently not supported");
  }
}
