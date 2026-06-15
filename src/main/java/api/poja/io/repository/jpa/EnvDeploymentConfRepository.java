package api.poja.io.repository.jpa;

import api.poja.io.repository.model.EnvDeploymentConf;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EnvDeploymentConfRepository extends JpaRepository<EnvDeploymentConf, String> {
  Optional<EnvDeploymentConf> findTopByEnvIdOrderByCreationDatetimeDesc(String envId);

  @Query(
      """
select e from EnvDeploymentConf e inner join AppEnvironmentDeployment a on e.id = a.envDeplConfId where a.id = ?1
""")
  Optional<EnvDeploymentConf> findByAppEnvDeplId(String appEnvDeplId);
}
