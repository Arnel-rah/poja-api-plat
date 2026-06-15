package api.poja.io.repository.jpa;

import api.poja.io.repository.model.DeploymentState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentStateRepository extends JpaRepository<DeploymentState, String> {
  List<DeploymentState> findAllByAppEnvDeploymentId(String deploymentId, Sort sort);

  Optional<DeploymentState> findTopByAppEnvDeploymentIdOrderByTimestampDesc(String deploymentId);
}
