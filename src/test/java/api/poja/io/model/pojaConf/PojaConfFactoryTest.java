package api.poja.io.model.pojaConf;

import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.PojaVersion.POJA_8;
import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.model.importer.TestMocks.EMAIL_CONF;
import static api.poja.io.model.importer.TestMocks.SES_SOURCE;
import static api.poja.io.model.importer.TestMocks.networkingConf;
import static api.poja.io.model.pojaConf.conf2.PojaConf2.Compute.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
import static api.poja.io.model.pojaConf.conf2.PojaConf2.Database.WithDatabaseEnum.NONE;
import static api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency.BASIC_USER_CONCURRENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import api.poja.io.endpoint.rest.model.WithQueuesNbEnum;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.model.pojaConf.conf9.PojaConf9Concurrency;
import api.poja.io.model.pojaConf.factory.PojaConfFactory;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PojaConfFactoryTest {

  final NetworkingService networkingServiceMock = mock();

  @BeforeEach
  void setUp() {
    when(networkingServiceMock.getNetworkingConfig()).thenReturn(networkingConf());
  }

  @Test
  void defaultPojaConf7_canBe_created() {
    var factory = new PojaConfFactory(networkingServiceMock, EMAIL_CONF);
    var actualPojaConf7 = factory.default7();

    assertEquals(expectedPojaConf7(), actualPojaConf7);
  }

  @Test
  void defaultPojaConf8_canBe_created() {
    var factory = new PojaConfFactory(networkingServiceMock, EMAIL_CONF);
    var actualPojaConf8 = factory.default8();

    assertEquals(expectedPojaConf8(), actualPojaConf8);
  }

  @Test
  void defaultPojaConf9_canBe_created() {
    var factory = new PojaConfFactory(networkingServiceMock, EMAIL_CONF);
    var actualPojaConf9 = factory.default9();

    assertEquals(expectedPojaConf9(), actualPojaConf9);
  }

  static PojaConf7 expectedPojaConf7() {
    return PojaConf7.builder()
        .general(
            PojaConf7.General.builder()
                .withSnapstart(true)
                .javaMainClass("PojaApplication")
                .publicGeneratorVersion(POJA_7.getPublicGeneratorVersion())
                .customJavaRepositories(List.of())
                .customJavaDeps(List.of())
                .customJavaEnvVars(List.of())
                .build())
        .compute(
            new PojaConf2.Compute(
                BigDecimal.valueOf(512),
                BigDecimal.valueOf(30),
                LAMBDA_URL,
                BigDecimal.valueOf(1024),
                BigDecimal.valueOf(1024),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(5),
                WithQueuesNbEnum.NUMBER_0,
                BigDecimal.valueOf(30 * 1000),
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(600)))
        .integration(new PojaConf1.Integration(false, false, false, false, false))
        .genApiClient(new PojaConf1.GenApiClient(null, false, null, null, null, null))
        .concurrency(BASIC_USER_CONCURRENCY)
        .testing(new PojaConf1.TestingConf("FacadeIT", BigDecimal.ZERO))
        .database(new PojaConf2.Database(NONE))
        .mailing(mailingConf())
        .networking(networkingConf())
        .scheduledTasks(List.of())
        .build();
  }

  static PojaConf8 expectedPojaConf8() {
    return PojaConf8.builder()
        .general(
            PojaConf8.General.builder()
                .withSnapstart(true)
                .javaMainClass("PojaApplication")
                .publicGeneratorVersion(POJA_8.getPublicGeneratorVersion())
                .customJavaRepositories(List.of())
                .customJavaDeps(List.of())
                .customJavaEnvVars(List.of())
                .build())
        .compute(
            new PojaConf2.Compute(
                BigDecimal.valueOf(512),
                BigDecimal.valueOf(30),
                LAMBDA_URL,
                BigDecimal.valueOf(1024),
                BigDecimal.valueOf(1024),
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(5),
                WithQueuesNbEnum.NUMBER_0,
                BigDecimal.valueOf(30 * 1000),
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(600)))
        .integration(new PojaConf1.Integration(false, false, false, false, false))
        .genApiClient(new PojaConf1.GenApiClient(null, false, null, null, null, null))
        .concurrency(BASIC_USER_CONCURRENCY)
        .testing(new PojaConf1.TestingConf("FacadeIT", BigDecimal.ZERO))
        .database(new PojaConf2.Database(NONE))
        .mailing(mailingConf())
        .networking(networkingConf())
        .scheduledTasks(List.of())
        .build();
  }

  static PojaConf9 expectedPojaConf9() {
    return PojaConf9.builder()
        .general(
            PojaConf9.General.builder()
                .withSnapstart(true)
                .javaMainClass("PojaApplication")
                .publicGeneratorVersion(POJA_9.getPublicGeneratorVersion())
                .customJavaRepositories(List.of())
                .customJavaDeps(List.of())
                .customJavaEnvVars(List.of())
                .build())
        .compute(
            new PojaConf9.Compute(
                BigDecimal.valueOf(512),
                BigDecimal.valueOf(30),
                PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.LAMBDA_URL,
                BigDecimal.valueOf(30 * 1000),
                List.of()))
        .integration(new PojaConf1.Integration(false, false, false, false, false))
        .genApiClient(new PojaConf1.GenApiClient(null, false, null, null, null, null))
        .concurrency(PojaConf9Concurrency.BASIC_USER_CONCURRENCY)
        .testing(new PojaConf1.TestingConf("FacadeIT", BigDecimal.ZERO))
        .database(new PojaConf2.Database(NONE))
        .mailing(mailingConf())
        .networking(networkingConf())
        .scheduledTasks(List.of())
        .build();
  }

  static PojaConf1.MailingConf mailingConf() {
    return new PojaConf1.MailingConf(SES_SOURCE);
  }
}
