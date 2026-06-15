package api.poja.io.model.pojaConf.conf6;

import static api.poja.io.model.PojaVersion.POJA_6;

import api.poja.io.endpoint.rest.model.EnvVar;
import api.poja.io.endpoint.rest.model.GeneralPojaConf6;
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
import lombok.Builder;

@JsonPropertyOrder(alphabetic = true)
@Builder(toBuilder = true)
public record PojaConf6(
    @JsonProperty("general") General general,
    @JsonProperty("integration") PojaConf1.Integration integration,
    @JsonProperty("gen_api_client") PojaConf1.GenApiClient genApiClient,
    @JsonProperty("concurrency") PojaConf2Concurrency concurrency,
    @JsonProperty("compute") PojaConf2.Compute compute,
    @JsonProperty("emailing") PojaConf1.MailingConf mailing,
    @JsonProperty("testing") PojaConf1.TestingConf testing,
    @JsonProperty("database") PojaConf2.Database database,
    @JsonProperty("networking") NetworkingConfig networking,
    @JsonProperty("scheduled_tasks") List<PojaConf2.ScheduledTask> scheduledTasks)
    implements PojaConf {

  @JsonGetter
  public String version() {
    return getVersion().toHumanReadableValue();
  }

  @Override
  public PojaVersion getVersion() {
    return POJA_6;
  }

  @Builder(toBuilder = true)
  public record General(
      @JsonProperty("app_name") String appName,
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
        GeneralPojaConf6 rest,
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

    public GeneralPojaConf6 toRest() {
      return new GeneralPojaConf6()
          .appName(appName)
          .withSnapstart(withSnapstart)
          .packageFullName(packageFullName)
          .customJavaDeps(customJavaDeps)
          .customJavaRepositories(customJavaRepositories)
          .customJavaEnvVars(customJavaEnvVars);
    }
  }
}
