package api.poja.io.model.pojaConf.conf1;

import static api.poja.io.model.PojaVersion.POJA_1;

import api.poja.io.endpoint.rest.model.ComputeConf;
import api.poja.io.endpoint.rest.model.DatabaseConf;
import api.poja.io.endpoint.rest.model.GenApiClientConf;
import api.poja.io.endpoint.rest.model.GeneralPojaConf;
import api.poja.io.endpoint.rest.model.IntegrationConf;
import api.poja.io.endpoint.rest.model.WithQueuesNbEnum;
import api.poja.io.model.PojaVersion;
import api.poja.io.model.pojaConf.NetworkingConfig;
import api.poja.io.model.pojaConf.PojaConf;
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
public record PojaConf1(
    @JsonProperty("general") General general,
    @JsonProperty("integration") Integration integration,
    @JsonProperty("gen_api_client") GenApiClient genApiClient,
    @JsonProperty("concurrency") PojaConf1Concurrency concurrency,
    @JsonProperty("compute") Compute compute,
    @JsonProperty("emailing") MailingConf mailing,
    @JsonProperty("testing") TestingConf testing,
    @JsonProperty("database") Database database,
    @JsonProperty("networking") NetworkingConfig networking)
    implements PojaConf {

  @JsonGetter
  public String version() {
    return getVersion().toHumanReadableValue();
  }

  @Override
  public PojaVersion getVersion() {
    return POJA_1;
  }

  public record General(
      @JsonProperty("app_name") String appName,
      @JsonProperty("with_snapstart") Boolean withSnapstart,
      @JsonProperty("with_queues_nb") WithQueuesNbEnum withQueuesNb,
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
        GeneralPojaConf rest,
        String pojaPythonRepositoryName,
        String pojaPythonRepositoryDomain,
        String pojaDomainOwner,
        String publicGeneratorVersion) {
      this(
          rest.getAppName(),
          rest.getWithSnapstart(),
          rest.getWithQueuesNb(),
          rest.getPackageFullName(),
          rest.getCustomJavaRepositories(),
          rest.getCustomJavaDeps(),
          rest.getCustomJavaEnvVars(),
          pojaPythonRepositoryName,
          pojaPythonRepositoryDomain,
          pojaDomainOwner,
          publicGeneratorVersion);
    }

    public GeneralPojaConf toRest() {
      return new GeneralPojaConf()
          .appName(appName)
          .withSnapstart(withSnapstart)
          .withQueuesNb(withQueuesNb)
          .packageFullName(packageFullName)
          .customJavaDeps(customJavaDeps)
          .customJavaRepositories(customJavaRepositories)
          .customJavaEnvVars(customJavaEnvVars);
    }
  }

  public record GenApiClient(
      @JsonProperty("aws_account_id") String awsAccountId,
      @JsonProperty("with_publish_to_npm_registry") Boolean withPublishToNpmRegistry,
      @JsonProperty("ts_client_default_openapi_server_url") String tsClientDefaultOpenapiServerUrl,
      @JsonProperty("ts_client_api_url_env_var_name") String tsClientApiUrlEnvVarName,
      @JsonProperty("codeartifact_repository_name") String codeartifactRepositoryName,
      @JsonProperty("codeartifact_domain_name") String codeartifactDomainName) {
    @Builder
    public GenApiClient(GenApiClientConf rest) {
      this(
          rest.getAwsAccountId(),
          rest.getWithPublishToNpmRegistry(),
          rest.getTsClientDefaultOpenapiServerUrl(),
          rest.getTsClientApiUrlEnvVarName(),
          rest.getCodeartifactRepositoryName(),
          rest.getCodeartifactDomainName());
    }

    public GenApiClientConf toRest() {
      return new GenApiClientConf()
          .awsAccountId(awsAccountId)
          .withPublishToNpmRegistry(withPublishToNpmRegistry)
          .tsClientApiUrlEnvVarName(tsClientApiUrlEnvVarName)
          .tsClientDefaultOpenapiServerUrl(tsClientDefaultOpenapiServerUrl)
          .codeartifactRepositoryName(codeartifactRepositoryName)
          .codeartifactDomainName(codeartifactDomainName);
    }
  }

  public record Integration(
      @JsonProperty("with_sentry") Boolean withSentry,
      @JsonProperty("with_sonar") Boolean withSonar,
      @JsonProperty("with_codeql") Boolean withCodeql,
      @JsonProperty("with_file_storage") Boolean withFileStorage,
      @JsonProperty("with_swagger_ui") Boolean withSwaggerUi) {
    @Builder
    public Integration(IntegrationConf rest) {
      this(
          rest.getWithSentry(),
          rest.getWithSonar(),
          rest.getWithCodeql(),
          rest.getWithFileStorage(),
          rest.getWithSwaggerUi());
    }

    public IntegrationConf toRest() {
      return new IntegrationConf()
          .withSentry(withSentry)
          .withSonar(withSonar)
          .withCodeql(withCodeql)
          .withFileStorage(withFileStorage)
          .withSwaggerUi(withSwaggerUi);
    }
  }

  public record TestingConf(
      @JsonProperty("java_facade_it") String javaFacadeIt,
      @JsonProperty("jacoco_min_coverage") BigDecimal jacocoMinCoverage) {
    @Builder
    public TestingConf(api.poja.io.endpoint.rest.model.TestingConf rest) {
      this(rest.getJavaFacadeIt(), rest.getJacocoMinCoverage());
    }

    public api.poja.io.endpoint.rest.model.TestingConf toRest() {
      return new api.poja.io.endpoint.rest.model.TestingConf()
          .jacocoMinCoverage(jacocoMinCoverage)
          .javaFacadeIt(javaFacadeIt);
    }
  }

  public record MailingConf(@JsonProperty("ses_source") String sesSource) {
    @Builder
    public MailingConf(api.poja.io.endpoint.rest.model.MailingConf rest) {
      this(rest.getSesSource());
    }

    public api.poja.io.endpoint.rest.model.MailingConf toRest() {
      return new api.poja.io.endpoint.rest.model.MailingConf().sesSource(sesSource);
    }
  }

  public record Compute(
      @JsonProperty("frontal_memory") BigDecimal frontalMemory,
      @JsonProperty("frontal_function_timeout") BigDecimal frontalFunctionTimeout,
      @JsonProperty("worker_memory") BigDecimal workerMemory,
      @JsonProperty("worker_batch") BigDecimal workerBatch,
      @JsonProperty("worker_function_1_timeout") BigDecimal workerFunction1Timeout,
      @JsonProperty("api_gateway_timeout") BigDecimal apiGatewayTimeout,
      @JsonProperty("worker_function_2_timeout") BigDecimal workerFunction2Timeout) {
    @Builder
    public Compute(ComputeConf rest) {
      this(
          rest.getFrontalMemory(),
          rest.getFrontalFunctionTimeout(),
          rest.getWorkerMemory(),
          rest.getWorkerBatch(),
          rest.getWorkerFunction1Timeout(),
          rest.getApiGatewayTimeout(),
          rest.getWorkerFunction2Timeout());
    }

    public ComputeConf toRest() {
      return new ComputeConf()
          .frontalMemory(frontalMemory)
          .frontalFunctionTimeout(frontalFunctionTimeout)
          .workerMemory(workerMemory)
          .workerBatch(workerBatch)
          .workerFunction1Timeout(workerFunction1Timeout)
          .apiGatewayTimeout(apiGatewayTimeout)
          .workerFunction2Timeout(workerFunction2Timeout);
    }
  }

  public record Database(
      @JsonProperty("with_database") PojaConf1.Database.WithDatabaseEnum dbType,
      @JsonProperty("database_non_root_username") String dbNonRootUsername,
      @JsonProperty("database_non_root_password") String dbNonrootPassword,
      @JsonProperty("prod_db_cluster_timeout") BigDecimal prodDbClusterTimeout,
      @JsonProperty("aurora_min_capacity") BigDecimal auroraMinCapacity,
      @JsonProperty("aurora_max_capacity") BigDecimal auroraMaxCapacity,
      @JsonProperty("aurora_scale_point") BigDecimal auroraScalePoint,
      @JsonProperty("aurora_sleep") BigDecimal auroraSleep,
      @JsonProperty("aurora_auto_pause") Boolean auroraAutoPause) {
    @Builder
    public Database(DatabaseConf rest) {
      this(
          PojaConf1.Database.WithDatabaseEnum.fromRest(rest.getWithDatabase()),
          rest.getDatabaseNonRootUsername(),
          rest.getDatabaseNonRootPassword(),
          rest.getProdDbClusterTimeout(),
          rest.getAuroraMinCapacity(),
          rest.getAuroraMaxCapacity(),
          rest.getAuroraScalePoint(),
          rest.getAuroraSleep(),
          rest.getAuroraAutoPause());
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
      public static PojaConf1.Database.WithDatabaseEnum fromValue(String value) {
        for (PojaConf1.Database.WithDatabaseEnum b : PojaConf1.Database.WithDatabaseEnum.values()) {
          if (b.value.equals(value)) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
      }

      public static PojaConf1.Database.WithDatabaseEnum fromRest(
          DatabaseConf.WithDatabaseEnum rest) {
        for (PojaConf1.Database.WithDatabaseEnum b : PojaConf1.Database.WithDatabaseEnum.values()) {
          if (b.value.equals(rest.getValue())) {
            return b;
          }
        }
        throw new IllegalArgumentException("Unexpected value '" + rest.getValue() + "'");
      }

      public static DatabaseConf.WithDatabaseEnum toRest(WithDatabaseEnum domain) {
        if (NON_POJA_MANAGED_POSTGRES.equals(domain)) {
          return DatabaseConf.WithDatabaseEnum.SELF_MANAGED_JPA_DATABASE;
        }
        return DatabaseConf.WithDatabaseEnum.fromValue(domain.getValue());
      }
    }

    public DatabaseConf toRest() {
      return new DatabaseConf()
          .withDatabase(PojaConf1.Database.WithDatabaseEnum.toRest(dbType))
          .databaseNonRootUsername(dbNonRootUsername)
          .databaseNonRootPassword(dbNonrootPassword)
          .prodDbClusterTimeout(prodDbClusterTimeout)
          .auroraMinCapacity(auroraMinCapacity)
          .auroraMaxCapacity(auroraMinCapacity)
          .auroraScalePoint(auroraScalePoint)
          .auroraSleep(auroraSleep)
          .auroraAutoPause(auroraAutoPause);
    }
  }
}
