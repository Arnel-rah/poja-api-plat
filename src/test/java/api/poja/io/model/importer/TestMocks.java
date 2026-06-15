package api.poja.io.model.importer;

import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository.DEFAULT_MAVEN_CENTRAL_REPO_NAME;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository.GOOGLE_REPO_NAME;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository.GOOGLE_URL;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository.GRADLE_PLUGIN_PORTAL_REPO_NAME;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository.GRADLE_PLUGIN_PORTAL_URL;
import static api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository.MAVEN_CENTRAL_URL;

import api.poja.io.mail.EmailConf;
import api.poja.io.model.EnvVar;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleSettings;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.JavaConfig;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.GradleRepository;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository.MavenRepository;
import api.poja.io.model.pojaConf.NetworkingConfig;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.regions.Region;

public class TestMocks {
  public static final String REGION = "us-east-1";
  public static final String SES_SOURCE = "noreply@mail.test.poja.io";
  public static final EmailConf EMAIL_CONF = new EmailConf(SES_SOURCE, Region.of(REGION));

  public static final String REAL_WORLD_GRADLE_PROJECT_PATH = "real_world_gradle";
  public static final String EMPTY_GRADLE_PROJECT_PATH = "empty_gradle";
  public static final String EMPTY_GRADLE_DEPS_PROJECT_PATH = "empty_gradle_deps";
  public static final String JAVA_PLUGIN_PROJECT_DIR = "java_plugin";
  public static final String INCORRECT_GRADLE_PROJECT_DIR = "incorrect";
  // TODO: test object eq
  public static final String[] REAL_WORLD_GRADLE_DEPS = {
    "implementation 'org.springframework.boot:spring-boot-starter-web'",
    "implementation 'org.springframework.boot:spring-boot-starter-data-jpa'",
    "implementation 'org.flywaydb:flyway-core'",
    "testImplementation 'org.testcontainers:postgresql:1.20.0'",
    "implementation 'org.postgresql:postgresql'",
    "implementation 'org.hibernate.orm:hibernate-community-dialects:6.4.3.Final'",
    "implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'",
    "implementation 'com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.0'",
    "implementation 'software.amazon.awssdk:aws-query-protocol:2.20.26'",
    "implementation 'com.amazonaws:aws-lambda-java-core:1.2.3'",
    "implementation 'com.amazonaws:aws-lambda-java-events:3.11.3'",
    "implementation 'software.amazon.awssdk:sqs:2.21.40'",
    "implementation 'software.amazon.awssdk:eventbridge:2.21.40'",
    "implementation 'software.amazon.awssdk:s3:2.21.40'",
    "implementation 'software.amazon.awssdk:s3-transfer-manager:2.21.40'",
    "implementation 'software.amazon.awssdk.crt:aws-crt:0.28.12'",
    "implementation 'software.amazon.awssdk:ses:2.21.40'",
    "implementation 'software.amazon.awssdk:core:2.21.40'",
    "implementation 'jakarta.mail:jakarta.mail-api:2.1.2'",
    "implementation 'jakarta.activation:jakarta.activation-api:2.1.2'",
    "implementation 'com.sun.mail:jakarta.mail:2.0.1'",
    "implementation 'com.sun.activation:jakarta.activation:2.0.1'",
    "implementation 'org.apache.tika:tika-core:2.9.1'",
    "implementation 'org.reflections:reflections:0.10.2'",
    "compileOnly 'org.projectlombok:lombok'",
    "annotationProcessor 'org.projectlombok:lombok'",
    "testCompileOnly 'org.projectlombok:lombok'",
    "testAnnotationProcessor 'org.projectlombok:lombok'",
    "implementation 'org.openapitools:jackson-databind-nullable:0.2.6'",
    "implementation 'io.swagger:swagger-annotations:1.6.12'",
    "testImplementation 'org.springframework.boot:spring-boot-starter-test'",
    "testImplementation 'org.testcontainers:junit-jupiter:1.19.1'",
    "testImplementation 'org.junit-pioneer:junit-pioneer:2.2.0'",
    "implementation 'io.sentry:sentry-logback:7.6.0'",
    "implementation 'io.sentry:sentry-spring-boot-starter-jakarta:7.4.0'",
    "implementation 'api.poja:poja-api-gen:latest'",
    "implementation 'org.springframework.boot:spring-boot-starter-validation'",
    "implementation 'org.springframework.boot:spring-boot-starter-security'",
    "implementation 'io.hypersistence:hypersistence-utils-hibernate-63:3.7.3'",
    "implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'",
    "implementation 'org.kohsuke:github-api:1.321'",
    "implementation 'software.amazon.awssdk:cloudformation:2.21.40'",
    "implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2'",
    "implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.2'",
    "implementation 'com.fasterxml.jackson.core:jackson-core:2.17.2'",
    "implementation 'com.fasterxml.jackson.core:jackson-annotations:2.17.2'",
    "implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'",
    "implementation 'org.bouncycastle:bcprov-jdk18on:1.78.1'",
    "implementation 'org.bouncycastle:bcpkix-jdk18on:1.78.1'",
    "implementation 'io.jsonwebtoken:jjwt-api:0.12.6'",
    "implementation 'io.jsonwebtoken:jjwt-jackson:0.12.6'",
    "implementation 'io.jsonwebtoken:jjwt-impl:0.12.6'",
    "implementation 'org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r'",
    "implementation 'software.amazon.awssdk:ssm:2.21.40'",
    "implementation 'com.stripe:stripe-java:26.3.0'",
    "implementation 'software.amazon.awssdk:iam-policy-builder:2.21.40'",
    "implementation 'software.amazon.awssdk:iam:2.21.40'",
    "implementation 'software.amazon.awssdk:cloudwatchlogs:2.21.40'",
    "implementation 'software.amazon.awssdk:lambda:2.21.40'",
    "implementation 'org.apache.commons:commons-text:1.13.0'",
    "implementation 'org.matheclipse:matheclipse-core:3.0.0'",
    "implementation 'com.goterl:lazysodium-java:5.2.0'",
    "implementation 'org.gradle:gradle-tooling-api:8.12'"
  };
  public static final String[] SIMPLE_GRADLE_DEPS_FROM_MVN = {
    "implementation 'org.springframework.boot:spring-boot-starter-web'",
    "implementation 'org.springframework.boot:spring-boot-starter-data-jpa'",
    "implementation 'org.flywaydb:flyway-core'",
    "testImplementation 'org.testcontainers:postgresql:1.20.0'",
    "implementation 'org.postgresql:postgresql'",
    "implementation 'org.hibernate.orm:hibernate-community-dialects:6.4.3.Final'",
    "implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'",
    "implementation 'com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.0'",
    "implementation 'software.amazon.awssdk:aws-query-protocol:2.20.26'",
    "implementation 'com.amazonaws:aws-lambda-java-core:1.2.3'",
    "implementation 'com.amazonaws:aws-lambda-java-events:3.11.3'",
    "implementation 'software.amazon.awssdk:sqs:2.21.40'",
    "implementation 'software.amazon.awssdk:eventbridge:2.21.40'",
    "implementation 'software.amazon.awssdk:s3:2.21.40'",
    "implementation 'software.amazon.awssdk:s3-transfer-manager:2.21.40'",
    "implementation 'software.amazon.awssdk.crt:aws-crt:0.28.12'",
    "implementation 'software.amazon.awssdk:ses:2.21.40'",
    "implementation 'software.amazon.awssdk:core:2.21.40'",
    "implementation 'jakarta.mail:jakarta.mail-api:2.1.2'",
    "implementation 'jakarta.activation:jakarta.activation-api:2.1.2'",
    "implementation 'com.sun.mail:jakarta.mail:2.0.1'",
    "implementation 'com.sun.activation:jakarta.activation:2.0.1'",
    "implementation 'org.apache.tika:tika-core:2.9.1'",
    "implementation 'org.reflections:reflections:0.10.2'",
    "implementation 'org.openapitools:jackson-databind-nullable:0.2.6'",
    "implementation 'io.swagger:swagger-annotations:1.6.12'",
    "testImplementation 'org.springframework.boot:spring-boot-starter-test'",
    "testImplementation 'org.testcontainers:junit-jupiter:1.19.1'",
    "testImplementation 'org.junit-pioneer:junit-pioneer:2.2.0'",
    "implementation 'io.sentry:sentry-logback:7.6.0'",
    "implementation 'io.sentry:sentry-spring-boot-starter-jakarta:7.4.0'",
    "implementation 'api.poja:poja-api-gen:latest'",
    "implementation 'org.springframework.boot:spring-boot-starter-validation'",
    "implementation 'org.springframework.boot:spring-boot-starter-security'",
    "implementation 'io.hypersistence:hypersistence-utils-hibernate-63:3.7.3'",
    "implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'",
    "implementation 'org.kohsuke:github-api:1.321'",
    "implementation 'software.amazon.awssdk:cloudformation:2.21.40'",
    "implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2'",
    "implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.2'",
    "implementation 'com.fasterxml.jackson.core:jackson-core:2.17.2'",
    "implementation 'com.fasterxml.jackson.core:jackson-annotations:2.17.2'",
    "implementation 'org.apache.httpcomponents.client5:httpclient5:5.3.1'",
    "implementation 'org.bouncycastle:bcprov-jdk18on:1.78.1'",
    "implementation 'org.bouncycastle:bcpkix-jdk18on:1.78.1'",
    "implementation 'io.jsonwebtoken:jjwt-api:0.12.6'",
    "implementation 'io.jsonwebtoken:jjwt-jackson:0.12.6'",
    "implementation 'io.jsonwebtoken:jjwt-impl:0.12.6'",
    "implementation 'org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r'",
    "implementation 'software.amazon.awssdk:ssm:2.21.40'",
    "implementation 'com.stripe:stripe-java:26.3.0'",
    "implementation 'software.amazon.awssdk:iam-policy-builder:2.21.40'",
    "implementation 'software.amazon.awssdk:iam:2.21.40'",
    "implementation 'software.amazon.awssdk:cloudwatchlogs:2.21.40'",
    "implementation 'software.amazon.awssdk:lambda:2.21.40'",
    "implementation 'org.apache.commons:commons-text:1.13.0'",
    "implementation 'org.matheclipse:matheclipse-core:3.0.0'",
    "implementation 'com.goterl:lazysodium-java:5.2.0'",
    "implementation 'org.gradle:gradle-tooling-api:8.12'"
  };
  public static final String[] ANNOTATION_PROCESSOR_DEPS_FROM_MVN = {
    "compileOnly 'org.projectlombok:lombok'",
    "annotationProcessor 'org.projectlombok:lombok'",
    "testCompileOnly 'org.projectlombok:lombok'",
    "testAnnotationProcessor 'org.projectlombok:lombok'"
  };

