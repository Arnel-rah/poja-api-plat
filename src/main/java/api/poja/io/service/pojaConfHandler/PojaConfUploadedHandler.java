package api.poja.io.service.pojaConfHandler;

import api.poja.io.endpoint.event.model.PojaConfUploaded;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.PojaConf;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface PojaConfUploadedHandler extends Consumer<PojaConfUploaded> {
  void accept(PojaConfUploaded pojaConfUploaded);

  boolean supports(PojaVersion pojaVersion);

  void configureCdCompute(Path clonedDirPath, PojaConf pojaConf);
}
