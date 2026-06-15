package api.poja.io.repository.jpa;

import api.poja.io.repository.model.AppSetupState;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSetupStateRepository extends JpaRepository<AppSetupState, String> {
  List<AppSetupState> findAllByOrgIdAndAppId(String orgId, String appId, Sort sort);
}
