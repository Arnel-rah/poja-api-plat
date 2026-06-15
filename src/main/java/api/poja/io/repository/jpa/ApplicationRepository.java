package api.poja.io.repository.jpa;

import api.poja.io.repository.model.Application;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository("ApplicationRepository")
public interface ApplicationRepository extends JpaRepository<Application, String> {
  Optional<Application> findByIdAndOrgId(String id, String orgId);

  Optional<Application> findByGithubRepositoryIdAndArchived(
      String githubRepositoryId, boolean archived);

  boolean existsByIdAndArchived(String id, boolean archived);

  Optional<Application> findByImportId(String importId);

  boolean existsByGithubRepositoryIdAndArchived(String githubRepositoryId, boolean archived);

  @Query(
      """
select a from Application a inner join Organization o on o.id = a.orgId where o.ownerId = ?1 and a.archived = ?2
""")
  Page<Application> findAllFromOrgsOwnedByUserByCriteria(
      String userId, boolean archived, Pageable pageable);

  @Query(
      """
select count(a.id) from Application a inner join Organization o on o.id = a.orgId where o.ownerId = ?1 and a.archived = ?2
""")
  long countAllFromOrgsOwnedByUserByCriteria(String userId, boolean archived);

  List<Application> findAllByUserIdAndArchived(String userId, boolean archived);

  boolean existsByNameAndArchived(String appName, boolean archived);

  boolean existsByGithubRepositoryId(String ghRepositoryId);

  long countByUserIdAndArchived(String userId, boolean archived);

  @Modifying
  @Query(
      """
      update Application a set a.githubRepositoryUrl = ?2,
      a.githubRepositoryId = ?3  where a.id = ?1""")
  void updateApplicationRepoUrl(String id, String githubRepositoryUrl, String githubRepositoryId);

  @Query(
      """
  select a from Application a where a.orgId = ?1 and a.creationDatetime<= ?2 and (a.archived = false or (a.archived = true and DATE(a.archivedAt) >= ?3))
""")
  List<Application> findAllToComputeBillingForByOrgId(
      String orgId, Instant computeDatetime, LocalDate dateIntervalEnd);

  @Query(
      """
select a from Application a where a.userId = ?1 and (YEAR(a.creationDatetime)*100+MONTH(a.creationDatetime) <= ?2) and (a.archived = false or (a.archived = true and YEAR(a.archivedAt)*100+MONTH(a.archivedAt) >= ?2 or a.archivedAt is null ))
""")
  List<Application> findAllToBillByUserId(String userId, long yearMonth);

  List<Application> findAllByOrgIdAndArchived(String orgId, boolean archived);

  boolean existsByNameAndUserIdAndArchived(String name, String userId, boolean archived);
}
