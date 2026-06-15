package api.poja.io.model.importer.transformer.mvn;

import java.io.File;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@Accessors(fluent = true)
public sealed class MvnReadError permits InvalidComponentError, InvalidPomXml {
  @ToString.Exclude @EqualsAndHashCode.Exclude protected final File file;
  protected final String message;

  public MvnReadError(File file) {
    this(file, "An error occurred while reading mvn pom.xml");
  }
}
