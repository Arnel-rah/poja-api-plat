package api.poja.io.repository.jpa;

import api.poja.io.repository.model.AppInstallation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppInstallationRepository extends JpaRepository<AppInstallation, String> {
  List<AppInstallation> findAllByUserId(String userId);

  Optional<AppInstallation> findByUserIdAndOwnerGithubLogin(String userId, String ownerGithubLogin);

  Optional<AppInstallation> findByOrgIdAndOwnerGithubLogin(String orgId, String ownerGithubLogin);

  List<AppInstallation> findAllByOrgId(String orgId);
}
