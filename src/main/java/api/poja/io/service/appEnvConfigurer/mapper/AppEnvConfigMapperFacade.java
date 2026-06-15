package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.endpoint.rest.model.PojaConf1.JSON_PROPERTY_GENERAL;
import static api.poja.io.model.PojaVersion.POJA_1;
import static api.poja.io.model.PojaVersion.POJA_2;
import static api.poja.io.model.PojaVersion.POJA_3;
import static api.poja.io.model.PojaVersion.POJA_4;
import static api.poja.io.model.PojaVersion.POJA_5;
import static api.poja.io.model.PojaVersion.POJA_6;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.PojaVersion.POJA_8;
import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static api.poja.io.model.pojaConf.conf1.PojaConf1.General.JSON_PROPERTY_PUBLIC_GENERATOR_VERSION;

import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.exception.ApiException;
import api.poja.io.model.exception.NotImplementedException;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public final class AppEnvConfigMapperFacade extends AbstractAppEnvConfigMapper {
  private final PojaConfFileMapper conf1Mapper;
  private final PojaConfFileMapper conf2Mapper;
  private final PojaConfFileMapper conf3Mapper;
  private final PojaConfFileMapper conf4Mapper;
  private final PojaConfFileMapper conf5Mapper;
  private final PojaConfFileMapper conf6Mapper;
  private final PojaConfFileMapper conf7Mapper;
  private final PojaConfFileMapper conf8Mapper;
  private final PojaConfFileMapper conf9Mapper;

  AppEnvConfigMapperFacade(
      @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
      NetworkingService networkingService,
      @Qualifier("pojaConf1Mapper") PojaConfFileMapper conf1Mapper,
      @Qualifier("pojaConf2Mapper") PojaConfFileMapper conf2Mapper,
      @Qualifier("pojaConf3Mapper") PojaConfFileMapper conf3Mapper,
      @Qualifier("pojaConf4Mapper") PojaConfFileMapper conf4Mapper,
      @Qualifier("pojaConf5Mapper") PojaConfFileMapper conf5Mapper,
      @Qualifier("pojaConf6Mapper") PojaConfFileMapper conf6Mapper,
      @Qualifier("pojaConf7Mapper") PojaConfFileMapper conf7Mapper,
      @Qualifier("pojaConf8Mapper") PojaConfFileMapper conf8Mapper,
      @Qualifier("pojaConf9Mapper") PojaConfFileMapper conf9Mapper) {
    super(yamlObjectMapper, networkingService);
    this.conf1Mapper = conf1Mapper;
    this.conf2Mapper = conf2Mapper;
    this.conf3Mapper = conf3Mapper;
    this.conf4Mapper = conf4Mapper;
    this.conf5Mapper = conf5Mapper;
    this.conf6Mapper = conf6Mapper;
    this.conf7Mapper = conf7Mapper;
    this.conf8Mapper = conf8Mapper;
    this.conf9Mapper = conf9Mapper;
  }

  private PojaConfFileMapper getMapper(PojaVersion pojaVersion) {
    if (POJA_1.equals(pojaVersion)) {
      return conf1Mapper;
    }
    if (POJA_2.equals(pojaVersion)) {
      return conf2Mapper;
    }
    if (POJA_3.equals(pojaVersion)) {
      return conf3Mapper;
    }
    if (POJA_4.equals(pojaVersion)) {
      return conf4Mapper;
    }
    if (POJA_5.equals(pojaVersion)) {
      return conf5Mapper;
    }
    if (POJA_6.equals(pojaVersion)) {
      return conf6Mapper;
    }
    if (POJA_7.equals(pojaVersion)) {
      return conf7Mapper;
    }
    if (POJA_8.equals(pojaVersion)) {
      return conf8Mapper;
    }
    if (POJA_9.equals(pojaVersion)) {
      return conf9Mapper;
    }
    throw new NotImplementedException("not implemented yet");
  }

  private PojaConfFileMapper getMapper(String humanReadableVersion) {
    if (POJA_1.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf1Mapper;
    }
    if (POJA_2.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf2Mapper;
    }
    if (POJA_3.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf3Mapper;
    }
    if (POJA_4.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf4Mapper;
    }
    if (POJA_5.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf5Mapper;
    }
    if (POJA_6.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf6Mapper;
    }
    if (POJA_7.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf7Mapper;
    }
    if (POJA_8.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf8Mapper;
    }
    if (POJA_9.toHumanReadableValue().equals(humanReadableVersion)) {
      return conf9Mapper;
    }
    throw new NotImplementedException("not implemented yet");
  }

  @Override
  public OneOfPojaConf readAsRest(File file) {
    var pojaVersion = getPojaVersionFrom(file);
    return getMapper(pojaVersion).readAsRest(file);
  }

  @Override
  public PojaConf readAsDomain(File file) {
    var pojaVersion = getPojaVersionFrom(file);
    return getMapper(pojaVersion).readAsDomain(file);
  }

  private PojaVersion getPojaVersionFrom(File file) {
    try {
      JsonNode jsonNode = yamlObjectMapper.readTree(file);
      String cliVersion =
          jsonNode.get(JSON_PROPERTY_GENERAL).get(JSON_PROPERTY_PUBLIC_GENERATOR_VERSION).asText();
      return PojaVersion.fromPublicGeneratorVersion(cliVersion)
          .orElseThrow(
              () ->
                  new ApiException(
                      SERVER_EXCEPTION,
                      "unable to convert cli_version of " + file.getName() + " to poja_version."));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public File applyMigration(PojaConf from, PojaConf to, boolean isPremium) {
    return getMapper(to.getVersion()).applyMigration(from, to, isPremium);
  }

  @Override
  public File writeToTempFile(PojaConf pojaConf) {
    return getMapper(pojaConf.getVersion()).writeToTempFile(pojaConf);
  }
}
