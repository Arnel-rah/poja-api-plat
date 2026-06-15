package api.poja.io.repository.jpa;

import api.poja.io.repository.model.ApplicationImportLog;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationImportLogRepository
    extends JpaRepository<ApplicationImportLog, String> {
  List<ApplicationImportLog> findAllByStateId(String StateId, Sort sort);
}