  public static final String[] EMPTY_GRADLE_DEPS_PLUGINS = {"id 'java'"};
  public static final String[] REAL_WORLD_GRADLE_PLUGINS = {
    "id 'java'",
    "id 'org.springframework.boot' version '3.2.2'",
    "id 'io.spring.dependency-management' version '1.1.3'",
    "id 'org.openapi.generator' version '7.7.0'",
    "id 'jacoco'"
  };
  public static final String[] REAL_WORLD_GRADLE_REPOSITORIES = {
    "mavenLocal()",
    "mavenCentral()",
    "google()",
    "gradlePluginPortal()",
    """
    maven {
    url = "https://repo.gradle.org/gradle/libs-releases/"
    }""",
    """
    maven {
    name = "CustomRepo"
    url = "https://gradle.dl.poja.io"
    }""",
    """
    flatDir {
    dirs 'lib'
    }""",
    """
    flatDir {
    dirs 'lib1', 'lib2'
    }"""
  };
  public static final String[] REPOSITORIES_FROM_MVN = {
    "mavenCentral()",
    "google()",
    "gradlePluginPortal()",
    """
    maven {
    name = "gradle libs releases"
    url = "https://repo.gradle.org/gradle/libs-releases/"
    }""",
    """
    maven {
    name = "CustomRepo"
    url = "https://my.custom.repo/dl/"
    }"""
  };
  public static final String[] UNHANDLED_MAVEN_TAG_PATHS = {
    "project/packaging",
    "project/version",
    "project/artifactId",
    "project/modelVersion",
    "project/repositories/repository/id",
    "project/build/plugins/plugin/configuration/ignoredPaths/path/groupId",
    "project/build/plugins/plugin/configuration/ignoredPaths/unknownPath/groupId",
    "project/build/plugins/plugin/configuration/ignoredPaths/path/artifactId",
    "project/build/plugins/plugin/configuration/ignoredPaths/unknownPath/artifactId",
  };
  public static final GradleRepository[] STRUCTURED_REPOSITORIES_FROM_MVN = {
    new MavenRepository(DEFAULT_MAVEN_CENTRAL_REPO_NAME, MAVEN_CENTRAL_URL),
    new MavenRepository(GOOGLE_REPO_NAME, GOOGLE_URL),
    new MavenRepository(GRADLE_PLUGIN_PORTAL_REPO_NAME, GRADLE_PLUGIN_PORTAL_URL),
    new MavenRepository("gradle libs releases", "https://repo.gradle.org/gradle/libs-releases/"),
    new MavenRepository("CustomRepo", "https://my.custom.repo/dl/")
  };
  public static final GradleDependency[] REAL_WORLD_STRUCTURED_GRADLE_DEPENDENCIES = {
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-web", null),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-data-jpa", null),
    new GradleDependency("implementation", "org.flywaydb", "flyway-core", null),
    new GradleDependency("testImplementation", "org.testcontainers", "postgresql", "1.20.0"),
    new GradleDependency("implementation", "org.postgresql", "postgresql", null),
    new GradleDependency(
        "implementation", "org.hibernate.orm", "hibernate-community-dialects", "6.4.3.Final"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", null),
    new GradleDependency(
        "implementation",
        "com.amazonaws.serverless",
        "aws-serverless-java-container-springboot3",
        "2.1.0"),
    new GradleDependency(
        "implementation", "software.amazon.awssdk", "aws-query-protocol", "2.20.26"),
    new GradleDependency("implementation", "com.amazonaws", "aws-lambda-java-core", "1.2.3"),
    new GradleDependency("implementation", "com.amazonaws", "aws-lambda-java-events", "3.11.3"),
    new GradleDependency("implementation", "software.amazon.awssdk", "sqs", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "eventbridge", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "s3", "2.21.40"),
    new GradleDependency(
        "implementation", "software.amazon.awssdk", "s3-transfer-manager", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk.crt", "aws-crt", "0.28.12"),
    new GradleDependency("implementation", "software.amazon.awssdk", "ses", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "core", "2.21.40"),
    new GradleDependency("implementation", "jakarta.mail", "jakarta.mail-api", "2.1.2"),
    new GradleDependency("implementation", "jakarta.activation", "jakarta.activation-api", "2.1.2"),
    new GradleDependency("implementation", "com.sun.mail", "jakarta.mail", "2.0.1"),
    new GradleDependency("implementation", "com.sun.activation", "jakarta.activation", "2.0.1"),
    new GradleDependency("implementation", "org.apache.tika", "tika-core", "2.9.1"),
    new GradleDependency("implementation", "org.reflections", "reflections", "0.10.2"),
    new GradleDependency(
        "implementation", "org.openapitools", "jackson-databind-nullable", "0.2.6"),
    new GradleDependency("implementation", "io.swagger", "swagger-annotations", "1.6.12"),
    new GradleDependency(
        "testImplementation", "org.springframework.boot", "spring-boot-starter-test", null),
    new GradleDependency("testImplementation", "org.testcontainers", "junit-jupiter", "1.19.1"),
    new GradleDependency("testImplementation", "org.junit-pioneer", "junit-pioneer", "2.2.0"),
    new GradleDependency("implementation", "io.sentry", "sentry-logback", "7.6.0"),
    new GradleDependency(
        "implementation", "io.sentry", "sentry-spring-boot-starter-jakarta", "7.4.0"),
    new GradleDependency("implementation", "api.poja", "poja-api-gen", "latest"),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-validation", null),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-security", null),
    new GradleDependency(
        "implementation", "io.hypersistence", "hypersistence-utils-hibernate-63", "3.7.3"),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-oauth2-client", null),
    new GradleDependency("implementation", "org.kohsuke", "github-api", "1.321"),
    new GradleDependency("implementation", "software.amazon.awssdk", "cloudformation", "2.21.40"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.17.2"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.core", "jackson-databind", "2.17.2"),
    new GradleDependency("implementation", "com.fasterxml.jackson.core", "jackson-core", "2.17.2"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.core", "jackson-annotations", "2.17.2"),
    new GradleDependency(
        "implementation", "org.apache.httpcomponents.client5", "httpclient5", "5.3.1"),
    new GradleDependency("implementation", "org.bouncycastle", "bcprov-jdk18on", "1.78.1"),
    new GradleDependency("implementation", "org.bouncycastle", "bcpkix-jdk18on", "1.78.1"),
    new GradleDependency("implementation", "io.jsonwebtoken", "jjwt-api", "0.12.6"),
    new GradleDependency("implementation", "io.jsonwebtoken", "jjwt-jackson", "0.12.6"),
    new GradleDependency("implementation", "io.jsonwebtoken", "jjwt-impl", "0.12.6"),
    new GradleDependency(
        "implementation", "org.eclipse.jgit", "org.eclipse.jgit", "6.10.0.202406032230-r"),
    new GradleDependency("implementation", "software.amazon.awssdk", "ssm", "2.21.40"),
    new GradleDependency("implementation", "com.stripe", "stripe-java", "26.3.0"),
    new GradleDependency(
        "implementation", "software.amazon.awssdk", "iam-policy-builder", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "iam", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "cloudwatchlogs", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "lambda", "2.21.40"),
    new GradleDependency("implementation", "org.apache.commons", "commons-text", "1.13.0"),
    new GradleDependency("implementation", "org.matheclipse", "matheclipse-core", "3.0.0"),
    new GradleDependency("implementation", "com.goterl", "lazysodium-java", "5.2.0"),
    new GradleDependency("implementation", "org.gradle", "gradle-tooling-api", "8.12"),
    new GradleDependency("compileOnly", "org.projectlombok", "lombok", null),
    new GradleDependency("annotationProcessor", "org.projectlombok", "lombok", null),
    new GradleDependency("testAnnotationProcessor", "org.projectlombok", "lombok", null),
    new GradleDependency("testCompileOnly", "org.projectlombok", "lombok", null),
  };
  public static final GradleDependency[] REAL_WORLD_RESOLVED_GRADLE_DEPENDENCIES = {
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-data-jpa", "3.2.2"),
    new GradleDependency("implementation", "org.flywaydb", "flyway-core", "9.22.3"),
    new GradleDependency("testImplementation", "org.testcontainers", "postgresql", "1.20.0"),
    new GradleDependency("implementation", "org.postgresql", "postgresql", "42.6.0"),
    new GradleDependency(
        "implementation", "org.hibernate.orm", "hibernate-community-dialects", "6.4.3.Final"),
    new GradleDependency("implementation", "software.amazon.awssdk", "s3", "2.21.40"),
    new GradleDependency(
        "implementation", "software.amazon.awssdk", "s3-transfer-manager", "2.21.40"),
    new GradleDependency("implementation", "io.sentry", "sentry-logback", "7.6.0"),
    new GradleDependency(
        "implementation", "io.sentry", "sentry-spring-boot-starter-jakarta", "7.4.0"),
    new GradleDependency("implementation", "api.poja", "poja-api-gen", "latest"),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-validation", null),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-security", null),
    new GradleDependency(
        "implementation", "io.hypersistence", "hypersistence-utils-hibernate-63", "3.7.3"),
    new GradleDependency(
        "implementation", "org.springframework.boot", "spring-boot-starter-oauth2-client", null),
    new GradleDependency("implementation", "org.kohsuke", "github-api", "1.321"),
    new GradleDependency("implementation", "software.amazon.awssdk", "cloudformation", "2.21.40"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.17.2"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.core", "jackson-databind", "2.17.2"),
    new GradleDependency("implementation", "com.fasterxml.jackson.core", "jackson-core", "2.17.2"),
    new GradleDependency(
        "implementation", "com.fasterxml.jackson.core", "jackson-annotations", "2.17.2"),
    new GradleDependency(
        "implementation", "org.apache.httpcomponents.client5", "httpclient5", "5.3.1"),
    new GradleDependency("implementation", "org.bouncycastle", "bcprov-jdk18on", "1.78.1"),
    new GradleDependency("implementation", "org.bouncycastle", "bcpkix-jdk18on", "1.78.1"),
    new GradleDependency("implementation", "io.jsonwebtoken", "jjwt-api", "0.12.6"),
    new GradleDependency("implementation", "io.jsonwebtoken", "jjwt-jackson", "0.12.6"),
    new GradleDependency("implementation", "io.jsonwebtoken", "jjwt-impl", "0.12.6"),
    new GradleDependency(
        "implementation", "org.eclipse.jgit", "org.eclipse.jgit", "6.10.0.202406032230-r"),
    new GradleDependency("implementation", "software.amazon.awssdk", "ssm", "2.21.40"),
    new GradleDependency("implementation", "com.stripe", "stripe-java", "26.3.0"),
    new GradleDependency(
        "implementation", "software.amazon.awssdk", "iam-policy-builder", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "iam", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "cloudwatchlogs", "2.21.40"),
    new GradleDependency("implementation", "software.amazon.awssdk", "lambda", "2.21.40"),
    new GradleDependency("implementation", "org.apache.commons", "commons-text", "1.13.0"),
    new GradleDependency("implementation", "org.matheclipse", "matheclipse-core", "3.0.0"),
    new GradleDependency("implementation", "com.goterl", "lazysodium-java", "5.2.0"),
    new GradleDependency("implementation", "org.gradle", "gradle-tooling-api", "8.12")
  };
  public static final JavaConfig REAL_WORLD_GRADLE_JAVA_CONFIG =
      new JavaConfig("api.poja.io", "21", "21");
  public static final JavaConfig GRADLE_JAVA_CONFIG_FROM_MVN =
      new JavaConfig("api.poja.io", "21", "17");
  public static final GradleBuild GRADLE_BUILD_FROM_MVN =
      new GradleBuild(
          List.of(REAL_WORLD_STRUCTURED_GRADLE_DEPENDENCIES),
          List.of(STRUCTURED_REPOSITORIES_FROM_MVN),
          /*unsupported*/ List.of(),
          GRADLE_JAVA_CONFIG_FROM_MVN);
  public static final GradleSettings GRADLE_SETTINGS_FROM_MVN =
      new GradleSettings("to_import"); // see pendingAppImport()

  public static NetworkingConfig networkingConf() {
    return new NetworkingConfig(REGION, false, "ssm_sg_id", "ssm_subnet1_id", "ssm_subnet2_id");
  }

  public static Set<EnvVar> domainEnvVarsWithTestValues() {
    return Set.of(new EnvVar("ENV1", "dummy", "dummy"), new EnvVar("ENV2", "dummy", "dummy"));
  }
}
