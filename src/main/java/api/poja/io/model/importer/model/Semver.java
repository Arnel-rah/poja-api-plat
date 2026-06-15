package api.poja.io.model.importer.model;

import static java.lang.Integer.parseInt;

public record Semver(int major, int minor, int patch) {

  public static Semver from(String version) throws IllegalArgumentException {
    var parts = version.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("version should be 3 dot-separated part");
    }
    return new Semver(parseInt(parts[0]), parseInt(parts[1]), parseInt(parts[2]));
  }

  public boolean isAfter(Semver other) {
    return major > other.major || minor > other.minor || patch > other.patch;
  }

  public boolean hasDifferentPastMajorVersionComparedTo(Semver other) {
    return major < other.major;
  }

  public boolean isBackwardCompatibleWith(Semver other) {
    return major == other.major() && minor >= other.minor && patch >= other.patch;
  }

  /** Returns the string representation of this version whose format is "major.minor.patch" */
  @Override
  public String toString() {
    return String.format("%d.%d.%s", major, minor, patch);
  }
}
