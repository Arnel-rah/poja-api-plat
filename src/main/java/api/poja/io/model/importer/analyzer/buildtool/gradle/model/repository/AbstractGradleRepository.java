package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
/*package-private*/ abstract sealed class AbstractGradleRepository implements GradleRepository
    permits FlatDirRepository, MavenRepository {
  protected RepositoryType type;
  protected final Map<String, Object> attributes;

  @Override
  public String toString() {
    return formatDeclaration();
  }
}
