package api.poja.io.model.pojaConf.conf8;

import static api.poja.io.endpoint.rest.model.EventStackSource._1;
import static api.poja.io.endpoint.rest.model.EventStackSource._2;
import static api.poja.io.model.PojaVersion.POJA_8;

import api.poja.io.endpoint.rest.model.EnvVar;
import api.poja.io.endpoint.rest.model.EventStackSource;
import api.poja.io.endpoint.rest.model.GeneralPojaConf7;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.NetworkingConfig;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf1.PojaConf1;
import api.poja.io.model.pojaConf.conf2.PojaConf2;
import api.poja.io.model.pojaConf.conf2.PojaConf2Concurrency;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;

@JsonPropertyOrder(alphabetic = true)
@Builder(toBuilder = true)
public record PojaConf8(
    @JsonProperty("general") General general,
    @JsonProperty("integration") PojaConf1.Integration integration,
    @JsonProperty("gen_api_client") PojaConf1.GenApiClient genApiClient,
    @JsonProperty("concurrency") PojaConf2Concurrency concurrency,
    @JsonProperty("compute") PojaConf2.Compute compute,
    @JsonProperty("emailing") PojaConf1.MailingConf mailing,
    @JsonProperty("testing") PojaConf1.TestingConf testing,
    @JsonProperty("database") PojaConf2.Database database,
    @JsonProperty("networking") NetworkingConfig networking,
    @JsonProperty("scheduled_tasks") List<ScheduledTask> scheduledTasks)
    implements PojaConf {

  @Override
  public PojaVersion getVersion() {
    return POJA_8;
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

  public record ScheduledTask(
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("class_name") String className,
      @JsonProperty("schedule_expression") String scheduleExpression,
      @JsonProperty("event_stack_source") Integer eventStackSource,
      @JsonProperty("cloud_name") String cloudName) {

    public static final String NON_ALPHANUMERIC_CHARACTERS = "[^a-zA-Z0-9]";

    @Builder
    public ScheduledTask(api.poja.io.endpoint.rest.model.ScheduledTask rest) {
      this(
          rest.getName(),
          rest.getDescription(),
          rest.getClassName(),
          rest.getScheduleExpression(),
          toDomainEventStackSource(rest.getEventStackSource()),
          null);
    }

    public ScheduledTask(
        api.poja.io.endpoint.rest.model.ScheduledTask rest, @Nonnull String appId) {
      this(
          rest.getName(),
          rest.getDescription(),
          rest.getClassName(),
          rest.getScheduleExpression(),
          toDomainEventStackSource(rest.getEventStackSource()),
          (rest.getName() + appId.substring(0, 8)).replaceAll(NON_ALPHANUMERIC_CHARACTERS, ""));
    }

    public ScheduledTask useCloudName() {
      var cloudName = this.cloudName == null || this.cloudName.isEmpty() ? name : this.cloudName;
      return new ScheduledTask(
          cloudName, description, className, scheduleExpression, eventStackSource, cloudName);
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
