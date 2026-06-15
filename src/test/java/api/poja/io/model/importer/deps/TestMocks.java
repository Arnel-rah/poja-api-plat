package api.poja.io.model.importer.deps;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import java.util.List;

public class TestMocks {
  // ========== SUCCESS CASES ==========
  // Case 1: New dependencies without conflicts
  public static final List<GradleDependency> USER_DEPS_NEW_DEPENDENCIES =
      List.of(
          new GradleDependency("implementation", "org.bouncycastle", "bcprov-jdk18on", "1.78.1"),
          new GradleDependency("implementation", "org.bouncycastle", "bcpkix-jdk18on", "1.78.1"));

  public static final List<GradleDependency> EXPECTED_DEPS_NEW_DEPENDENCIES =
      USER_DEPS_NEW_DEPENDENCIES;

  // Case 2: Same version as base POJA deps (should be filtered out)
  public static final List<GradleDependency> USER_DEPS_SAME_VERSION =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.40"));

  public static final List<GradleDependency> EXPECTED_DEPS_SAME_VERSION = List.of();

  // Case 3: Null versions
  public static final List<GradleDependency> USER_DEPS_NULL_VERSIONS =
      List.of(
          new GradleDependency(
              "implementation", "org.springframework.boot", "spring-boot-starter-web", null),
          new GradleDependency("implementation", "custom.lib", "custom-artifact", null));

  public static final List<GradleDependency> EXPECTED_DEPS_NULL_VERSIONS =
      List.of(new GradleDependency("implementation", "custom.lib", "custom-artifact", null));

  // Case 4: Different configuration (not filtered)
  public static final List<GradleDependency> USER_DEPS_DIFFERENT_CONFIGURATION =
      List.of(
          new GradleDependency("testImplementation", "software.amazon.awssdk", "sqs", "2.21.40"));

  public static final List<GradleDependency> EXPECTED_DEPS_DIFFERENT_CONFIGURATION =
      USER_DEPS_DIFFERENT_CONFIGURATION;

