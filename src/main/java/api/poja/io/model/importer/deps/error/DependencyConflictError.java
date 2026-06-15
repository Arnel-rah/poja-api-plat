package api.poja.io.model.importer.deps.error;

import static api.poja.io.model.importer.deps.error.DependencyConflictError.ConflictType.FUTURE_VERSION;
import static api.poja.io.model.importer.deps.error.DependencyConflictError.ConflictType.INVALID_FORMAT;
import static api.poja.io.model.importer.deps.error.DependencyConflictError.ConflictType.MAJOR_VERSION_CONFLICT;

import lombok.Getter;

@Getter
public class DependencyConflictError {
  private final String dependency;
  private final String pojaVersion;
  private final String userVersion;
  private final ConflictType conflictType;
  private final String message;

  private DependencyConflictError(
      String dependency, String pojaVersion, String userVersion, ConflictType conflictType) {
    this.dependency = dependency;
    this.pojaVersion = pojaVersion;
    this.userVersion = userVersion;
    this.conflictType = conflictType;
    this.message = buildMessage(dependency, pojaVersion, userVersion, conflictType);
  }

  public static DependencyConflictError majorVersionConflict(
      String dependency, String pojaVersion, String userVersion) {
    return new DependencyConflictError(
        dependency, pojaVersion, userVersion, MAJOR_VERSION_CONFLICT);
  }

  public static DependencyConflictError futureVersion(
      String dependency, String pojaVersion, String userVersion) {
    return new DependencyConflictError(dependency, pojaVersion, userVersion, FUTURE_VERSION);
  }

  public static DependencyConflictError invalidFormat(
      String dependency, String pojaVersion, String userVersion) {
    return new DependencyConflictError(dependency, pojaVersion, userVersion, INVALID_FORMAT);
  }

  private static String buildMessage(
      String dependency, String pojaVersion, String userVersion, ConflictType conflictType) {
    return switch (conflictType) {
      case MAJOR_VERSION_CONFLICT ->
          "Major version conflict for "
              + dependency
              + " - POJA: "
              + pojaVersion
              + ", User: "
              + userVersion;
      case FUTURE_VERSION ->
          "Version conflict for "
              + dependency
              + " - User version "
              + userVersion
              + " is greater than POJA version "
              + pojaVersion;
      case INVALID_FORMAT ->
          "Incompatible version format for "
              + dependency
              + " - User version "
              + userVersion
              + " is not semver compatible";
    };
  }

  public enum ConflictType {
    MAJOR_VERSION_CONFLICT,
    FUTURE_VERSION,
    INVALID_FORMAT
  }
}
