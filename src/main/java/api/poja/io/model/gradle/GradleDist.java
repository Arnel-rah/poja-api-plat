package api.poja.io.model.gradle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@AllArgsConstructor
public enum GradleDist {
  VERSION_8_5("8.5");

  private final String version;

  public String filename() {
    return "gradle-" + version;
  }

  @Override
  public String toString() {
    return filename();
  }
}
