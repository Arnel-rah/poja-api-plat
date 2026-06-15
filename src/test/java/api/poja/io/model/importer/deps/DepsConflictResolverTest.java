package api.poja.io.model.importer.deps;

import static api.poja.io.model.PojaVersion.POJA_6;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_ALPHA_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_COMPATIBLE_LOWER_VERSIONS;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_DATE_VERSION;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_DIFFERENT_CONFIGURATION;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_ECLIPSE_TIMESTAMP_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_ECLIPSE_TIMESTAMP_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_EMPTY;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_FOUR_PART_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_FOUR_PART_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_INCOMPLETE_SEMVER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_MAJOR_CONFLICT;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_MILESTONE_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_MINOR_CONFLICT;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_MIXED;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_MIXED_FORMATS;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_MULTIPLE_CONFLICTS;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_NEW_DEPENDENCIES;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_NULL_VERSIONS;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_OPTIONAL_POJA;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_PATCH_CONFLICT;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_PATCH_HIGHER_MINOR_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_QUALIFIER_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_QUALIFIER_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_RC_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_RC_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_RELEASE_QUALIFIER_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_SAME_VERSION;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_SNAPSHOT_TIMESTAMP;
import static api.poja.io.model.importer.deps.TestMocks.EXPECTED_DEPS_TIMESTAMP_VERSION;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_ALPHA_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_COMPATIBLE_LOWER_VERSIONS;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_DATE_VERSION;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_DIFFERENT_CONFIGURATION;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_ECLIPSE_TIMESTAMP_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_ECLIPSE_TIMESTAMP_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_EMPTY;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_FOUR_PART_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_FOUR_PART_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_INCOMPLETE_SEMVER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_MAJOR_CONFLICT;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_MILESTONE_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_MINOR_CONFLICT;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_MIXED;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_MIXED_FORMATS;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_MULTIPLE_CONFLICTS;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_NEW_DEPENDENCIES;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_NULL_VERSIONS;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_OPTIONAL_POJA;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_PATCH_CONFLICT;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_PATCH_HIGHER_MINOR_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_QUALIFIER_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_QUALIFIER_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_RC_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_RC_LOWER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_RELEASE_QUALIFIER_HIGHER;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_SAME_VERSION;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_SNAPSHOT_TIMESTAMP;
import static api.poja.io.model.importer.deps.TestMocks.USER_DEPS_TIMESTAMP_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import api.poja.io.model.PojaVersion;
import org.junit.jupiter.api.Test;

class DepsConflictResolverTest {
  final DepsConflictResolver subject = new DepsConflictResolver();

  private static final PojaVersion TEST_VERSION = POJA_6;

  @Test
  void should_succeed_when_adding_new_dependencies_without_conflicts() {
    var result = subject.apply(USER_DEPS_NEW_DEPENDENCIES, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().isEmpty());
    assertEquals(EXPECTED_DEPS_NEW_DEPENDENCIES, result.data().dependencies());
  }

  @Test
  void should_succeed_when_same_version() {
    var result = subject.apply(USER_DEPS_SAME_VERSION, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().isEmpty());
    assertEquals(EXPECTED_DEPS_SAME_VERSION, result.data().dependencies());
  }

  @Test
  void should_succeed_and_keep_only_new_deps_when_user_has_null_versions() {
    var result = subject.apply(USER_DEPS_NULL_VERSIONS, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_NULL_VERSIONS, result.data().dependencies());
  }

  @Test
  void should_succeed_and_keep_different_configuration() {
    var result = subject.apply(USER_DEPS_DIFFERENT_CONFIGURATION, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().isEmpty());
    assertEquals(EXPECTED_DEPS_DIFFERENT_CONFIGURATION, result.data().dependencies());
  }

