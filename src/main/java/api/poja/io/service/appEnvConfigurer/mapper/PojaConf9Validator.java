package api.poja.io.service.appEnvConfigurer.mapper;

import static api.poja.io.endpoint.rest.model.WithQueuesNbEnum.NUMBER_0;
import static api.poja.io.model.pojaConf.conf9.PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.HTTP_API;
import static api.poja.io.model.pojaConf.conf9.PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
import static api.poja.io.service.appEnvConfigurer.mapper.PojaConf6Validator.validateEnvVars;
import static api.poja.io.service.validator.AppNameValidator.appNamePatternWithLen;
import static api.poja.io.service.validator.AppNameValidator.matchesAppNamePattern;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import api.poja.io.endpoint.rest.model.WithQueuesNbEnum;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf3.PojaConf3;
import api.poja.io.model.pojaConf.conf4.PojaConf4;
import api.poja.io.model.pojaConf.conf5.PojaConf5;
import api.poja.io.model.pojaConf.conf6.PojaConf6;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.service.validator.EmailValidator;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PojaConf9Validator implements PojaConfValidator<PojaConf9> {
  private final EmailValidator emailValidator;
  private static final int POJA_CONF_APP_NAME_MAX_LENGTH = 20;
  private static final Pattern packageNameRegexPattern =
      Pattern.compile("^([a-z][a-z0-9]*)(\\.([a-z][a-z0-9]*))+$");
  private final ScheduledTasksValidator scheduledTasksValidator;

  @Override
  public void accept(@Nullable PojaConf9 from, PojaConf9 to, Boolean isPremium) {
    Set<String> invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to, isPremium);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private Set<String> invalidAttributes(PojaConf9 to, boolean isPremium) {
    Set<String> exceptionMessages = new HashSet<>();
    var generalConf = to.general();
    var integrationConf = to.integration();
    var mailingConf = to.mailing();
    var testingConf = to.testing();
    var computeConf = to.compute();
    var databaseConf = to.database();
    var concurrencyConf = to.concurrency();

    if (generalConf == null) {
      exceptionMessages.add("general is mandatory.");
    } else {
      if (generalConf.appName() == null) {
        exceptionMessages.add("general.app_name is mandatory.");
      } else {
        if (!matchesAppNamePattern(
            generalConf.appName(), appNamePatternWithLen(POJA_CONF_APP_NAME_MAX_LENGTH))) {
          exceptionMessages.add(
              "general.app_name must not have more than "
                  + POJA_CONF_APP_NAME_MAX_LENGTH
                  + " characters and contain only lowercase"
                  + " letters, numbers and hyphen (-).");
        }
      }
      if (generalConf.packageFullName() == null) {
        exceptionMessages.add("general.package_full_name is mandatory.");
      } else {
        if (!isPackageFullNameValid(generalConf.packageFullName())) {
          exceptionMessages.add(
              "general.package_full_name must include at least 2 lowercase alphanumeric segments"
                  + " separated by dots.");
        }
      }
      if (generalConf.customJavaDeps() == null) {
        exceptionMessages.add("general.custom_java_deps is mandatory.");
      }
      if (generalConf.customJavaEnvVars() == null) {
        exceptionMessages.add("general.custom_java_env_vars is mandatory.");
      } else {
        validateEnvVars(generalConf.customJavaEnvVars(), exceptionMessages);
      }
      if (generalConf.customJavaRepositories() == null) {
        exceptionMessages.add("general.custom_java_repositories is mandatory.");
      }
    }
    if (integrationConf == null) {
      exceptionMessages.add("integration is mandatory.");
    } else {
      if (integrationConf.withSwaggerUi() == null) {
        exceptionMessages.add("integration.with_swagger_ui is mandatory.");
      }
      if (integrationConf.withCodeql() == null) {
        exceptionMessages.add("integration.with_codeql is mandatory.");
      }
      if (integrationConf.withFileStorage() == null) {
        exceptionMessages.add("integration.with_file_storage is mandatory.");
      }
      if (integrationConf.withSentry() == null) {
        exceptionMessages.add("integration.with_sentry is mandatory.");
      }
      if (integrationConf.withSonar() == null) {
        exceptionMessages.add("integration.with_sonar is mandatory.");
      }
    }
    if (mailingConf == null) {
      exceptionMessages.add("emailing is mandatory.");
    } else {
      if (mailingConf.sesSource() == null) {
        exceptionMessages.add("emailing.ses_source is mandatory.");
      } else {
        if (!emailValidator.test(mailingConf.sesSource())) {
          exceptionMessages.add("emailing.ses_source must be a valid email address.");
        }
      }
    }
    if (testingConf == null) {
      exceptionMessages.add("testing is mandatory.");
    } else {
      if (testingConf.jacocoMinCoverage() == null) {
        exceptionMessages.add("testing.jacoco_min_coverage is mandatory.");
      }
      if (testingConf.javaFacadeIt() == null) {
        exceptionMessages.add("testing.java_facade_it is mandatory.");
      }
    }
    if (computeConf == null) {
      exceptionMessages.add("compute is mandatory.");
    } else {
      if (computeConf.frontalMemory() == null) {
        exceptionMessages.add("compute.compute_frontal_memory is mandatory.");
      }
      if (computeConf.frontalFunctionTimeout() == null) {
        exceptionMessages.add("compute.frontal_function_timeout is mandatory.");
      }
      var frontalFunctionInvocationMethod = computeConf.frontalFunctionInvocationMethod();
      var frontalTimeout = computeConf.frontalFunctionTimeout();
      if (frontalTimeout != null) {
        if (HTTP_API.equals(frontalFunctionInvocationMethod)
            && frontalTimeout.compareTo(new BigDecimal("30")) > 0) {
          exceptionMessages.add(
              "compute.frontal_function_timeout must be at most 30 seconds when "
                  + "compute.frontal_function_invocation_method is HTTP_API.");
        }
        if (LAMBDA_URL.equals(frontalFunctionInvocationMethod)
            && frontalTimeout.compareTo(new BigDecimal("900")) > 0) {
          exceptionMessages.add(
              "compute.frontal_function_timeout must be at most 900 seconds when "
                  + "compute.frontal_function_invocation_method is LAMBDA_URL.");
        }
      }
      if (!isPremium) {
        if (HTTP_API.equals(frontalFunctionInvocationMethod)) {
          exceptionMessages.add(
              "compute.frontal_function_invocation_method = HTTP_API is a premium feature.");
        }
      }
      if (computeConf.workers() == null) {
        exceptionMessages.add("compute.workers is mandatory.");
      }
      if (computeConf.workers() != null && computeConf.workers().size() > 10) {
        exceptionMessages.add("compute.workers cannot have more than 10 elements.");
      }
      if (computeConf.workers() != null && computeConf.workers().size() > 2 && !isPremium) {
        exceptionMessages.add("compute.workers with more than 2 workers is a premium feature.");
      }
    }
    if (databaseConf == null) {
      exceptionMessages.add("database is mandatory.");
    } else {
      if (databaseConf.dbType() == null) {
        exceptionMessages.add("database.db_type is mandatory.");
      }
    }
    if (concurrencyConf == null) {
      exceptionMessages.add("concurrency is mandatory.");
    }
    if (to.scheduledTasks() != null) {
      var scheduledTasksExceptionMessages =
          scheduledTasksValidator.errorMessages9(to.scheduledTasks());
      if (!scheduledTasksExceptionMessages.isEmpty()) {
        exceptionMessages.add(String.join(" ", scheduledTasksExceptionMessages));
      }
    }
    return exceptionMessages;
  }

  private boolean isPackageFullNameValid(String packageFullName) {
    return packageNameRegexPattern.matcher(packageFullName).matches();
  }

  @Override
  public Set<String> outputIllegalTransitions(
      @Nullable PojaConf9 from, PojaConf9 to, Boolean isPremium) {
    if (from == null) {
      return Set.of();
    }
    Set<String> illegalTransitions = new HashSet<>();
    var fromintegration = from.integration();
    if (fromintegration != null) {
      Boolean withFileStorageFrom = fromintegration.withFileStorage();
      var tointegration = to.integration();
      if (tointegration != null) {
        Boolean withFileStorageTo = tointegration.withFileStorage();
        if (TRUE.equals(withFileStorageFrom) && FALSE.equals(withFileStorageTo)) {
          illegalTransitions.add(
              "illegal transition: from integration.withFileStorage TRUE to"
                  + " integration.withFileStorage FALSE.");
        }
      }
    }
    if (from.compute() != null && to.compute() != null) {
      var fromWorkers = from.compute().workers();
      var toWorkers = to.compute().workers();
      if (fromWorkers != null && !fromWorkers.isEmpty() && toWorkers.size() < fromWorkers.size()) {
        illegalTransitions.add(
            "illegal transition: from compute.workers "
                + fromWorkers.size()
                + " to compute.workers "
                + toWorkers.size()
                + ".");
      }
    }
    return illegalTransitions;
  }

  public void accept(@Nullable PojaConf1 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf2 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf3 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf4 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf5 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf6 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf7 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(@Nullable PojaConf8 from, PojaConf9 to, boolean isPremium) {
    var invalidAttributes = invalidAttributes(to, isPremium);
    var illegalTransitions = outputIllegalTransitions(from, to);
    invalidAttributes.addAll(illegalTransitions);
    String exceptionMessage = String.join(" ", invalidAttributes);
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf1 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf2 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf3 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf4 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf5 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf6 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf7 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  public Set<String> outputIllegalTransitions(@Nullable PojaConf8 from, PojaConf9 to) {
    if (from == null) return Set.of();
    Set<String> illegalTransitions = new HashSet<>();
    checkFileStorageTransition(from.integration(), to.integration(), illegalTransitions);
    checkWorkersTransitionFromLegacyQueuesNb(
        from.compute() == null ? null : from.compute().withQueuesNb(), to, illegalTransitions);
    return illegalTransitions;
  }

  private void checkFileStorageTransition(
      PojaConf1.Integration fromIntegration,
      PojaConf1.Integration toIntegration,
      Set<String> illegalTransitions) {
    if (fromIntegration != null && toIntegration != null) {
      if (TRUE.equals(fromIntegration.withFileStorage())
          && FALSE.equals(toIntegration.withFileStorage())) {
        illegalTransitions.add(
            "illegal transition: from integration.withFileStorage TRUE to"
                + " integration.withFileStorage FALSE.");
      }
    }
  }

  private void checkWorkersTransitionFromLegacyQueuesNb(
      WithQueuesNbEnum fromWithQueuesNb, PojaConf9 to, Set<String> illegalTransitions) {
    if (fromWithQueuesNb != null && !NUMBER_0.equals(fromWithQueuesNb)) {
      var toWorkers = to.compute() == null ? null : to.compute().workers();
      if (toWorkers != null && toWorkers.size() < 2) {
        illegalTransitions.add(
            "illegal transition: from compute.withQueuesNb "
                + fromWithQueuesNb
                + " to compute.workers with less than 2 workers.");
      }
    }
  }
}
