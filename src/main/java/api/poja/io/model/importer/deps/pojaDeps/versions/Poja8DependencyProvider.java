package api.poja.io.model.importer.deps.pojaDeps.versions;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.Stream.concat;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.deps.pojaDeps.PojaDependencyProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Poja8DependencyProvider implements PojaDependencyProvider {
  private static final List<GradleDependency> DEFAULT_DEPENDENCIES =
      List.of(
          new GradleDependency(
              "implementation", "org.springframework.boot", "spring-boot-starter-web", "3.2.2"),
          new GradleDependency(
              "testImplementation", "org.testcontainers", "testcontainers", "2.0.2"),
          new GradleDependency(
              "implementation",
              "com.fasterxml.jackson.datatype",
              "jackson-datatype-jsr310",
              "2.15.3"),
          new GradleDependency(
              "implementation",
              "com.amazonaws.serverless",
              "aws-serverless-java-container-springboot3",
              "2.1.0"),
          new GradleDependency(
              "implementation", "software.amazon.awssdk", "aws-query-protocol", "2.20.26"),
          new GradleDependency("implementation", "com.amazonaws", "aws-lambda-java-core", "1.2.3"),
          new GradleDependency(
              "implementation", "com.amazonaws", "aws-lambda-java-events", "3.11.3"),
          new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.40"),
          new GradleDependency(
              "implementation", "software.amazon.awssdk", "eventbridge", "2.21.40"),
          new GradleDependency(
              "implementation", "software.amazon.awssdk.crt", "aws-crt", "0.28.12"),
          new GradleDependency("implementation", "software.amazon.awssdk", "ses", "2.21.40"),
          new GradleDependency("implementation", "software.amazon.awssdk", "core", "2.21.40"),
          new GradleDependency("implementation", "jakarta.mail", "jakarta.mail-api", "2.1.2"),
          new GradleDependency(
              "implementation", "jakarta.activation", "jakarta.activation-api", "2.1.2"),
          new GradleDependency("implementation", "com.sun.mail", "jakarta.mail", "2.0.1"),
          new GradleDependency(
              "implementation", "com.sun.activation", "jakarta.activation", "2.0.1"),
          new GradleDependency("implementation", "org.apache.tika", "tika-core", "2.9.1"),
          new GradleDependency("implementation", "org.reflections", "reflections", "0.10.2"),
          new GradleDependency("compileOnly", "org.projectlombok", "lombok", "1.18.30"),
          new GradleDependency("annotationProcessor", "org.projectlombok", "lombok", "1.18.30"),
          new GradleDependency("testCompileOnly", "org.projectlombok", "lombok", "1.18.30"),
          new GradleDependency("testAnnotationProcessor", "org.projectlombok", "lombok", "1.18.30"),
          new GradleDependency(
              "implementation", "org.openapitools", "jackson-databind-nullable", "0.2.6"),
          new GradleDependency("implementation", "io.swagger", "swagger-annotations", "1.6.12"),
          new GradleDependency(
              "testImplementation",
              "org.springframework.boot",
              "spring-boot-starter-test",
              "3.2.2"),
          new GradleDependency(
              "testImplementation", "org.testcontainers", "testcontainers-junit-jupiter", "2.0.2"),
          new GradleDependency(
              "testImplementation", "org.junit-pioneer", "junit-pioneer", "2.2.0"));

  private static final List<GradleDependency> ADDITIONAL_DEPENDENCIES =
      List.of(
          new GradleDependency("implementation", "software.amazon.awssdk", "s3", "2.21.40"),
          new GradleDependency(
              "implementation", "software.amazon.awssdk", "s3-transfer-manager", "2.21.40"),
          new GradleDependency("implementation", "io.sentry", "sentry-logback", "7.6.0"),
          new GradleDependency(
              "implementation", "io.sentry", "sentry-spring-boot-starter-jakarta", "7.4.0"),
          new GradleDependency(
              "implementation", "org.springdoc", "springdoc-openapi-starter-webmvc-ui", "2.5.0"),
          new GradleDependency(
              "implementation",
              "org.springframework.boot",
              "spring-boot-starter-data-jpa",
              "3.2.2"),
          new GradleDependency("implementation", "org.flywaydb", "flyway-core", "9.22.3"),
          new GradleDependency(
              "testImplementation", "org.testcontainers", "testcontainers-postgresql", "2.0.2"),
          new GradleDependency("implementation", "org.postgresql", "postgresql", "42.6.0"),
          new GradleDependency(
              "implementation",
              "org.hibernate.orm",
              "hibernate-community-dialects",
              "6.4.3.Final"));

  private static final Set<String> DEFAULT_DEPENCDENCY_KEYS =
      DEFAULT_DEPENDENCIES.stream().map(GradleDependency::key).collect(toUnmodifiableSet());

  private static final Map<String, GradleDependency> ADDITIONAL_DEPENDENCIES_MAP =
      ADDITIONAL_DEPENDENCIES.stream().collect(toMap(GradleDependency::key, dep -> dep));

  private static final Map<String, GradleDependency> ALL_DEPENDENCIES_MAP =
      concat(DEFAULT_DEPENDENCIES.stream(), ADDITIONAL_DEPENDENCIES.stream())
          .collect(toMap(GradleDependency::key, dep -> dep));

  @Override
  public List<GradleDependency> defaultDependencies() {
    return DEFAULT_DEPENDENCIES;
  }

  @Override
  public List<GradleDependency> additionalDependencies() {
    return ADDITIONAL_DEPENDENCIES;
  }

  @Override
  public List<GradleDependency> allDependencies() {
    return concat(DEFAULT_DEPENDENCIES.stream(), ADDITIONAL_DEPENDENCIES.stream()).toList();
  }

  @Override
  public Set<String> defaultDependencyKeys() {
    return DEFAULT_DEPENCDENCY_KEYS;
  }

  @Override
  public Map<String, GradleDependency> additionalDependenciesMap() {
    return ADDITIONAL_DEPENDENCIES_MAP;
  }

  @Override
  public Map<String, GradleDependency> allDependenciesMap() {
    return ALL_DEPENDENCIES_MAP;
  }
}
