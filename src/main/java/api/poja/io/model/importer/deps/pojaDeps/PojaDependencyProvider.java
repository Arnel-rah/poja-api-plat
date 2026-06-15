package api.poja.io.model.importer.deps.pojaDeps;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PojaDependencyProvider {
  List<GradleDependency> defaultDependencies();

  List<GradleDependency> additionalDependencies();

  List<GradleDependency> allDependencies();

  Set<String> defaultDependencyKeys();

  Map<String, GradleDependency> additionalDependenciesMap();

  Map<String, GradleDependency> allDependenciesMap();
}
