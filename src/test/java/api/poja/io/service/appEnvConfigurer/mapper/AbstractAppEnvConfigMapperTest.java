package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.endpoint.rest.model.ComputeConf2.FrontalFunctionInvocationMethodEnum.HTTP_API;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf1;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf2;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf3;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf4;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf5;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf6;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf7;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf8;
import static api.poja.io.integration.conf.utils.TestMocks.getValidPojaConf9;
import static api.poja.io.integration.conf.utils.TestUtils.getResource;
import static api.poja.io.model.pojaConf.conf1.PojaConf1Concurrency.BASIC_USER_CONCURRENCY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import api.poja.io.conf.MockedThirdParties;
import api.poja.io.endpoint.rest.model.ComputeConf9;
import api.poja.io.endpoint.rest.model.OneOfPojaConf;
import api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency;
import api.poja.io.model.pojaConf.conf9.PojaConf9Concurrency;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AbstractAppEnvConfigMapperTest extends MockedThirdParties {

  public static final String POJA_CONF_1_YML_RESOURCE_PATH = "files/poja_1.yml";
  public static final String POJA_CONF_2_YML_RESOURCE_PATH = "files/poja_2.yml";
  public static final String POJA_CONF_3_YML_RESOURCE_PATH = "files/poja_3.yml";
  public static final String POJA_CONF_4_YML_RESOURCE_PATH = "files/poja_4.yml";
  public static final String POJA_CONF_5_YML_RESOURCE_PATH = "files/poja_5.yml";
  public static final String POJA_CONF_6_YML_RESOURCE_PATH = "files/poja_6.yml";
  public static final String POJA_CONF_7_YML_RESOURCE_PATH = "files/poja_7.yml";
  public static final String POJA_CONF_8_YML_RESOURCE_PATH = "files/poja_8.yml";
  public static final String POJA_CONF_9_YML_RESOURCE_PATH = "files/poja_9.yml";
  public static final String NO_APP_NAME_SUFFIX_POJA_CONF_1_YML_RESOURCE_PATH =
      "files/poja_1_not_suffixed.yml";
  public static final String NO_APP_NAME_SUFFIX_POJA_CONF_2_YML_RESOURCE_PATH =
      "files/poja_2_not_suffixed.yml";
  public static final String NO_APP_NAME_SUFFIX_POJA_CONF_3_YML_RESOURCE_PATH =
      "files/poja_3_not_suffixed.yml";
  public static final String USER_ID = "9f7332d1-778b-425b-828f-b165660259f5";
  public static final String APP_ID = "27e4fce1-34dd-4864-ade7-b48dffeca40f";

  @Autowired AbstractAppEnvConfigMapper subject;
  @Autowired PojaConfMapper pojaConfMapper;

  @Test
  void createNamedTempFile() {
    String randomNameWithYamlExtension = randomUUID() + ".yml";

    var actualFile = subject.createNamedTempFile(randomNameWithYamlExtension);

    assertEquals(randomNameWithYamlExtension, actualFile.getName());
  }

  @Test
  void writeToTempFilePojaConf1() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf1(getValidPojaConf1(), BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_1_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_1_ok() throws IOException {
    var file = getResource(POJA_CONF_1_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf1());

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void read_1_without_suffixed_app_name_ok() throws IOException {
    var file = getResource(NO_APP_NAME_SUFFIX_POJA_CONF_1_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf1());

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void read_2_ok() throws IOException {
    var file = getResource(POJA_CONF_2_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf2());

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void read_2_without_suffixed_app_name_ok() throws IOException {
    var file = getResource(NO_APP_NAME_SUFFIX_POJA_CONF_2_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf2());

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf2() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf2(
                getValidPojaConf2(), PojaConf2Concurrency.BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_2_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_3_ok() throws IOException {
    var file = getResource(POJA_CONF_3_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf3(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf3() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf3(
                getValidPojaConf3(HTTP_API), PojaConf2Concurrency.BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_3_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_4_ok() throws IOException {
    var file = getResource(POJA_CONF_4_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf4(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf4() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf4(
                getValidPojaConf4(HTTP_API), PojaConf2Concurrency.BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_4_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_5_ok() throws IOException {
    var file = getResource(POJA_CONF_5_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf5(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf5() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf5(
                getValidPojaConf5(HTTP_API), PojaConf2Concurrency.BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_5_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_6_ok() throws IOException {
    var file = getResource(POJA_CONF_6_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf6(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf6() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf6(
                getValidPojaConf6(HTTP_API), PojaConf2Concurrency.BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_6_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_7_ok() throws IOException {
    var file = getResource(POJA_CONF_7_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf7(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf7() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf7(
                getValidPojaConf7(HTTP_API), PojaConf2Concurrency.BASIC_USER_CONCURRENCY, USER_ID));

    assertEquals(
        getResource(POJA_CONF_7_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_8_ok() throws IOException {
    var file = getResource(POJA_CONF_8_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf8(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf8() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf8(
                getValidPojaConf8(HTTP_API),
                PojaConf2Concurrency.BASIC_USER_CONCURRENCY,
                USER_ID,
                APP_ID));

    assertEquals(
        getResource(POJA_CONF_8_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_9_ok() throws IOException {
    var file = getResource(POJA_CONF_9_YML_RESOURCE_PATH).getFile();
    var expected =
        new OneOfPojaConf(
            getValidPojaConf9(ComputeConf9.FrontalFunctionInvocationMethodEnum.HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @Test
  void writeToTempFilePojaConf9() throws IOException {
    var pojaFile =
        subject.writeToTempFile(
            pojaConfMapper.toDomainPojaConf9(
                getValidPojaConf9(ComputeConf9.FrontalFunctionInvocationMethodEnum.HTTP_API),
                PojaConf9Concurrency.BASIC_USER_CONCURRENCY,
                USER_ID,
                APP_ID));

    assertEquals(
        getResource(POJA_CONF_9_YML_RESOURCE_PATH).getContentAsString(UTF_8),
        readFileContent(pojaFile));
  }

  @Test
  void read_3_without_suffixed_app_name_ok() throws IOException {
    var file = getResource(NO_APP_NAME_SUFFIX_POJA_CONF_3_YML_RESOURCE_PATH).getFile();
    var expected = new OneOfPojaConf(getValidPojaConf3(HTTP_API));

    var actual = subject.readAsRest(file);

    assertEquals(expected, actual);
  }

  @SneakyThrows
  private static String readFileContent(File file) {
    return readBytesToString(readAllBytes(Path.of(file.getAbsolutePath())));
  }

  private static String readBytesToString(byte[] bytes) {
    return new String(bytes);
  }
}
