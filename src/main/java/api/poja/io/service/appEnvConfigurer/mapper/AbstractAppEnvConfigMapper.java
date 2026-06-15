package api.poja.io.service.appEnvConfigurer.mapper;

import api.poja.io.service.appEnvConfigurer.NetworkingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;

abstract sealed class AbstractAppEnvConfigMapper implements PojaConfFileMapper
    permits AppEnvConfigMapperFacade,
        PojaConf1Mapper,
        PojaConf2Mapper,
        PojaConf3Mapper,
        PojaConf4Mapper,
        PojaConf5Mapper,
        PojaConf6Mapper,
        PojaConf7Mapper,
        PojaConf8Mapper,
        PojaConf9Mapper {
  protected final ObjectMapper yamlObjectMapper;
  protected final NetworkingService networkingService;
  protected static final Pattern suffixedAppNamePattern = Pattern.compile(".*-[0-9a-fA-F]{8}$");
  protected static final int APP_NAME_SUFFIX_LENGTH = 9;

  AbstractAppEnvConfigMapper(
      @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
      NetworkingService networkingService) {
    this.yamlObjectMapper = yamlObjectMapper;
    this.networkingService = networkingService;
  }

  protected final String removeAppNameSuffix(String appName) {
    if (appName.length() > APP_NAME_SUFFIX_LENGTH
        && suffixedAppNamePattern.matcher(appName).matches()) {
      return appName.substring(0, appName.length() - APP_NAME_SUFFIX_LENGTH);
    }
    return appName;
  }

  protected final File createNamedTempFile(String filename) {
    return new File(createTempDir(), filename);
  }

  @SneakyThrows
  private static File createTempDir() {
    return Files.createTempDirectory("poja-conf").toFile();
  }
}
