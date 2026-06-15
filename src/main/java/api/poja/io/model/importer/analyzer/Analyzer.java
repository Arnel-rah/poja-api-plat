package api.poja.io.model.importer.analyzer;

import api.poja.io.model.importer.model.UnknownApplication;

public interface Analyzer {
  Result analyze(UnknownApplication unknownApplication);
}
