package api.poja.io.model.importer.analyzer.buildtool.gradle.model.repository;

public interface GradleRepository {
  String MAVEN_CENTRAL_URL = "https://repo.maven.apache.org/maven2";
  String GOOGLE_URL = "https://dl.google.com/dl/android/maven2";
  String GRADLE_PLUGIN_PORTAL_URL = "https://plugins.gradle.org/m2";
  String DEFAULT_MAVEN_LOCAL_URL_END = "/.m2/repository";

  String GRADLE_PLUGIN_PORTAL_REPO_NAME = "Gradle Central Plugin Repository";
  String GOOGLE_REPO_NAME = "Google";
  String DEFAULT_MAVEN_CENTRAL_REPO_NAME = "MavenRepo";
  String DEFAULT_MAVEN_LOCAL_REPO_NAME = "MavenLocal";

  String FLAT_DIR_DEFAULT_NAME = "flatDir";
  String MAVEN_REPO_DEFAULT_NAME = "maven";
  String IVY_REPO_DEFAULT_NAME = "ivy";

  /**
   * Returns the String representation of this repository whose format is "repositoryType()" or
   * "repositoryType{ ...attributes }" if !attributes.isEmpty()
   */
  String formatDeclaration();

  RepositoryType type();
}
