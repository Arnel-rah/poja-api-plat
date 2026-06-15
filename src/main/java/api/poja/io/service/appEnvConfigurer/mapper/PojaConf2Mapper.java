package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.model.PojaVersion.POJA_1;
import static api.poja.io.model.PojaVersion.POJA_2;
import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import api.poja.io.endpoint.rest.model.GeneralPojaConf2;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
final class PojaConf2Mapper extends AbstractAppEnvConfigMapper {
  private final PojaConf2Validator validator;

  PojaConf2Mapper(
      @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
      NetworkingService networkingService,
      PojaConf2Validator validator) {
    super(yamlObjectMapper, networkingService);
    this.validator = validator;
  }

  @SneakyThrows
  @Override
  public File writeToTempFile(PojaConf pojaConf) {
    var domainPojaConf = (PojaConf2) pojaConf;
    File namedTempFile =
        createNamedTempFile("conf-v-" + domainPojaConf.version() + "-" + randomUUID() + ".yml");
    this.yamlObjectMapper.writeValue(namedTempFile, domainPojaConf);
    return namedTempFile;
  }

  public OneOfPojaConf readAsRest(File file) {
    api.poja.io.endpoint.rest.model.PojaConf2 pojaConf;
    try {
      var domain = yamlObjectMapper.readValue(file, PojaConf2.class);
      pojaConf = toRest(domain);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return new OneOfPojaConf(pojaConf);
  }

  @Override
  public PojaConf readAsDomain(File file) {
    try {
      return yamlObjectMapper.readValue(file, PojaConf2.class);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @Override
  public File applyMigration(PojaConf from, PojaConf to, boolean isPremium) {
    if (from == null) {
      return writeToTempFile(to);
    }
    var pojaConf = ((PojaConf2) to);
    if (POJA_2.equals(from.getVersion())) {
      validator.accept((PojaConf2) from, pojaConf, isPremium);
    } else if (POJA_1.equals(from.getVersion())) {
      validator.accept(((PojaConf1) from), pojaConf);
    }
    return writeToTempFile(pojaConf);
  }

  private api.poja.io.endpoint.rest.model.PojaConf2 toRest(PojaConf2 domain) {
    List<PojaConf2.ScheduledTask> scheduledTasks = domain.scheduledTasks();
    var restGeneralConf = domain.general().toRest();
    var updatedGeneralConf =
        new GeneralPojaConf2()
            .appName(removeAppNameSuffix(restGeneralConf.getAppName()))
            .packageFullName(restGeneralConf.getPackageFullName())
            .withSnapstart(restGeneralConf.getWithSnapstart())
            .customJavaDeps(restGeneralConf.getCustomJavaDeps())
            .customJavaEnvVars(restGeneralConf.getCustomJavaEnvVars())
            .customJavaRepositories(restGeneralConf.getCustomJavaRepositories());

    return new api.poja.io.endpoint.rest.model.PojaConf2()
        .general(updatedGeneralConf)
        .integration(domain.integration().toRest())
        .genApiClient(domain.genApiClient().toRest())
        .concurrency(domain.concurrency().toRest())
        .compute(domain.compute().toRest())
        .emailing(domain.mailing().toRest())
        .testing(domain.testing().toRest())
        .database(domain.database().toRest())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::toRest).toList())
        .version(domain.version());
  }
}
