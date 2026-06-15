package api.poja.io.model.importer.analyzer.buildtool.gradle.model;

import static api.poja.io.model.importer.util.StringFormatUtils.formatAssign;
import static api.poja.io.model.importer.util.StringFormatUtils.formatSingleQuoted;

import javax.annotation.Nonnull;

public record GradleSettings(@Nonnull String rootProjectName) {
  /** Returns the {@code settings.gradle} of this GradleSettings */
  public String formatDeclaration() {
    return formatAssign("rootProject.name", formatSingleQuoted(rootProjectName));
  }

  /**
   * Returns the {@code settings.gradle} of this GradleSettings. This is just the string return by
   * the {@link #formatDeclaration()} method
   */
  @Override
  public String toString() {
    return formatDeclaration();
  }
}
