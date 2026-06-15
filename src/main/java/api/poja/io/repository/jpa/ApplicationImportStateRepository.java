package api.poja.io.repository.jpa;

import api.poja.io.repository.model.ApplicationImportState;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationImportStateRepository
    extends JpaRepository<ApplicationImportState, String> {
  List<ApplicationImportState> findAllByImportId(String importId, Sort sort);
}
