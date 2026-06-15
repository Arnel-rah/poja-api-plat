package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.endpoint.rest.model.WithQueuesNbEnum.NUMBER_0;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.service.validator.EmailValidator;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PojaConf1Validator implements PojaConfValidator<PojaConf1> {
  private final EmailValidator emailValidator;
  private static final Pattern appNameRegexPattern = Pattern.compile("^[a-z0-9-]{1,20}$");
  private static final Pattern packageNameRegexPattern =
      Pattern.compile("^([a-z][a-z0-9]*)\\.([a-z][a-z0-9]*)\\.([a-z][a-z0-9]*)$");

  @Override
  public void accept(@Nullable PojaConf1 from, PojaConf1 to, Boolean isPremium) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    var generalConf = to.general();
    var integrationConf = to.integration();
    var mailingConf = to.mailing();
    var testingConf = to.testing();
    var computeConf = to.compute();
    var databaseConf = to.database();
    var concurrencyConf = to.concurrency();

    if (generalConf == null) {
      exceptionMessageBuilder.append("general is mandatory. ");
    } else {
      if (generalConf.appName() == null) {
        exceptionMessageBuilder.append("general.app_name is mandatory. ");
      } else {
        if (!isAValidAppName(generalConf.appName())) {
          exceptionMessageBuilder.append(
              "general.app_name must not have more than 20 characters and contain only lowercase"
                  + " letters, numbers and hyphen (-). ");
        }
      }
      if (generalConf.withSnapstart() == null) {
        exceptionMessageBuilder.append("general.with_snapstart is mandatory. ");
      }
      if (generalConf.packageFullName() == null) {
        exceptionMessageBuilder.append("general.package_full_name is mandatory. ");
      } else {
        if (!isPackageFullNameValid(generalConf.packageFullName())) {
          exceptionMessageBuilder.append(
              "general.package_full_name must include three lowercase alphanumeric segments"
                  + " separated by dots. ");
        }
      }
      if (generalConf.withQueuesNb() == null) {
        exceptionMessageBuilder.append("general.queues_nb is mandatory. ");
      }
      if (generalConf.customJavaDeps() == null) {
        exceptionMessageBuilder.append("general.custom_java_deps is mandatory. ");
      }
      if (generalConf.customJavaEnvVars() == null) {
        exceptionMessageBuilder.append("general.custom_java_env_vars is mandatory. ");
      }
      if (generalConf.customJavaRepositories() == null) {
        exceptionMessageBuilder.append("general.custom_java_repositories is mandatory. ");
      }
    }
    if (integrationConf == null) {
      exceptionMessageBuilder.append("integration is mandatory. ");
    } else {
      if (integrationConf.withSwaggerUi() == null) {
        exceptionMessageBuilder.append("integration.with_swagger_ui is mandatory. ");
      }
      if (integrationConf.withCodeql() == null) {
        exceptionMessageBuilder.append("integration.with_codeql is mandatory. ");
      }
      if (integrationConf.withFileStorage() == null) {
        exceptionMessageBuilder.append("integration.with_file_storage is mandatory. ");
      }
      if (integrationConf.withSentry() == null) {
        exceptionMessageBuilder.append("integration.with_sentry is mandatory. ");
      }
      if (integrationConf.withSonar() == null) {
        exceptionMessageBuilder.append("integration.with_sonar is mandatory. ");
      }
    }
    if (mailingConf == null) {
      exceptionMessageBuilder.append("emailing is mandatory. ");
    } else {
      if (mailingConf.sesSource() == null) {
        exceptionMessageBuilder.append("emailing.ses_source is mandatory. ");
      } else {
        if (!emailValidator.test(mailingConf.sesSource())) {
          exceptionMessageBuilder.append("emailing.ses_source must be a valid email address. ");
        }
      }
    }
    if (testingConf == null) {
      exceptionMessageBuilder.append("testing is mandatory. ");
    } else {
      if (testingConf.jacocoMinCoverage() == null) {
        exceptionMessageBuilder.append("testing.jacoco_min_coverage is mandatory. ");
      }
      if (testingConf.javaFacadeIt() == null) {
        exceptionMessageBuilder.append("testing.java_facade_it is mandatory. ");
      }
    }
    if (computeConf == null) {
      exceptionMessageBuilder.append("compute is mandatory. ");
    } else {
      if (computeConf.frontalMemory() == null) {
        exceptionMessageBuilder.append("compute.compute_frontal_memory is mandatory. ");
      }
      if (computeConf.frontalFunctionTimeout() == null) {
        exceptionMessageBuilder.append("compute.frontal_function_timeout is mandatory. ");
      }
      if (computeConf.workerMemory() == null) {
        exceptionMessageBuilder.append("compute.worker_memory is mandatory. ");
      }
      if (computeConf.workerFunction1Timeout() == null) {
        exceptionMessageBuilder.append("compute.worker_function_1_timeout is mandatory. ");
      }
      if (computeConf.workerFunction2Timeout() == null) {
        exceptionMessageBuilder.append("compute.worker_function_2_timeout is mandatory. ");
      }
      if (computeConf.workerBatch() == null) {
        exceptionMessageBuilder.append("compute.worker_batch is mandatory. ");
      }
    }
    if (databaseConf == null) {
      exceptionMessageBuilder.append("database is mandatory. ");
    } else {
      if (databaseConf.dbType() == null) {
        exceptionMessageBuilder.append("database.db_type is mandatory. ");
      }
    }
    if (concurrencyConf == null) {
      exceptionMessageBuilder.append("database.concurrency is mandatory. ");
    }
    var illegalTransitions = outputIllegalTransitions(from, to, isPremium);
    if (!illegalTransitions.isEmpty()) {
      exceptionMessageBuilder.append(String.join(" ", illegalTransitions));
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private boolean isPackageFullNameValid(String packageFullName) {
    return packageNameRegexPattern.matcher(packageFullName).matches();
  }

  private boolean isAValidAppName(String appName) {
    return appNameRegexPattern.matcher(appName).matches();
  }

  @Override
  public Set<String> outputIllegalTransitions(
      @Nullable PojaConf1 from, PojaConf1 to, Boolean isPremium) {
    if (from == null) {
      return Set.of();
    }
    Set<String> illegalTransitions = new HashSet<>();

    var fromIntegration = from.integration();
    if (fromIntegration != null) {
      Boolean withFileStorageFrom = fromIntegration.withFileStorage();
      var toIntegration = to.integration();
      if (toIntegration != null) {
        Boolean withFileStorageTo = toIntegration.withFileStorage();
        if (TRUE.equals(withFileStorageFrom) && FALSE.equals(withFileStorageTo)) {
          illegalTransitions.add(
              "illegal transition: from integration.withFileStorage TRUE to"
                  + " integration.withFileStorage FALSE.");
        }
      }
    }
    if (from.general() != null) {
      var fromWithQueuesNb = from.general().withQueuesNb();
      if (to.general() != null) {
        var toWithQueuesNb = to.general().withQueuesNb();
        if (!NUMBER_0.equals(fromWithQueuesNb) && NUMBER_0.equals(toWithQueuesNb)) {
          illegalTransitions.add(
              "illegal transition: from general.withQueuesNb "
                  + fromWithQueuesNb
                  + " to general.withQueuesNb"
                  + toWithQueuesNb
                  + ".");
        }
      }
    }
    return illegalTransitions;
  }
}
