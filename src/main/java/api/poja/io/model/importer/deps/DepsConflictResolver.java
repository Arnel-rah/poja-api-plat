package api.poja.io.model.importer.deps;

import static api.poja.io.model.importer.model.ApplicationImportLog.error;
import static api.poja.io.model.importer.model.ApplicationImportLog.warning;
import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.compile;

import api.poja.io.model.PojaVersion;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.deps.error.DependencyConflictError;
import api.poja.io.model.importer.deps.pojaDeps.PojaDependencies;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.model.importer.model.FallibleResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.stereotype.Component;

@Component
public class DepsConflictResolver
    implements BiFunction<List<GradleDependency>, PojaVersion, DepsConflictResolverResult> {
  private static final Pattern VERSION_PATTERN = compile("^\\d+\\.\\d+\\.\\d+.*");

  @Override
  public DepsConflictResolverResult apply(
      List<GradleDependency> userDependencies, PojaVersion pojaVersion) {
    var pojaDependencies = PojaDependencies.forVersion(pojaVersion);
    var resolutionResult = resolveDependencies(userDependencies, pojaDependencies);

    List<ApplicationImportLog> logs = new ArrayList<>();
    resolutionResult.errors().forEach(e -> logs.add(error(e.getMessage())));
    resolutionResult.warnings().forEach(w -> logs.add(warning(w)));

    return new DepsConflictResolverResult(resolutionResult.value(), logs);
  }

  private FallibleResult<List<GradleDependency>, String, DependencyConflictError>
      resolveDependencies(List<GradleDependency> userDeps, PojaDependencies pojaDependencies) {
    List<String> warnings = new ArrayList<>();
    List<DependencyConflictError> errors = new ArrayList<>();
    List<GradleDependency> resolvedDeps = new ArrayList<>();
    var allDepsMap = pojaDependencies.allDependenciesMap();

    userDeps.forEach(
        userDep ->
            resolveUserDependency(
                userDep, pojaDependencies, allDepsMap, warnings, errors, resolvedDeps));

    return new FallibleResult<>(errors.isEmpty() ? resolvedDeps : List.of(), warnings, errors);
  }

  private void resolveUserDependency(
      GradleDependency userDep,
      PojaDependencies pojaDependencies,
      Map<String, GradleDependency> allDepsMap,
      List<String> warnings,
      List<DependencyConflictError> errors,
      List<GradleDependency> resolvedDeps) {

    var key = userDep.key();
    var pojaDep = allDepsMap.get(key);

    if (pojaDep == null) {
      resolvedDeps.add(userDep);
      return;
    }

    var compatibilityCheck = checkVersionCompatibility(pojaDep, userDep);
    errors.addAll(compatibilityCheck.errors());

    if (compatibilityCheck.isSuccess() && !pojaDep.version().equals(userDep.version())) {
      warnings.add(versionReplacementWarning(pojaDep, userDep));
    }

    if (!pojaDependencies.defaultDependencyKeys().contains(key)) {
      var additionalPojaDep = pojaDependencies.additionalDependenciesMap().get(key);
      resolvedDeps.add(additionalPojaDep != null ? additionalPojaDep : userDep);
    }
  }

  private String versionReplacementWarning(GradleDependency pojaDep, GradleDependency userDep) {
    return String.format(
        "Version replaced for %s - User: %s -> POJA: %s",
        userDep, userDep.version(), pojaDep.version());
  }

  // TODO: Extend ComparableVersion so the version segments can be retrieved directly from an
  // instance
  private static int majorVersionOf(String version) {
    return parseInt(version.split("\\.")[0]);
  }

  private FallibleResult<Void, String, DependencyConflictError> checkVersionCompatibility(
      GradleDependency pojaDep, GradleDependency userDep) {

    if (userDep.version() == null) {
      return new FallibleResult<>(null, List.of(), List.of());
    }

    if (!isValidVersion(userDep.version())) {
      var error =
          DependencyConflictError.invalidFormat(
              userDep.toString(), pojaDep.version(), userDep.version());
      return new FallibleResult<>(null, List.of(), List.of(error));
    }

    var pojaVersion = new ComparableVersion(pojaDep.version());
    var userVersion = new ComparableVersion(userDep.version());

    if (majorVersionOf(pojaDep.version()) != majorVersionOf(userDep.version())) {
      var error =
          DependencyConflictError.majorVersionConflict(
              userDep.toString(), pojaDep.version(), userDep.version());
      return new FallibleResult<>(null, List.of(), List.of(error));
    }

    if (userVersion.compareTo(pojaVersion) > 0) {
      var error =
          DependencyConflictError.futureVersion(
              userDep.toString(), pojaDep.version(), userDep.version());
      return new FallibleResult<>(null, List.of(), List.of(error));
    }

    return new FallibleResult<>(null, List.of(), List.of());
  }

  private static boolean isValidVersion(String version) {
    // Semver pattern: X.Y.Z optionally followed by .W and/or a qualifier
    // Valid: 1.0.0, 1.2.3-SNAPSHOT, 2.1.0.RELEASE, 6.10.0.202406032230-r
    // Invalid: 20231218, 8.5-20231201, 1.0-20131123.140000-1
    return VERSION_PATTERN.matcher(version).matches();
  }
}
