package api.poja.io.repository.impl;

import static java.util.EnumSet.allOf;

import api.poja.io.model.PojaVersion;
import api.poja.io.repository.PojaVersionRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PojaVersionRepositoryImpl implements PojaVersionRepository {

  private static final List<PojaVersion> ALL_POJA_VERSIONS =
      allOf(PojaVersion.class).stream().toList();

  @Override
  public List<PojaVersion> findAll() {
    return ALL_POJA_VERSIONS;
  }

  @Override
  public Optional<PojaVersion> findByVersion(String version) {
    return ALL_POJA_VERSIONS.stream()
        .filter(pojaVersion -> pojaVersion.toHumanReadableValue().equals(version))
        .findFirst();
  }
}
