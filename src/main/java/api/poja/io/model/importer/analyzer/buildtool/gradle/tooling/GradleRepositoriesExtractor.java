package api.poja.io.model.importer.analyzer.buildtool.gradle.tooling;

import static java.lang.Integer.parseInt;
import static java.nio.file.Files.readString;
import static java.util.UUID.randomUUID;

import api.poja.io.datastructure.Pair;
import api.poja.io.file.FileWriter;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFExtractRepoError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepositoryFactory;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.RepositoryType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * This class implements the {@code ProjectAwareGradlePropertyExtractor} interface. The {{@link
 * #extract(GradleProject)}} method extract all the dependencies for the specified {@link
 * GradleProject}
 */
@Slf4j
public final class GradleRepositoriesExtractor
    implements ProjectAwareGradlePropertyExtractor<List<GradleRepository>> {

  private static final String REPOSITORY_TYPENAME_ATTR_KEY = "typename";
  private static final Pattern REPOSITORY_LINE_INDEX_START_PATTERN =
      Pattern.compile("^(\\d+)=(.+)");
  private static final Pattern REPOSITORY_ATTR_PATTERN = Pattern.compile("^(\\w+):(\\w.*)$");

  private final FileWriter fileWriter;
  private final String repositoryLineStart;
  private final String taskName;

  public GradleRepositoriesExtractor(FileWriter fileWriter) {
    this.fileWriter = fileWriter;
    this.repositoryLineStart = String.format("%s=", randomUUID());
    this.taskName = String.format("listRepositories%s", randomUUID());
  }

  @Override
  public FallibleResult extract(GradleProject gradleProject) {
    var build = gradleProject.buildFile();

    try {
      var buildContent = readString(build.toPath());
      var newBuildContent = addPropertyExtractorTask(buildContent);

      fileWriter.write(newBuildContent.getBytes(), null, build.getPath());
      gradleProject.refreshProjectFiles(build.toPath());

      try {
        var output = gradleProject.runTask(taskName);
        return new FallibleResult(parseOutput(output), List.of(), List.of());
      } finally {
        fileWriter.write(buildContent.getBytes(), null, build.getPath());
      }
    } catch (IOException e) {
      return new FallibleResult(null, List.of(), List.of(new GBFExtractRepoError(build)));
    }
  }

  /**
   * Transforms the list repositories task stdout to gradle repository
   *
   * <p>gradle repository is build from index-grouped attribute (key=value) whose format is as
   * follows "INDEX=KEY:VALUE"
   *
   * <pre>
   *   0=key1:value1
   *   0=key2:value2
   *   1=key1:value3
   * </pre>
   *
   * {@link GradleRepository}
   */
  private List<GradleRepository> parseOutput(String stdout) {
    List<Map<String, Object>> indexLineAttributes = new ArrayList<>();

    var lines =
        Arrays.stream(stdout.split("\\R"))
            .map(this::getLineWithoutStartOpt)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    // Parse a line with a potential index and key=value pair, then merge it into the
    // corresponding entry in indexLineAttributes. If the same key appears multiple times
    // for the same index, values are accumulated into a list. Non-indexed or invalid
    // lines are skipped. The list grows dynamically to match the highest index found.
    for (var line : lines) {
      Pair<Integer, String> indexedLine = extractIndexedLine(line);
      var index = indexedLine.first();
      if (index < 0) {
        // Drop non-indexed line
        continue;
      }

      var keyValueOpt = extractKeyValue(indexedLine.second());
      if (keyValueOpt.isEmpty()) {
        // Drop non-key-value
        continue;
      }

      var keyValue = keyValueOpt.get();

      // insert new map entry if none exists
      if (indexLineAttributes.size() <= index) {
        Map<String, Object> attributeMap =
            new HashMap<>(Map.of(keyValue.first(), keyValue.second()));
        indexLineAttributes.add(attributeMap);
        continue;
      }

      var attributeMap = indexLineAttributes.get(index);
      var key = keyValue.first();
      var value = keyValue.second();

      if (!attributeMap.containsKey(key)) {
        attributeMap.put(key, value);
        continue;
      }

      var currentValue = attributeMap.get(key);
      if (currentValue instanceof List<?> list) {
        // todo: unchecked?
        ((List<Object>) list).add(value);
      } else {
        List<Object> list = new ArrayList<>();
        list.add(currentValue);
        list.add(value);
        attributeMap.put(key, list);
      }
    }

    return indexLineAttributes.stream()
        .map(GradleRepositoriesExtractor::fromAttributes)
        .filter(Objects::nonNull)
        .toList();
  }

  private static GradleRepository fromAttributes(Map<String, Object> attributes) {
    var typename = (String) attributes.get(REPOSITORY_TYPENAME_ATTR_KEY);
    if (typename == null) {
      return null;
    }
    try {
      var type = RepositoryType.fromValue(typename);
      return GradleRepositoryFactory.create(type, attributes);
    } catch (IllegalArgumentException e) {
      log.error("GradleRepositoryFactory.create() failed for typename {}", typename, e);
      return null;
    }
  }

  /**
   * Returns an optional key,value {@code Pair} from a string whose format is "KEY:VALUE" Returns
   *
   * <p>Optional.empty() if {@code s} doesn't match the format
   */
  private Optional<Pair<String, String>> extractKeyValue(String s) {
    var matcher = REPOSITORY_ATTR_PATTERN.matcher(s);
    if (matcher.find()) {
      var attr = new Pair<>(matcher.group(1), matcher.group(2));
      return Optional.of(attr);
    }
    return Optional.empty();
  }

  private Pair<Integer, String> extractIndexedLine(String s) {
    var matcher = REPOSITORY_LINE_INDEX_START_PATTERN.matcher(s);
    if (matcher.find()) {
      var indexStr = matcher.group(1);
      var lineContinue = matcher.group(2);
      return new Pair<>(parseInt(indexStr), lineContinue);
    }
    return new Pair<>(-1, null);
  }

  private Optional<String> getLineWithoutStartOpt(String s) {
    if (s.length() == repositoryLineStart.length() || !s.startsWith(repositoryLineStart)) {
      return Optional.empty();
    }
    var lineWithoutStart = s.substring(repositoryLineStart.length());
    return Optional.of(lineWithoutStart);
  }

  private String addPropertyExtractorTask(String s) {
    return String.join("\n", s, listDepsTask());
  }

  private String listDepsTask() {
    return String.format(
        """
tasks.register("%s") {
    doLast {
        repositories.eachWithIndex { repo, i ->
            def idx = String.format("%s%%02d", i) // e.g., 01, 02, 03
            if (repo instanceof MavenArtifactRepository) {
                if (repo instanceof org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository) {
                    println "${idx}=typename:mavenLocal"
                } else {
                    println "${idx}=typename:maven"
                }
                println "${idx}=name:${repo.name}"
                println "${idx}=url:${repo.url}"
            } else if (repo instanceof IvyArtifactRepository) {
                println "${idx}=typename:ivy"
                println "${idx}=name:${repo.name}"
                println "${idx}=url:${repo.url}"
            } else if (repo instanceof FlatDirectoryArtifactRepository) {
                println "${idx}=typename:flatDir"
                println "${idx}=name:${repo.name}"
                repo.dirs.each { dir ->
                    def relPath = project.projectDir.toPath().relativize(dir.toPath()).toString()
                    println "${idx}=dirs:${relPath}"
                }
           }
       }
    }
}
""",
        taskName, repositoryLineStart);
  }

  public static final class FallibleResult
      extends api.poja.io.model.importer.model.FallibleResult<
          List<GradleRepository>, GBFReadWarning, GBFReadError> {
    public FallibleResult(
        @Nullable List<GradleRepository> value,
        List<GBFReadWarning> warnings,
        List<GBFReadError> errors) {
      super(value, warnings, errors);
    }
  }
}
