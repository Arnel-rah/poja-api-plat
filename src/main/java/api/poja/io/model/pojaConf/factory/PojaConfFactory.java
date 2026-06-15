package api.poja.io.model.pojaConf.factory;

import static api.poja.io.model.PojaVersion.POJA_6;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.PojaVersion.POJA_8;
import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.model.pojaConf.conf2.PojaConf2.Compute.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
import static api.poja.io.model.pojaConf.conf2.PojaConf2.Database.WithDatabaseEnum.NONE;
import static api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency.BASIC_USER_CONCURRENCY;
import static java.math.BigDecimal.ZERO;

import api.poja.io.endpoint.rest.model.WithQueuesNbEnum;
import api.poja.io.mail.EmailConf;
import api.poja.io.model.pojaConf.NetworkingConfig;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf6.PojaConf6;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.model.pojaConf.conf9.PojaConf9Concurrency;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public final class PojaConfFactory {

  static final BigDecimal FRONTAL_MEMORY = BigDecimal.valueOf(512);
  static final BigDecimal FRONTAL_TIMEOUT = BigDecimal.valueOf(30);
  static final BigDecimal WORKER_MEMORY = BigDecimal.valueOf(1024);
  static final BigDecimal WORKER_BATCH = BigDecimal.valueOf(5);
  static final BigDecimal API_GW_TIMEOUT = BigDecimal.valueOf(30_000);
  static final BigDecimal WORKER_FUNCTION_TIMEOUT = BigDecimal.valueOf(600);
  static final String DEFAULT_JAVA_FACADE_IT = "FacadeIT";
  static final String DEFAULT_JAVA_MAIN_CLASS = "PojaApplication";

  static final PojaConf1.GenApiClient DEFAULT_GEN_API_CLIENT_CONF =
      new PojaConf1.GenApiClient(null, false, null, null, null, null);

  static final PojaConf1.Integration DEFAULT_INTEGRATION_CONF =
      new PojaConf1.Integration(false, false, false, false, false);

  static final PojaConf1.TestingConf DEFAULT_TESTING_CONF =
      new PojaConf1.TestingConf(DEFAULT_JAVA_FACADE_IT, ZERO);

  static final PojaConf2.Database DEFAULT_DATABASE2_CONF = new PojaConf2.Database(NONE);

  static final PojaConf2.Compute DEFAULT_COMPUTE2_CONF =
      new PojaConf2.Compute(
          FRONTAL_MEMORY,
          FRONTAL_TIMEOUT,
          LAMBDA_URL,
          WORKER_MEMORY,
          WORKER_MEMORY,
          WORKER_BATCH,
          WORKER_BATCH,
          WithQueuesNbEnum.NUMBER_0,
          API_GW_TIMEOUT,
          WORKER_FUNCTION_TIMEOUT,
          WORKER_FUNCTION_TIMEOUT);

  static final PojaConf6.General DEFAULT_GENERAL6_CONF =
      PojaConf6.General.builder()
          .publicGeneratorVersion(POJA_6.getPublicGeneratorVersion())
          .withSnapstart(true)
          .customJavaDeps(List.of())
          .customJavaRepositories(List.of())
          .customJavaEnvVars(List.of())
          // note(!): _MUST_ be manually set for proper use. They are typically provided by user
          .appName(null)
          .packageFullName(null)
          .pojaDomainOwner(null)
          .pojaPythonRepositoryName(null)
          .pojaPythonRepositoryDomain(null)
          .build();

  static final PojaConf7.General DEFAULT_GENERAL7_CONF =
      PojaConf7.General.builder()
          .publicGeneratorVersion(POJA_7.getPublicGeneratorVersion())
          .javaMainClass(DEFAULT_JAVA_MAIN_CLASS)
          .withSnapstart(true)
          .customJavaDeps(List.of())
          .customJavaRepositories(List.of())
          .customJavaEnvVars(List.of())
          // note(!): _MUST_ be manually set for proper use. They are typically provided by user
          .appName(null)
          .packageFullName(null)
          .pojaDomainOwner(null)
          .pojaPythonRepositoryName(null)
          .pojaPythonRepositoryDomain(null)
          .build();

  static final PojaConf8.General DEFAULT_GENERAL8_CONF =
      PojaConf8.General.builder()
          .publicGeneratorVersion(POJA_8.getPublicGeneratorVersion())
          .javaMainClass(DEFAULT_JAVA_MAIN_CLASS)
          .withSnapstart(true)
          .customJavaDeps(List.of())
          .customJavaRepositories(List.of())
          .customJavaEnvVars(List.of())
          // note(!): _MUST_ be manually set for proper use. They are typically provided by user
          .appName(null)
          .packageFullName(null)
          .pojaDomainOwner(null)
          .pojaPythonRepositoryName(null)
          .pojaPythonRepositoryDomain(null)
          .build();

  static final PojaConf9.Compute DEFAULT_COMPUTE9_CONF =
      new PojaConf9.Compute(
          FRONTAL_MEMORY,
          FRONTAL_TIMEOUT,
          PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.LAMBDA_URL,
          API_GW_TIMEOUT,
          List.of());

  static final PojaConf9.General DEFAULT_GENERAL9_CONF =
      PojaConf9.General.builder()
          .publicGeneratorVersion(POJA_9.getPublicGeneratorVersion())
          .javaMainClass(DEFAULT_JAVA_MAIN_CLASS)
          .withSnapstart(true)
          .customJavaDeps(List.of())
          .customJavaRepositories(List.of())
          .customJavaEnvVars(List.of())
          // note(!): _MUST_ be manually set for proper use. They are typically provided by user
          .appName(null)
          .packageFullName(null)
          .pojaDomainOwner(null)
          .pojaPythonRepositoryName(null)
          .pojaPythonRepositoryDomain(null)
          .build();

  private final NetworkingService networkingService;
  private final EmailConf emailConf;

  public PojaConf6 default6() {
    return PojaConf6.builder()
        .general(DEFAULT_GENERAL6_CONF)
        .compute(DEFAULT_COMPUTE2_CONF)
        .integration(DEFAULT_INTEGRATION_CONF)
        .genApiClient(DEFAULT_GEN_API_CLIENT_CONF)
        .concurrency(BASIC_USER_CONCURRENCY)
        .testing(DEFAULT_TESTING_CONF)
        .database(DEFAULT_DATABASE2_CONF)
        .mailing(mailingConf())
        .networking(networkingConfig())
        .scheduledTasks(List.of())
        .build();
  }

  public PojaConf7 default7() {
    return PojaConf7.builder()
        .general(DEFAULT_GENERAL7_CONF)
        .compute(DEFAULT_COMPUTE2_CONF)
        .integration(DEFAULT_INTEGRATION_CONF)
        .genApiClient(DEFAULT_GEN_API_CLIENT_CONF)
        .concurrency(BASIC_USER_CONCURRENCY)
        .testing(DEFAULT_TESTING_CONF)
        .database(DEFAULT_DATABASE2_CONF)
        .mailing(mailingConf())
        .networking(networkingConfig())
        .scheduledTasks(List.of())
        .build();
  }

  public PojaConf8 default8() {
    return PojaConf8.builder()
        .general(DEFAULT_GENERAL8_CONF)
        .compute(DEFAULT_COMPUTE2_CONF)
        .integration(DEFAULT_INTEGRATION_CONF)
        .genApiClient(DEFAULT_GEN_API_CLIENT_CONF)
        .concurrency(BASIC_USER_CONCURRENCY)
        .testing(DEFAULT_TESTING_CONF)
        .database(DEFAULT_DATABASE2_CONF)
        .mailing(mailingConf())
        .networking(networkingConfig())
        .scheduledTasks(List.of())
        .build();
  }

  public PojaConf9 default9() {
    return PojaConf9.builder()
        .general(DEFAULT_GENERAL9_CONF)
        .compute(DEFAULT_COMPUTE9_CONF)
        .integration(DEFAULT_INTEGRATION_CONF)
        .genApiClient(DEFAULT_GEN_API_CLIENT_CONF)
        .concurrency(PojaConf9Concurrency.BASIC_USER_CONCURRENCY)
        .testing(DEFAULT_TESTING_CONF)
        .database(DEFAULT_DATABASE2_CONF)
        .mailing(mailingConf())
        .networking(networkingConfig())
        .scheduledTasks(List.of())
        .build();
  }

  private PojaConf1.MailingConf mailingConf() {
    return new PojaConf1.MailingConf(emailConf.getSesSource());
  }

  private NetworkingConfig networkingConfig() {
    return networkingService.getNetworkingConfig();
  }
}
