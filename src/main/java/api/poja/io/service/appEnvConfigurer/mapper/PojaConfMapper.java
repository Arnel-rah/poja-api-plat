package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.model.PojaVersion.POJA_1;
import static api.poja.io.model.PojaVersion.POJA_2;
import static api.poja.io.model.PojaVersion.POJA_3;
import static api.poja.io.model.PojaVersion.POJA_4;
import static api.poja.io.model.PojaVersion.POJA_5;
import static api.poja.io.model.PojaVersion.POJA_6;
import static api.poja.io.model.PojaVersion.POJA_7;
import static api.poja.io.model.PojaVersion.POJA_8;
import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.service.validator.AppNameValidator.DOMAIN_APP_NAME_LEN;

import api.poja.io.endpoint.rest.model.GeneralPojaConf;
import api.poja.io.endpoint.rest.model.GeneralPojaConf2;
import api.poja.io.endpoint.rest.model.GeneralPojaConf6;
import api.poja.io.endpoint.rest.model.GeneralPojaConf7;
import api.poja.io.endpoint.rest.model.ScheduledTask;
import api.poja.io.endpoint.rest.model.ScheduledTask9;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf1.PojaConf1Concurrency;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency;
import api.poja.io.model.pojaConf.conf3.PojaConf3;
import api.poja.io.model.pojaConf.conf4.PojaConf4;
import api.poja.io.model.pojaConf.conf5.PojaConf5;
import api.poja.io.model.pojaConf.conf6.PojaConf6;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.model.pojaConf.conf9.PojaConf9Concurrency;
import api.poja.io.service.appEnvConfigurer.NetworkingService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PojaConfMapper {
  private final NetworkingService networkingService;

  public PojaConf1 toDomainPojaConf1(
      api.poja.io.endpoint.rest.model.PojaConf1 rest,
      PojaConf1Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .withQueuesNb(generalConf.getWithQueuesNb())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    return PojaConf1.builder()
        .general(
            new PojaConf1.General(
                updatedGeneralConf, null, null, null, POJA_1.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf1.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf1.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .build();
  }

  public PojaConf2 toDomainPojaConf2(
      api.poja.io.endpoint.rest.model.PojaConf2 rest,
      PojaConf2Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf2()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf2.builder()
        .general(
            new PojaConf2.General(
                updatedGeneralConf, null, null, null, POJA_2.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::new).toList())
        .build();
  }

  public PojaConf3 toDomainPojaConf3(
      api.poja.io.endpoint.rest.model.PojaConf3 rest,
      PojaConf2Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf2()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf3.builder()
        .general(
            new PojaConf2.General(
                updatedGeneralConf, null, null, null, POJA_3.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::new).toList())
        .build();
  }

  public PojaConf4 toDomainPojaConf4(
      api.poja.io.endpoint.rest.model.PojaConf4 rest,
      PojaConf2Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf2()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf4.builder()
        .general(
            new PojaConf2.General(
                updatedGeneralConf, null, null, null, POJA_4.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::new).toList())
        .build();
  }

  public PojaConf5 toDomainPojaConf5(
      api.poja.io.endpoint.rest.model.PojaConf5 rest,
      PojaConf2Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf2()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf5.builder()
        .general(
            new PojaConf2.General(
                updatedGeneralConf, null, null, null, POJA_5.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::new).toList())
        .build();
  }

  public PojaConf6 toDomainPojaConf6(
      api.poja.io.endpoint.rest.model.PojaConf6 rest,
      PojaConf2Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf6()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf6.builder()
        .general(
            new PojaConf6.General(
                updatedGeneralConf, null, null, null, POJA_6.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::new).toList())
        .build();
  }

  public PojaConf7 toDomainPojaConf7(
      api.poja.io.endpoint.rest.model.PojaConf7 rest,
      PojaConf2Concurrency concurrency,
      String userId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf7()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .javaMainClass(generalConf.getJavaMainClass())
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf7.builder()
        .general(
            new PojaConf7.General(
                updatedGeneralConf, null, null, null, POJA_7.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(PojaConf2.ScheduledTask::new).toList())
        .build();
  }

  public PojaConf8 toDomainPojaConf8(
      api.poja.io.endpoint.rest.model.PojaConf8 rest,
      PojaConf2Concurrency concurrency,
      String userId,
      String appId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf7()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .javaMainClass(generalConf.getJavaMainClass())
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask> scheduledTasks = rest.getScheduledTasks();
    return PojaConf8.builder()
        .general(
            new PojaConf8.General(
                updatedGeneralConf, null, null, null, POJA_8.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf2.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(e -> new PojaConf8.ScheduledTask(e, appId)).toList())
        .build();
  }

  public PojaConf9 toDomainPojaConf9(
      api.poja.io.endpoint.rest.model.PojaConf9 rest,
      PojaConf9Concurrency concurrency,
      String userId,
      String appId) {
    var generalConf = rest.getGeneral();
    var updatedGeneralConf =
        new GeneralPojaConf7()
            .appName(suffixAppName(generalConf.getAppName(), userId))
            .javaMainClass(generalConf.getJavaMainClass())
            .packageFullName(generalConf.getPackageFullName())
            .withSnapstart(generalConf.getWithSnapstart())
            .customJavaDeps(generalConf.getCustomJavaDeps())
            .customJavaEnvVars(generalConf.getCustomJavaEnvVars())
            .customJavaRepositories(generalConf.getCustomJavaRepositories());

    List<ScheduledTask9> scheduledTasks = rest.getScheduledTasks();
    return PojaConf9.builder()
        .general(
            new PojaConf9.General(
                updatedGeneralConf, null, null, null, POJA_9.getPublicGeneratorVersion()))
        .integration(new PojaConf1.Integration(rest.getIntegration()))
        .genApiClient(new PojaConf1.GenApiClient(rest.getGenApiClient()))
        .concurrency(concurrency)
        .compute(new PojaConf9.Compute(rest.getCompute()))
        .mailing(new PojaConf1.MailingConf(rest.getEmailing()))
        .testing(new PojaConf1.TestingConf(rest.getTesting()))
        .database(new PojaConf2.Database(rest.getDatabase()))
        .networking(networkingService.getNetworkingConfig())
        .scheduledTasks(
            scheduledTasks == null
                ? null
                : scheduledTasks.stream().map(e -> new PojaConf9.ScheduledTask(e, appId)).toList())
        .build();
  }

  public static String suffixAppName(String appName, String userId) {
    if (appName.length() <= DOMAIN_APP_NAME_LEN) {
      return appName + "-" + userId.substring(0, 8);
    }
    return appName;
  }
}
