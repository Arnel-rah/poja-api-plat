package api.poja.io.model.importer.deps;

import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;
import static api.poja.io.repository.model.enums.ApplicationImportLogType.ERROR;

import api.poja.io.model.importer.analyzer.AbstractResult;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.ConflictResolutionData;
import java.util.List;

public class DepsConflictResolverResult extends AbstractResult<ConflictResolutionData> {
  private final List<GradleDependency> resolvedDependencies;

  public DepsConflictResolverResult(
      List<GradleDependency> resolvedDependencies, List<ApplicationImportLog> logs) {
    super(logs);
    this.resolvedDependencies = resolvedDependencies;
  }

  @Override
  public ConflictResolutionData data() {
    return new ConflictResolutionData(resolvedDependencies);
  }

  @Override
  public Status status() {
    return logs().stream().anyMatch(log -> log.getType() == ERROR) ? FAILED : SUCCESS;
  }
}
