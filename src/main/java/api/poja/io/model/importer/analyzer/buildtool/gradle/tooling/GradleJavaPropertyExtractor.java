package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import static java.nio.file.Files.readString;
import static java.util.regex.Pattern.MULTILINE;

import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.CouldNotReadJavaGroupError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.CouldNotReadJavaSourceVersionError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadJavaError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.CouldNotReadJavaTargetVersionWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.JavaConfig;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements the {@code RawGradlePropertyExtractor} interface. The {{@link
 * #extract(File)}} method extract the java plugin configuration for the specified `build.gradle`
 */
@Slf4j
public final class GradleJavaPropertyExtractor implements GradleFilePropertyExtractor<JavaConfig> {

  private static final Pattern JAVA_BLOCK_PATTERN =
      Pattern.compile("(?s)java\\s*\\{(.*?)}", MULTILINE);

  private static final Pattern JAVA_GROUP_PATTERN =
      Pattern.compile("(?m)^\\s*group\\s*=\\s*('.*?'|\".*?\")\\s*$");

  public static final Pattern JAVA_VERSION_PATTERN =
      Pattern.compile(
          "(?m)^\\s*(sourceCompatibility|targetCompatibility)\\s*=\\s*(JavaVersion\\.[A-Z_0-9]+|\\d+|'\\d+'|\"\\d+\")\\s*$");

  @Override
  public FallibleResult extract(File file) {
    List<GBFReadError> errors = new ArrayList<>();
    List<GBFReadWarning> warnings = new ArrayList<>();

    try {
      var content = readString(file.toPath());
      var blockMatcher = JAVA_BLOCK_PATTERN.matcher(content);

      if (!blockMatcher.find()) {
        return new FallibleResult(null, List.of(), List.of(new GBFReadJavaError(file)));
      }

      var javaBlock = blockMatcher.group(1);

      var groupMatcher = JAVA_GROUP_PATTERN.matcher(javaBlock);
      var group = groupMatcher.find() ? groupMatcher.group(1).replaceAll("['\"]", "") : null;

      var versionMatcher = JAVA_VERSION_PATTERN.matcher(javaBlock);
      Map<String, String> versions = new HashMap<>();
      while (versionMatcher.find()) {
        var property = versionMatcher.group(1);
        var value =
            versionMatcher.group(2).replace("JavaVersion.", "").replace("'", "").replace("\"", "");
        versions.put(property, value);
      }

      var sourceCompatibility = versions.get("sourceCompatibility");
      var targetCompatibility = versions.get("targetCompatibility");

      if (targetCompatibility == null || targetCompatibility.isBlank()) {
        warnings.add(new CouldNotReadJavaTargetVersionWarning(file));
      }

      if (group == null || group.isBlank()) {
        errors.add(new CouldNotReadJavaGroupError(file));
      }
      if (sourceCompatibility == null || sourceCompatibility.isBlank()) {
        errors.add(new CouldNotReadJavaSourceVersionError(file));
      }
      if (!errors.isEmpty()) {
        return new FallibleResult(null, warnings, errors);
      }

      var javaConfig = new JavaConfig(group, sourceCompatibility, targetCompatibility);
      return new FallibleResult(javaConfig, warnings, List.of());
    } catch (IOException e) {
      return new FallibleResult(null, warnings, List.of(new GBFReadJavaError(file)));
    }
  }

  public static final class FallibleResult
      extends api.poja.io.model.importer.model.FallibleResult<
          JavaConfig, GBFReadWarning, GBFReadError> {
    public FallibleResult(
        @Nullable JavaConfig value, List<GBFReadWarning> warnings, List<GBFReadError> errors) {
      super(value, warnings, errors);
    }
  }
}
