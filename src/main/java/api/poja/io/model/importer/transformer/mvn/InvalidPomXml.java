package api.poja.io.model.importer.transformer.mvn;

import java.io.File;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class InvalidPomXml extends MvnReadError {
  public InvalidPomXml(File file, String message) {
    super(file, message);
  }

  public InvalidPomXml(File file) {
    super(file);
  }
}
