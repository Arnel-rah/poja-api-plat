package api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread;

import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.error.GBFReadError;
import api.poja.io.model.importer.analyzer.buildtool.gradle.gbfread.warning.GBFReadWarning;
import api.poja.io.model.importer.model.FallibleResult;
import java.io.File;
import java.util.function.Function;

public interface GBFReader<T>
    extends Function<File, FallibleResult<T, GBFReadWarning, GBFReadError>> {}
