package api.poja.io.model.importer.poja;

import api.poja.io.model.PojaVersion;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleBuild;
import api.poja.io.model.pojaConf.PojaConf;
import api.poja.io.model.pojaConf.conf7.PojaConf7;
import api.poja.io.model.pojaConf.conf8.PojaConf8;
import api.poja.io.model.pojaConf.conf9.PojaConf9;
import api.poja.io.model.pojaConf.factory.PojaConfFactory;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public final class GBFToPojaConf {
  private final PojaConfFactory pojaConfFactory;

  public PojaConf toPojaConf(GradleBuild gradleBuild, PojaVersion pojaVersion) {
    return switch (pojaVersion) {
      case POJA_7 -> toPojaConf7(gradleBuild);
      case POJA_8 -> toPojaConf8(gradleBuild);
      case POJA_9 -> toPojaConf9(gradleBuild);
      default ->
          throw new UnsupportedOperationException("Unsupported poja version: " + pojaVersion);
    };
  }

  private PojaConf7 toPojaConf7(GradleBuild gradleBuild) {
    var pojaConf = pojaConfFactory.default7();
    List<String> dependencies = gradleBuild.dependencies().stream().map(Object::toString).toList();
    List<String> repositories = gradleBuild.repositories().stream().map(Object::toString).toList();

    return pojaConf.toBuilder()
        .general(
            pojaConf.general().toBuilder()
                .customJavaDeps(dependencies)
                .customJavaRepositories(repositories)
                .build())
        .build();
  }

  private PojaConf8 toPojaConf8(GradleBuild gradleBuild) {
    var pojaConf = pojaConfFactory.default8();
    List<String> dependencies = gradleBuild.dependencies().stream().map(Object::toString).toList();
    List<String> repositories = gradleBuild.repositories().stream().map(Object::toString).toList();

    return pojaConf.toBuilder()
        .general(
            pojaConf.general().toBuilder()
                .customJavaDeps(dependencies)
                .customJavaRepositories(repositories)
                .build())
        .build();
  }

  private PojaConf9 toPojaConf9(GradleBuild gradleBuild) {
    var pojaConf = pojaConfFactory.default9();
    List<String> dependencies = gradleBuild.dependencies().stream().map(Object::toString).toList();
    List<String> repositories = gradleBuild.repositories().stream().map(Object::toString).toList();

    return pojaConf.toBuilder()
        .general(
            pojaConf.general().toBuilder()
                .customJavaDeps(dependencies)
                .customJavaRepositories(repositories)
                .build())
        .build();
  }
}
