package api.poja.io.model.importer.deps.pojaDeps;

import static api.poja.io.model.PojaVersion.LATEST;

import api.poja.io.model.PojaVersion;
import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import api.poja.io.model.importer.deps.pojaDeps.versions.Poja6DependencyProvider;
import api.poja.io.model.importer.deps.pojaDeps.versions.Poja8DependencyProvider;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PojaDependencies {
  private final PojaDependencyProvider provider;

  public static PojaDependencies forVersion(PojaVersion version) {
    var provider =
        switch (version) {
          case POJA_1, POJA_2, POJA_3, POJA_4, POJA_5, POJA_6, POJA_7 ->
              new Poja6DependencyProvider();
          case POJA_8, POJA_9 -> new Poja8DependencyProvider();
        };
    return new PojaDependencies(provider);
  }

  public static PojaDependencies latest() {
    return forVersion(LATEST);
  }

  public List<GradleDependency> defaultDependencies() {
    return provider.defaultDependencies();
  }

  public List<GradleDependency> additionalDependencies() {
    return provider.additionalDependencies();
  }

  public List<GradleDependency> allDependencies() {
    return provider.allDependencies();
  }

  public Set<String> defaultDependencyKeys() {
    return provider.defaultDependencyKeys();
  }

  public Map<String, GradleDependency> additionalDependenciesMap() {
    return provider.additionalDependenciesMap();
  }

  public Map<String, GradleDependency> allDependenciesMap() {
    return provider.allDependenciesMap();
  }
}
