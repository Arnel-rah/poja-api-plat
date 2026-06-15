package api.poja.io.repository.jpa;

import api.poja.io.repository.model.OrganizationSetupState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationSetupStateRepository
    extends JpaRepository<OrganizationSetupState, String> {
  List<OrganizationSetupState> findAllByOrgId(String orgId, Sort sort);

  Optional<OrganizationSetupState> findTopByOrgIdOrderByTimestampDesc(String orgId);
}
