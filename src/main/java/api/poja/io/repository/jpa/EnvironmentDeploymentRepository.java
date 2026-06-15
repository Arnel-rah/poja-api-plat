package api.poja.io.repository.jpa;

import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EnvironmentDeploymentRepository
    extends JpaRepository<AppEnvironmentDeployment, String> {
  @Modifying
  @Query("update AppEnvironmentDeployment a set a.envDeplConfId = ?2 where a.id = ?1")
  void updateEnvDeploymentConfId(String id, String envDeploymentConfId);

  @Modifying
  @Query(
      "update AppEnvironmentDeployment a set a.ghTagName = ?2, a.ghTagMessage = ?3 where a.id = ?1")
  void updateGhTagInfo(String id, String ghTagName, String ghTagMessage);

  @Modifying
  @Query("update AppEnvironmentDeployment a set a.deployedUrl = ?2 where a.id = ?1")
  void updateDeployedUri(String id, String uri);

  @Modifying
  @Query(
      "update AppEnvironmentDeployment a set a.ghWorkflowRunId = ?2, a.ghWorkflowRunAttempt = ?3"
          + " where a.id = ?1")
  void updateWorkflowRunId(String id, String workflowRunId, String workflowRunAttempt);

  Optional<AppEnvironmentDeployment> findByIdAndEnv_EnvironmentType(
      String id, EnvironmentType environmentType);

  Optional<AppEnvironmentDeployment> findByAppIdAndId(String appId, String id);
}
