package api.poja.io.model.pojaConf.conf2;

import static api.poja.io.endpoint.rest.model.EventStackSource._1;
import static api.poja.io.endpoint.rest.model.EventStackSource._2;
import static api.poja.io.model.PojaVersion.POJA_2;
import static java.util.Objects.requireNonNull;

import api.poja.io.endpoint.rest.model.ComputeConf2;
import api.poja.io.endpoint.rest.model.DatabaseConf2;
import api.poja.io.endpoint.rest.model.EventStackSource;
import api.poja.io.endpoint.rest.model.GeneralPojaConf2;
import api.poja.io.endpoint.rest.model.WithQueuesNbEnum;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.NetworkingConfig;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@JsonPropertyOrder(alphabetic = true)
@Builder
public record PojaConf2(
    @JsonProperty("general") General general,
    @JsonProperty("integration") PojaConf1.Integration integration,
    @JsonProperty("gen_api_client") PojaConf1.GenApiClient genApiClient,
    @JsonProperty("concurrency") PojaConf2Concurrency concurrency,
    @JsonProperty("compute") Compute compute,
    @JsonProperty("emailing") PojaConf1.MailingConf mailing,
    @JsonProperty("testing") PojaConf1.TestingConf testing,
    @JsonProperty("database") Database database,
    @JsonProperty("networking") NetworkingConfig networking,
    @JsonProperty("scheduled_tasks") List<ScheduledTask> scheduledTasks)
    implements PojaConf {

  @JsonGetter
  public String version() {
    return getVersion().toHumanReadableValue();
  }

  @Override
  public PojaVersion getVersion() {
    return POJA_2;
  }

  public record General(
      @JsonProperty("app_name") String appName,
      @JsonProperty("with_snapstart") Boolean withSnapstart,
      @JsonProperty("package_full_name") String packageFullName,
      @JsonProperty("custom_java_repositories") List<String> customJavaRepositories,
      @JsonProperty("custom_java_deps") List<String> customJavaDeps,
      @JsonProperty("custom_java_env_vars") Map<String, String> customJavaEnvVars,
      @JsonProperty("poja_python_repository_name") String pojaPythonRepositoryName,
      @JsonProperty("poja_python_repository_domain") String pojaPythonRepositoryDomain,
      @JsonProperty("poja_domain_owner") String pojaDomainOwner,
      @JsonProperty(JSON_PROPERTY_PUBLIC_GENERATOR_VERSION) String publicGeneratorVersion) {
    public static final String JSON_PROPERTY_PUBLIC_GENERATOR_VERSION = "public_generator_version";

    @Builder
    public General(
        GeneralPojaConf2 rest,
        String pojaPythonRepositoryName,
        String pojaPythonRepositoryDomain,
        String pojaDomainOwner,
        String publicGeneratorVersion) {
      this(
          rest.getAppName(),
          rest.getWithSnapstart(),
          rest.getPackageFullName(),
          rest.getCustomJavaRepositories(),
          rest.getCustomJavaDeps(),
          rest.getCustomJavaEnvVars(),
          pojaPythonRepositoryName,
          pojaPythonRepositoryDomain,
          pojaDomainOwner,
          publicGeneratorVersion);
    }

    public GeneralPojaConf2 toRest() {
      return new GeneralPojaConf2()
          .appName(appName)
          .withSnapstart(withSnapstart)
          .packageFullName(packageFullName)
          .customJavaDeps(customJavaDeps)
          .customJavaRepositories(customJavaRepositories)
          .customJavaEnvVars(customJavaEnvVars);
    }
  }

  public record Compute(
      @JsonProperty("frontal_memory") BigDecimal frontalMemory,
      @JsonProperty("frontal_function_timeout") BigDecimal frontalFunctionTimeout,
      @JsonProperty("frontal_function_invocation_method")
          Compute.FrontalFunctionInvocationMethodEnum frontalFunctionInvocationMethod,
      @JsonProperty("worker_1_memory") BigDecimal worker1Memory,
      @JsonProperty("worker_2_memory") BigDecimal worker2Memory,
      @JsonProperty("worker_1_batch") BigDecimal worker1Batch,
      @JsonProperty("worker_2_batch") BigDecimal worker2Batch,
      @JsonProperty("with_queues_nb") WithQueuesNbEnum withQueuesNb,
      @JsonProperty("api_gateway_timeout") BigDecimal apiGatewayTimeout,
      @JsonProperty("worker_function_1_timeout") BigDecimal workerFunction1Timeout,
      @JsonProperty("worker_function_2_timeout") BigDecimal workerFunction2Timeout) {

    public enum FrontalFunctionInvocationMethodEnum {
      LAMBDA_URL("lambda-url"),
      HTTP_API("http-api");

      private final String value;

      FrontalFunctionInvocationMethodEnum(String value) {
        this.value = value;
      }

      @JsonValue
      public String getValue() {
        return value;
      }

      @Override
      public String toString() {
        return String.valueOf(value);
      }

      @JsonCreator
      public static FrontalFunctionInvocationMethodEnum fromString(String value) {
        for (FrontalFunctionInvocationMethodEnum b : FrontalFunctionInvocationMethodEnum.values()) {
          if (b.value.equals(value)) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
      }

      public ComputeConf2.FrontalFunctionInvocationMethodEnum toRest() {
        return switch (this) {
          case HTTP_API -> ComputeConf2.FrontalFunctionInvocationMethodEnum.HTTP_API;
          case LAMBDA_URL -> ComputeConf2.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
          default -> throw new IllegalStateException("Unexpected value: " + value);
        };
      }

      public static FrontalFunctionInvocationMethodEnum fromRest(
          ComputeConf2.FrontalFunctionInvocationMethodEnum rest) {
        return FrontalFunctionInvocationMethodEnum.valueOf(rest.getValue());
      }
    }

    @Builder
    public Compute(ComputeConf2 rest) {
      this(
          rest.getFrontalMemory(),
          rest.getFrontalFunctionTimeout(),
          FrontalFunctionInvocationMethodEnum.fromRest(
              requireNonNull(rest.getFrontalFunctionInvocationMethod())),
          rest.getWorker1Memory(),
          rest.getWorker2Memory(),
          rest.getWorker1Batch(),
          rest.getWorker2Batch(),
          rest.getWithQueuesNb(),
          rest.getApiGatewayTimeout(),
          rest.getWorkerFunction1Timeout(),
          rest.getWorkerFunction2Timeout());
    }

    public ComputeConf2 toRest() {
      return new ComputeConf2()
          .frontalMemory(frontalMemory)
          .frontalFunctionTimeout(frontalFunctionTimeout)
          .frontalFunctionInvocationMethod(frontalFunctionInvocationMethod.toRest())
          .worker1Memory(worker1Memory)
          .worker2Memory(worker2Memory)
          .worker1Batch(worker1Batch)
          .worker2Batch(worker2Batch)
          .withQueuesNb(withQueuesNb)
          .apiGatewayTimeout(apiGatewayTimeout)
          .workerFunction1Timeout(workerFunction1Timeout)
          .workerFunction2Timeout(workerFunction2Timeout);
    }
  }

  public record Database(@JsonProperty("with_database") WithDatabaseEnum dbType) {
    @Builder
    public Database(DatabaseConf2 rest) {
      this(WithDatabaseEnum.fromRest(rest.getWithDatabase()));
    }

    public enum WithDatabaseEnum {
      NONE("NONE"),
      NON_POJA_MANAGED_POSTGRES("NON_POJA_MANAGED_POSTGRES"),

      SELF_MANAGED_JPA_DATABASE("SELF_MANAGED_JPA_DATABASE");

      private final String value;

      WithDatabaseEnum(String value) {
        this.value = value;
      }

      @JsonValue
      public String getValue() {
        return value;
      }

      @Override
      public String toString() {
        return String.valueOf(value);
      }

      @JsonCreator
      public static WithDatabaseEnum fromValue(String value) {
        for (WithDatabaseEnum b : WithDatabaseEnum.values()) {
          if (b.value.equals(value)) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
      }

      public static WithDatabaseEnum fromRest(DatabaseConf2.WithDatabaseEnum rest) {
        for (WithDatabaseEnum b : WithDatabaseEnum.values()) {
          if (b.value.equals(rest.getValue())) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + rest.getValue() + "'");
      }

      public static DatabaseConf2.WithDatabaseEnum toRest(WithDatabaseEnum domain) {
        if (NON_POJA_MANAGED_POSTGRES.equals(domain)) {
          return DatabaseConf2.WithDatabaseEnum.SELF_MANAGED_JPA_DATABASE;
        }
        return DatabaseConf2.WithDatabaseEnum.fromValue(domain.getValue());
      }
    }

    public DatabaseConf2 toRest() {
      return new DatabaseConf2().withDatabase(WithDatabaseEnum.toRest(dbType));
    }
  }

  public record ScheduledTask(
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("class_name") String className,
      @JsonProperty("schedule_expression") String scheduleExpression,
      @JsonProperty("event_stack_source") Integer eventStackSource) {
    @Builder
    public ScheduledTask(api.poja.io.endpoint.rest.model.ScheduledTask rest) {
      this(
          rest.getName(),
          rest.getDescription(),
          rest.getClassName(),
          rest.getScheduleExpression(),
          toDomainEventStackSource(rest.getEventStackSource()));
    }

    private static Integer toDomainEventStackSource(EventStackSource eventStackSource) {
      return switch (eventStackSource) {
        case _1 -> 1;
        case _2 -> 2;
      };
    }

    private static EventStackSource toRestEventStackSource(Integer eventStackSource) {
      return switch (eventStackSource) {
        case 1 -> _1;
        case 2 -> _2;
        default -> throw new IllegalStateException("Unexpected value: " + eventStackSource);
      };
    }

    public api.poja.io.endpoint.rest.model.ScheduledTask toRest() {
      return new api.poja.io.endpoint.rest.model.ScheduledTask()
          .name(name)
          .description(description)
          .className(className)
          .scheduleExpression(scheduleExpression)
          .eventStackSource(toRestEventStackSource(eventStackSource));
    }
  }
}
