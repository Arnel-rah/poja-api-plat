package api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer;

import static api.poja.io.model.importer.analyzer.Result.Status.FAILED;
import static api.poja.io.model.importer.analyzer.Result.Status.SUCCESS;

import api.poja.io.model.importer.analyzer.AbstractResult;
import api.poja.io.model.importer.model.ApplicationImportLog;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildToolAnalysisResult extends AbstractResult<BuildToolData> {
  private final Path root;
  private final BuildTool buildTool;

  @Setter
  @Accessors(fluent = true)
  private List<Path> buildToolFilePaths = List.of();

  public BuildToolAnalysisResult(Path root, BuildTool buildTool, ApplicationImportLog... logs) {
    super(Stream.of(logs).toList());
    this.root = root;
    this.buildTool = buildTool;
  }

  @Override
  public BuildToolData data() {
    return new BuildToolData(
        buildTool, buildToolFilePaths, buildToolFilePaths.stream().map(root::relativize).toList());
  }

  @Override
  public Status status() {
    return switch (buildTool) {
      case MAVEN, GRADLE -> SUCCESS;
      default -> FAILED;
    };
  }
}
