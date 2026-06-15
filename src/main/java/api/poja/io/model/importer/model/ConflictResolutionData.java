package api.poja.io.model.importer.model;

import api.poja.io.model.importer.analyzer.buildtool.gradle.model.GradleDependency;
import java.util.List;

public record ConflictResolutionData(List<GradleDependency> dependencies) {}
