package api.poja.io.model.importer.analyzer.buildtool.gradle.model;

import static api.poja.io.model.importer.util.StringFormatUtils.formatAssign;
import static api.poja.io.model.importer.util.StringFormatUtils.formatBlock;
import static api.poja.io.model.importer.util.StringFormatUtils.formatSingleQuoted;

import java.util.StringJoiner;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public record JavaConfig(
    String group, String sourceCompatibility, @Nullable String targetCompatibility) {

  private static final Pattern GROUP_ID_PATTERN =
      Pattern.compile("^([a-z][a-z0-9]*)(\\.([a-z][a-z0-9]*))+$");

  public JavaConfig {
    validate(group, sourceCompatibility);
  }

  private static void validate(String group, String sourceCompatibility) {
    var sj = new StringJoiner(". ");

    if (group == null) {
      sj.add("group cannot be null");
    } else if (!GROUP_ID_PATTERN.matcher(group).matches()) {
      sj.add("groupId must include at least 2 lowercase alphanumeric segments");
    }
    if (sourceCompatibility == null) {
      sj.add("sourceCompatibility cannot be null");
    }

    if (sj.length() > 1) {
      throw new IllegalArgumentException(sj.toString());
    }
  }

  /**
   * Returns the string representation of this java plugin config. This is just the string returned
   * by the {@link #formatDeclaration()} method.
   */
  @Override
  public String toString() {
    return formatDeclaration();
  }

  /**
   * Returns the string representation of this java plugin config whose format is
   *
   * <pre>
   *   java {
   *     group = GROUP
   *     sourceCompatibility = SOURCE
   *     targetCompatibility = TARGET
   *   }
   * </pre>
   */
  public String formatDeclaration() {
    var b = formatBlock("java");
    b.add(formatAssign("group", formatSingleQuoted(group)));
    b.add(formatAssign("sourceCompatibility", formatSingleQuoted(sourceCompatibility)));
    b.add(formatAssign("targetCompatibility", formatSingleQuoted(jvmTargetVersion())));
    return b.toString();
  }

  @Override
  public String targetCompatibility() {
    return jvmTargetVersion();
  }

  /**
   * Returns the JVM target version, falling back to `sourceCompatibility` if `targetCompatibility`
   * is null.
   */
  public String jvmTargetVersion() {
    return targetCompatibility != null ? targetCompatibility : sourceCompatibility;
  }
}
