package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.rest.model.GeneralPojaConf;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
final class PojaConf1Mapper extends AbstractAppEnvConfigMapper {
  private final PojaConf1Validator validator;

  PojaConf1Mapper(
      @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
      NetworkingService networkingService,
      PojaConf1Validator validator) {
    super(yamlObjectMapper, networkingService);
    this.validator = validator;
  }

  @SneakyThrows
  @Override
  public File writeToTempFile(PojaConf pojaConf) {
    var domainPojaConf = (PojaConf1) pojaConf;
    File namedTempFile =
        createNamedTempFile("conf-v-" + domainPojaConf.version() + "-" + randomUUID() + ".yml");
    this.yamlObjectMapper.writeValue(namedTempFile, domainPojaConf);
    return namedTempFile;
  }

  public OneOfPojaConf readAsRest(File file) {
    api.poja.io.endpoint.rest.model.PojaConf1 pojaConf;
    try {
      var domain = yamlObjectMapper.readValue(file, PojaConf1.class);
      pojaConf = toRest(domain);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return new OneOfPojaConf(pojaConf);
  }

  @Override
  public PojaConf readAsDomain(File file) {
    try {
      return yamlObjectMapper.readValue(file, PojaConf1.class);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @Override
  public File applyMigration(PojaConf from, PojaConf to, boolean isPremium) {
    var pojaConf1 = (PojaConf1) to;
    validator.accept(from == null ? null : (PojaConf1) from, pojaConf1, isPremium);
    return writeToTempFile(pojaConf1);
  }

  private api.poja.io.endpoint.rest.model.PojaConf1 toRest(PojaConf1 domain) {
    var restGeneralConf = domain.general().toRest();
    var updatedGeneralConf =
        new GeneralPojaConf()
            .appName(removeAppNameSuffix(restGeneralConf.getAppName()))
            .packageFullName(restGeneralConf.getPackageFullName())
            .withSnapstart(restGeneralConf.getWithSnapstart())
            .withQueuesNb(restGeneralConf.getWithQueuesNb())
            .customJavaDeps(restGeneralConf.getCustomJavaDeps())
            .customJavaEnvVars(restGeneralConf.getCustomJavaEnvVars())
            .customJavaRepositories(restGeneralConf.getCustomJavaRepositories());

    return new api.poja.io.endpoint.rest.model.PojaConf1()
        .general(updatedGeneralConf)
        .integration(domain.integration().toRest())
        .genApiClient(domain.genApiClient().toRest())
        .concurrency(domain.concurrency().toRest())
        .compute(domain.compute().toRest())
        .emailing(domain.mailing().toRest())
        .testing(domain.testing().toRest())
        .database(domain.database().toRest())
        .version(domain.version());
  }
}
