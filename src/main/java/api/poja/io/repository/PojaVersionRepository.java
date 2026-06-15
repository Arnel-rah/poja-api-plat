package api.poja.io.repository;

import api.poja.io.model.PojaVersion;
import java.util.List;
import java.util.Optional;

public interface PojaVersionRepository {
  List<PojaVersion> findAll();

  Optional<PojaVersion> findByVersion(String version);
}
