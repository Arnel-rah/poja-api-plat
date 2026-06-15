package api.poja.io.model.pojaConf.conf9;

import static api.poja.io.model.PojaVersion.POJA_9;
import static api.poja.io.model.pojaConf.conf8.PojaConf8.ScheduledTask.NON_ALPHANUMERIC_CHARACTERS;
import static java.util.Objects.requireNonNull;

import api.poja.io.endpoint.rest.model.ComputeConf9;
import api.poja.io.endpoint.rest.model.EnvVar;
import api.poja.io.endpoint.rest.model.GeneralPojaConf7;
import api.poja.io.endpoint.rest.model.ScheduledTask9;
import api.poja.io.endpoint.rest.model.Worker;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.NetworkingConfig;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;

@JsonPropertyOrder(alphabetic = true)
@Builder(toBuilder = true)
public record PojaConf9(
    @JsonProperty("general") General general,
    @JsonProperty("integration") PojaConf1.Integration integration,
    @JsonProperty("gen_api_client") PojaConf1.GenApiClient genApiClient,
    @JsonProperty("concurrency") PojaConf9Concurrency concurrency,
    @JsonProperty("compute") PojaConf9.Compute compute,
    @JsonProperty("emailing") PojaConf1.MailingConf mailing,
    @JsonProperty("testing") PojaConf1.TestingConf testing,
    @JsonProperty("database") PojaConf2.Database database,
    @JsonProperty("networking") NetworkingConfig networking,
    @JsonProperty("scheduled_tasks") List<PojaConf9.ScheduledTask> scheduledTasks)
    implements PojaConf {

  @Override
  public PojaVersion getVersion() {
    return POJA_9;
  }

  @JsonGetter
  public String version() {
    return getVersion().toHumanReadableValue();
  }

  @Builder(toBuilder = true)
  public record General(
      @JsonProperty("app_name") String appName,
      @JsonProperty("java_main_class") String javaMainClass,
      @JsonProperty("with_snapstart") boolean withSnapstart,
      @JsonProperty("package_full_name") String packageFullName,
      @JsonProperty("custom_java_repositories") List<String> customJavaRepositories,
      @JsonProperty("custom_java_deps") List<String> customJavaDeps,
      @JsonProperty("custom_java_env_vars") List<EnvVar> customJavaEnvVars,
      @JsonProperty("poja_python_repository_name") String pojaPythonRepositoryName,
      @JsonProperty("poja_python_repository_domain") String pojaPythonRepositoryDomain,
      @JsonProperty("poja_domain_owner") String pojaDomainOwner,
      @JsonProperty(JSON_PROPERTY_PUBLIC_GENERATOR_VERSION) String publicGeneratorVersion) {
    public static final String JSON_PROPERTY_PUBLIC_GENERATOR_VERSION = "public_generator_version";

    @Builder
    public General(
        GeneralPojaConf7 rest,
        String pojaPythonRepositoryName,
        String pojaPythonRepositoryDomain,
        String pojaDomainOwner,
        String publicGeneratorVersion) {
      this(
          rest.getAppName(),
          rest.getJavaMainClass(),
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

    public GeneralPojaConf7 toRest() {
      return new GeneralPojaConf7()
          .appName(appName)
          .javaMainClass(javaMainClass)
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
          PojaConf9.Compute.FrontalFunctionInvocationMethodEnum frontalFunctionInvocationMethod,
      @JsonProperty("api_gateway_timeout") BigDecimal apiGatewayTimeout,
      @JsonProperty("workers") List<Worker> workers) {

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
      public static PojaConf9.Compute.FrontalFunctionInvocationMethodEnum fromString(String value) {
        for (PojaConf9.Compute.FrontalFunctionInvocationMethodEnum b :
            PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.values()) {
          if (b.value.equals(value)) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
      }

      public ComputeConf9.FrontalFunctionInvocationMethodEnum toRest() {
        return switch (this) {
          case HTTP_API -> ComputeConf9.FrontalFunctionInvocationMethodEnum.HTTP_API;
          case LAMBDA_URL -> ComputeConf9.FrontalFunctionInvocationMethodEnum.LAMBDA_URL;
        };
      }

      public static PojaConf9.Compute.FrontalFunctionInvocationMethodEnum fromRest(
          ComputeConf9.FrontalFunctionInvocationMethodEnum rest) {
        return PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.valueOf(rest.getValue());
      }
    }

    @Builder
    public Compute(ComputeConf9 rest) {
      this(
          rest.getFrontalMemory(),
          rest.getFrontalFunctionTimeout(),
          PojaConf9.Compute.FrontalFunctionInvocationMethodEnum.fromRest(
              requireNonNull(rest.getFrontalFunctionInvocationMethod())),
          rest.getApiGatewayTimeout(),
          rest.getWorkers());
    }

    public ComputeConf9 toRest() {
      return new ComputeConf9()
          .frontalMemory(frontalMemory)
          .frontalFunctionTimeout(frontalFunctionTimeout)
          .frontalFunctionInvocationMethod(frontalFunctionInvocationMethod.toRest())
          .apiGatewayTimeout(apiGatewayTimeout)
          .workers(workers);
    }
  }

  @Builder(toBuilder = true)
  public record ScheduledTask(
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("class_name") String className,
      @JsonProperty("schedule_expression") String scheduleExpression,
      @JsonProperty("event_stack_source") Integer eventStackSource,
      @JsonProperty("cloud_name") String cloudName) {

    @Builder
    public ScheduledTask(ScheduledTask9 rest) {
      this(
          rest.getName(),
          rest.getDescription(),
          rest.getClassName(),
          rest.getScheduleExpression(),
          rest.getEventStackSource(),
          null);
    }

    public ScheduledTask(ScheduledTask9 rest, @Nonnull String appId) {
      this(
          rest.getName(),
          rest.getDescription(),
          rest.getClassName(),
          rest.getScheduleExpression(),
          rest.getEventStackSource(),
          (rest.getName() + appId.substring(0, 8)).replaceAll(NON_ALPHANUMERIC_CHARACTERS, ""));
    }

    public PojaConf9.ScheduledTask useCloudName() {
      var cloudName = this.cloudName == null || this.cloudName.isEmpty() ? name : this.cloudName;
      return new PojaConf9.ScheduledTask(
          cloudName, description, className, scheduleExpression, eventStackSource, cloudName);
    }

    public ScheduledTask9 toRest() {
      return new ScheduledTask9()
          .name(name)
          .description(description)
          .className(className)
          .scheduleExpression(scheduleExpression)
          .eventStackSource(eventStackSource);
    }
  }
}
