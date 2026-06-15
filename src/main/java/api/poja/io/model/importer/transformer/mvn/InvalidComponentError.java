package api.poja.io.model.importer.transformer.mvn;

import java.io.File;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class InvalidComponentError extends MvnReadError {
  public InvalidComponentError(File file, String message) {
    super(file, message);
  }

  public InvalidComponentError(File file) {
    super(file);
  }
}
