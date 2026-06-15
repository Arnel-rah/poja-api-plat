package api.poja.io.repository.jpa;

import api.poja.io.repository.model.EnvBuildRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvBuildRequestRepository extends JpaRepository<EnvBuildRequest, String> {
  Optional<EnvBuildRequest> findByAppEnvDeplId(String appEnvDeplId);
}
