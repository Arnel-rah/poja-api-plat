package api.poja.io.repository;

import api.poja.io.repository.model.PromptAppRequest;
import api.poja.io.repository.model.enums.PromptAppRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptAppRequestRepository extends JpaRepository<PromptAppRequest, String> {
  Optional<PromptAppRequest> findByIdAndOrgId(String id, String orgId);

  List<PromptAppRequest> findByOrgIdAndStatus(String orgId, PromptAppRequestStatus status);
}
