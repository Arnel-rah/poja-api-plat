package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.RepositoryType.FLAT_DIR;

import api.poja.io.model.importer.util.StringFormatUtils;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
/*package-private*/ final class FlatDirRepository extends AbstractGradleRepository {
  private final List<String> dirs;

  public FlatDirRepository(List<String> dirs) {
    super(FLAT_DIR, Map.of());
    this.dirs = dirs;
  }

  @Override
  public String formatDeclaration() {
    var sj = new StringJoiner("\n", "flatDir {\n", "\n}");
    if (dirs.isEmpty()) {
      return sj.toString();
    }
    var dirJoiner = new StringJoiner(", ", "dirs ", "");
    dirs.stream().map(StringFormatUtils::formatSingleQuoted).forEach(dirJoiner::add);
    sj.add(dirJoiner.toString());
    return sj.toString();
  }
}