  // Case 5: Compatible but lower versions (base deps filtered)
  public static final List<GradleDependency> USER_DEPS_COMPATIBLE_LOWER_VERSIONS =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.20.30"),
          new GradleDependency("implementation", "software.amazon.awssdk", "ses", "2.21.10"));

  public static final List<GradleDependency> EXPECTED_DEPS_COMPATIBLE_LOWER_VERSIONS = List.of();

  // Case 6: Higher patch but lower minor (filtered)
  public static final List<GradleDependency> USER_DEPS_PATCH_HIGHER_MINOR_LOWER =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.20.50"));

  public static final List<GradleDependency> EXPECTED_DEPS_PATCH_HIGHER_MINOR_LOWER = List.of();

  // Case 7: Qualifier with lower version (filtered)
  public static final List<GradleDependency> USER_DEPS_QUALIFIER_LOWER =
      List.of(
          new GradleDependency(
              "implementation", "software.amazon.awssdk", "sqs", "2.20.0-SNAPSHOT"));

  public static final List<GradleDependency> EXPECTED_DEPS_QUALIFIER_LOWER = List.of();

  // Case 8: Release candidate with lower version (filtered)
  public static final List<GradleDependency> USER_DEPS_RC_LOWER =
      List.of(
          new GradleDependency(
              "implementation",
              "org.springframework.boot",
              "spring-boot-starter-web",
              "3.1.0-RC2"));

  public static final List<GradleDependency> EXPECTED_DEPS_RC_LOWER = List.of();

  // Case 9: Alpha with lower version (filtered)
  public static final List<GradleDependency> USER_DEPS_ALPHA_LOWER =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.0-alpha"));

  public static final List<GradleDependency> EXPECTED_DEPS_ALPHA_LOWER = List.of();

  // Case 10: Eclipse timestamp with lower version (filtered)
  public static final List<GradleDependency> USER_DEPS_ECLIPSE_TIMESTAMP_LOWER =
      List.of(
          new GradleDependency(
              "implementation", "com.amazonaws", "aws-lambda-java-core", "1.2.0.202406011200-r"));

  public static final List<GradleDependency> EXPECTED_DEPS_ECLIPSE_TIMESTAMP_LOWER = List.of();

  // Case 11: Four-part version lower (filtered)
  public static final List<GradleDependency> USER_DEPS_FOUR_PART_LOWER =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.39.5"));

  public static final List<GradleDependency> EXPECTED_DEPS_FOUR_PART_LOWER = List.of();

  // Case 12: Optional POJA dep with same version
  public static final List<GradleDependency> USER_DEPS_OPTIONAL_POJA =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "s3", "2.20.30"));

  public static final List<GradleDependency> EXPECTED_DEPS_OPTIONAL_POJA =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "s3", "2.21.40"));

  // Case 13: Mixed - new deps and POJA deps
  public static final List<GradleDependency> USER_DEPS_MIXED =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.40"),
          new GradleDependency("implementation", "org.bouncycastle", "bcprov-jdk18on", "1.78.1"));

  public static final List<GradleDependency> EXPECTED_DEPS_MIXED =
      List.of(
          new GradleDependency("implementation", "org.bouncycastle", "bcprov-jdk18on", "1.78.1"));

  // Case 14: Empty
  public static final List<GradleDependency> USER_DEPS_EMPTY = List.of();

  public static final List<GradleDependency> EXPECTED_DEPS_EMPTY = List.of();

  // ========== VERSION CONFLICT FAILURES ==========
  // Case 15: Major version conflict
  public static final List<GradleDependency> USER_DEPS_MAJOR_CONFLICT =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "3.21.40"));

  public static final List<GradleDependency> EXPECTED_DEPS_MAJOR_CONFLICT = List.of();

  // Case 16: Minor version conflict
  public static final List<GradleDependency> USER_DEPS_MINOR_CONFLICT =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.22.40"));

  public static final List<GradleDependency> EXPECTED_DEPS_MINOR_CONFLICT = List.of();

  // Case 17: Patch version conflict
  public static final List<GradleDependency> USER_DEPS_PATCH_CONFLICT =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.50"));

  public static final List<GradleDependency> EXPECTED_DEPS_PATCH_CONFLICT = List.of();

  // Case 18: Qualifier with higher version
  public static final List<GradleDependency> USER_DEPS_QUALIFIER_HIGHER =
      List.of(
          new GradleDependency(
              "implementation", "software.amazon.awssdk", "sqs", "2.22.0-SNAPSHOT"));

  public static final List<GradleDependency> EXPECTED_DEPS_QUALIFIER_HIGHER = List.of();

  // Case 19: RC with higher version
  public static final List<GradleDependency> USER_DEPS_RC_HIGHER =
      List.of(
          new GradleDependency(
              "implementation",
              "org.springframework.boot",
              "spring-boot-starter-web",
              "3.3.0-RC1"));

  public static final List<GradleDependency> EXPECTED_DEPS_RC_HIGHER = List.of();

  // Case 20: Milestone with higher version
  public static final List<GradleDependency> USER_DEPS_MILESTONE_HIGHER =
      List.of(
          new GradleDependency(
              "implementation", "org.springframework.boot", "spring-boot-starter-web", "3.3.0-M1"));

  public static final List<GradleDependency> EXPECTED_DEPS_MILESTONE_HIGHER = List.of();

  // Case 21: Eclipse timestamp higher
  public static final List<GradleDependency> USER_DEPS_ECLIPSE_TIMESTAMP_HIGHER =
      List.of(
          new GradleDependency(
              "implementation", "com.amazonaws", "aws-lambda-java-core", "1.3.0.202506011200-r"));

  public static final List<GradleDependency> EXPECTED_DEPS_ECLIPSE_TIMESTAMP_HIGHER = List.of();

  // Case 22: RELEASE qualifier higher
  public static final List<GradleDependency> USER_DEPS_RELEASE_QUALIFIER_HIGHER =
      List.of(
          new GradleDependency(
              "implementation",
              "org.springframework.boot",
              "spring-boot-starter-web",
              "3.3.0.RELEASE"));

  public static final List<GradleDependency> EXPECTED_DEPS_RELEASE_QUALIFIER_HIGHER = List.of();

  // Case 23: Four-part version higher
  public static final List<GradleDependency> USER_DEPS_FOUR_PART_HIGHER =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.40.2"));

  public static final List<GradleDependency> EXPECTED_DEPS_FOUR_PART_HIGHER = List.of();

  // Case 24: Date-based version
  public static final List<GradleDependency> USER_DEPS_DATE_VERSION =
      List.of(
          new GradleDependency(
              "implementation", "com.amazonaws", "aws-lambda-java-core", "2023.12.18"));

  public static final List<GradleDependency> EXPECTED_DEPS_DATE_VERSION = List.of();

  // Case 25: Multiple conflicts
  public static final List<GradleDependency> USER_DEPS_MULTIPLE_CONFLICTS =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "3.0.0"),
          new GradleDependency("implementation", "software.amazon.awssdk", "ses", "3.0.0"));

  public static final List<GradleDependency> EXPECTED_DEPS_MULTIPLE_CONFLICTS = List.of();

  // ========== INVALID FORMAT FAILURES ==========
  // Case 26: Timestamp version
  public static final List<GradleDependency> USER_DEPS_TIMESTAMP_VERSION =
      List.of(new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "20231218"));

  public static final List<GradleDependency> EXPECTED_DEPS_TIMESTAMP_VERSION = List.of();

  // Case 27: Incomplete semver
  public static final List<GradleDependency> USER_DEPS_INCOMPLETE_SEMVER =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "8.5-20231201"));

  public static final List<GradleDependency> EXPECTED_DEPS_INCOMPLETE_SEMVER = List.of();

  // Case 28: Snapshot timestamp
  public static final List<GradleDependency> USER_DEPS_SNAPSHOT_TIMESTAMP =
      List.of(
          new GradleDependency(
              "implementation", "software.amazon.awssdk", "sqs", "1.0-20131123.140000-1"));

  public static final List<GradleDependency> EXPECTED_DEPS_SNAPSHOT_TIMESTAMP = List.of();

  // Case 29: Mixed formats
  public static final List<GradleDependency> USER_DEPS_MIXED_FORMATS =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.20.0"),
          new GradleDependency("implementation", "software.amazon.awssdk", "ses", "20231218"));

  public static final List<GradleDependency> EXPECTED_DEPS_MIXED_FORMATS = List.of();
}
