package api.poja.io.model.importer.transformer.mvn;

import java.io.File;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Getter
@Accessors(fluent = true)
public abstract class MvnReadWarning {
  private final File pomXml;
}
