package api.poja.io.service.appEnvConfigurer.mapper;

import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.model.pojaConf.PojaConf;
import java.io.File;

public interface PojaConfFileMapper {
  OneOfPojaConf readAsRest(File file);

  PojaConf readAsDomain(File file);

  File applyMigration(PojaConf from, PojaConf to, boolean isPremium);

  File writeToTempFile(PojaConf pojaConf);
}
