package api.poja.io.repository.jpa;

import api.poja.io.repository.model.ApplicationImportGhaRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationImportGhaRunRepository
    extends JpaRepository<ApplicationImportGhaRun, String> {
  List<ApplicationImportGhaRun> findAllByAppImportId(String appImportId);
}