  @Test
  void should_succeed_when_compatible_lower_versions() {
    var result = subject.apply(USER_DEPS_COMPATIBLE_LOWER_VERSIONS, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertTrue(result.logs().getLast().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_COMPATIBLE_LOWER_VERSIONS, result.data().dependencies());
  }

  @Test
  void should_succeed_when_patch_higher_but_minor_lower() {
    var result = subject.apply(USER_DEPS_PATCH_HIGHER_MINOR_LOWER, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_PATCH_HIGHER_MINOR_LOWER, result.data().dependencies());
  }

  @Test
  void should_succeed_when_qualifier_with_lower_version() {
    var result = subject.apply(USER_DEPS_QUALIFIER_LOWER, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_QUALIFIER_LOWER, result.data().dependencies());
  }

  @Test
  void should_succeed_when_release_candidate_with_lower_version() {
    var result = subject.apply(USER_DEPS_RC_LOWER, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_RC_LOWER, result.data().dependencies());
  }

  @Test
  void should_succeed_when_alpha_with_lower_version() {
    var result = subject.apply(USER_DEPS_ALPHA_LOWER, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_ALPHA_LOWER, result.data().dependencies());
  }

  @Test
  void should_succeed_when_eclipse_timestamp_with_lower_version() {
    var result = subject.apply(USER_DEPS_ECLIPSE_TIMESTAMP_LOWER, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_ECLIPSE_TIMESTAMP_LOWER, result.data().dependencies());
  }

  @Test
  void should_succeed_when_four_part_version_is_lower() {
    var result = subject.apply(USER_DEPS_FOUR_PART_LOWER, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_FOUR_PART_LOWER, result.data().dependencies());
  }

  @Test
  void should_succeed_when_optional_poja_dep() {
    var result = subject.apply(USER_DEPS_OPTIONAL_POJA, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().getFirst().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_OPTIONAL_POJA, result.data().dependencies());
  }

  @Test
  void should_succeed_and_keep_only_new_deps_when_mixed() {
    var result = subject.apply(USER_DEPS_MIXED, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().isEmpty());
    assertEquals(EXPECTED_DEPS_MIXED, result.data().dependencies());
  }

  @Test
  void should_succeed_when_empty() {
    var result = subject.apply(USER_DEPS_EMPTY, TEST_VERSION);

    assertTrue(result.successful());
    assertTrue(result.logs().isEmpty());
    assertEquals(EXPECTED_DEPS_EMPTY, result.data().dependencies());
  }

  @Test
  void should_fail_when_major_version_different() {
    var result = subject.apply(USER_DEPS_MAJOR_CONFLICT, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("Major version conflict"));
    assertEquals(EXPECTED_DEPS_MAJOR_CONFLICT, result.data().dependencies());
  }

  @Test
  void should_fail_when_minor_version_is_greater() {
    var result = subject.apply(USER_DEPS_MINOR_CONFLICT, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_MINOR_CONFLICT, result.data().dependencies());
  }

  @Test
  void should_fail_when_patch_version_is_greater() {
    var result = subject.apply(USER_DEPS_PATCH_CONFLICT, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_PATCH_CONFLICT, result.data().dependencies());
  }

  @Test
  void should_fail_when_qualifier_with_higher_version() {
    var result = subject.apply(USER_DEPS_QUALIFIER_HIGHER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_QUALIFIER_HIGHER, result.data().dependencies());
  }

  @Test
  void should_fail_when_release_candidate_with_higher_version() {
    var result = subject.apply(USER_DEPS_RC_HIGHER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_RC_HIGHER, result.data().dependencies());
  }

  @Test
  void should_fail_when_milestone_with_higher_version() {
    var result = subject.apply(USER_DEPS_MILESTONE_HIGHER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_MILESTONE_HIGHER, result.data().dependencies());
  }

  @Test
  void should_fail_when_eclipse_timestamp_with_higher_version() {
    var result = subject.apply(USER_DEPS_ECLIPSE_TIMESTAMP_HIGHER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_ECLIPSE_TIMESTAMP_HIGHER, result.data().dependencies());
  }

  @Test
  void should_fail_when_release_qualifier_with_higher_version() {
    var result = subject.apply(USER_DEPS_RELEASE_QUALIFIER_HIGHER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_RELEASE_QUALIFIER_HIGHER, result.data().dependencies());
  }

  @Test
  void should_fail_when_four_part_version_is_higher() {
    var result = subject.apply(USER_DEPS_FOUR_PART_HIGHER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("is greater than"));
    assertEquals(EXPECTED_DEPS_FOUR_PART_HIGHER, result.data().dependencies());
  }

  @Test
  void should_fail_when_date_based_version_causes_major_conflict() {
    var result = subject.apply(USER_DEPS_DATE_VERSION, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("Major version conflict"));
    assertEquals(EXPECTED_DEPS_DATE_VERSION, result.data().dependencies());
  }

  @Test
  void should_fail_when_multiple_conflicts() {
    var result = subject.apply(USER_DEPS_MULTIPLE_CONFLICTS, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("Major version conflict"));
    assertEquals(EXPECTED_DEPS_MULTIPLE_CONFLICTS, result.data().dependencies());
  }

  @Test
  void should_fail_when_timestamp_format() {
    var result = subject.apply(USER_DEPS_TIMESTAMP_VERSION, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("not semver compatible"));
    assertEquals(EXPECTED_DEPS_TIMESTAMP_VERSION, result.data().dependencies());
  }

  @Test
  void should_fail_when_incomplete_semver_format() {
    var result = subject.apply(USER_DEPS_INCOMPLETE_SEMVER, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("not semver compatible"));
    assertEquals(EXPECTED_DEPS_INCOMPLETE_SEMVER, result.data().dependencies());
  }

  @Test
  void should_fail_when_snapshot_timestamp() {
    var result = subject.apply(USER_DEPS_SNAPSHOT_TIMESTAMP, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("not semver compatible"));
    assertEquals(EXPECTED_DEPS_SNAPSHOT_TIMESTAMP, result.data().dependencies());
  }

  @Test
  void should_fail_when_mixed_formats_with_invalid_version() {
    var result = subject.apply(USER_DEPS_MIXED_FORMATS, TEST_VERSION);

    assertTrue(result.failed());
    assertTrue(result.logs().getFirst().getMessage().contains("not semver compatible"));
    assertTrue(result.logs().getLast().getMessage().contains("Version replaced"));
    assertEquals(EXPECTED_DEPS_MIXED_FORMATS, result.data().dependencies());
  }
}
