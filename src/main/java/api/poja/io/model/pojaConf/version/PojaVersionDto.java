package api.poja.io.model.pojaConf.version;

import api.poja.io.model.PojaVersion;
import java.net.URI;
import javax.annotation.Nullable;

public record PojaVersionDto(int major, int minor, int patch, @Nullable URI changelogUri) {
  public static PojaVersionDto from(PojaVersion pojaVersion) {
    return new PojaVersionDto(
        pojaVersion.getMajor(), pojaVersion.getMinor(), pojaVersion.getPatch(), null);
  }

  public String toHumanReadableValue() {
    return String.format("%d.%d.%d", major, minor, patch);
  }
}
