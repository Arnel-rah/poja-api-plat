package api.poja.io.repository.jpa;

import api.poja.io.repository.model.ApplicationImport;
import api.poja.io.repository.model.enums.ApplicationImportStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationImportRepository extends JpaRepository<ApplicationImport, String> {
  @Modifying
  @Query("update ApplicationImport ai set ai.status = ?2 where ai.id = ?1")
  void updateStatus(String importId, ApplicationImportStatus status);

  @Modifying
  @Query("update ApplicationImport ai set ai.createdAppId = ?2 where ai.id = ?1")
  void updateCreatedApplicationId(String importId, String applicationId);

  List<ApplicationImport> findAllByOrgIdAndArchived(
      String orgId, Pageable pageable, boolean archived);

  Optional<ApplicationImport> findByOrgIdAndIdAndArchived(
      String orgId, String id, boolean archived);

  Optional<ApplicationImport> findByIdAndGithubRepositoryIdAndArchived(
      String id, String githubRepositoryId, boolean archived);

  List<ApplicationImport> findAllByGithubRepositoryIdAndArchived(
      String githubRepositoryId, boolean archived);

  Long countApplicationImportsByUserIdAndStatusInAndArchived(
      String userId, Collection<ApplicationImportStatus> statuses, boolean archived);

  Long countApplicationImportsByUserIdAndStatusInAndIdNotInAndArchived(
      String userId,
      Collection<ApplicationImportStatus> statuses,
      Collection<String> ids,
      boolean archived);
}
