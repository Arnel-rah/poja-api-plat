package api.poja.io.model;

import static java.util.Optional.empty;

import java.util.Optional;
import lombok.Getter;

@Getter
public enum PojaVersion implements Comparable<PojaVersion> {
  POJA_1(1, 2, 0),
  POJA_2(1, 3, 0),
  POJA_3(1, 3, 1),
  POJA_4(1, 3, 2),
  POJA_5(1, 3, 3),
  POJA_6(2, 0, 1),
  POJA_7(2, 1, 0),
  POJA_8(3, 0, 0),
  POJA_9(3, 1, 0);

  public static final PojaVersion LATEST = POJA_9;

  private final int major;
  private final int minor;
  private final int patch;

  public final String getPublicGeneratorVersion() {
    return toHumanReadableValue();
  }

  PojaVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  public final String toHumanReadableValue() {
    return String.format("%d.%d.%d", major, minor, patch);
  }

  public static Optional<PojaVersion> fromHumanReadableValue(String humanReadableValue) {
    for (PojaVersion value : values()) {
      if (value.toHumanReadableValue().equals(humanReadableValue)) {
        return Optional.of(value);
      }
    }
    return empty();
  }

  public static Optional<PojaVersion> fromPublicGeneratorVersion(String publicGeneratorVersion) {
    for (PojaVersion value : values()) {
      if (value.getPublicGeneratorVersion().equals(publicGeneratorVersion)) {
        return Optional.of(value);
      }
    }
    return empty();
  }
}
