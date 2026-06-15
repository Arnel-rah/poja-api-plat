package api.poja.io.model.importer.analyzer.buildtool.gradle.analyzer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;

/**
 * @param buildTool
 * @param rawBuildToolFilePaths Always null if dese from its string format
 * @param relativeBuildToolFilePaths
 */
public record BuildToolData(
    BuildTool buildTool,
    @JsonIgnore @Nullable List<Path> rawBuildToolFilePaths,
    @JsonProperty("buildToolFilePaths") @JsonSerialize(contentUsing = ToStringSerializer.class)
        List<Path> relativeBuildToolFilePaths) {
  public BuildToolData(BuildTool buildTool, List<Path> buildToolFilePaths) {
    this(buildTool, null, buildToolFilePaths);
  }
}
