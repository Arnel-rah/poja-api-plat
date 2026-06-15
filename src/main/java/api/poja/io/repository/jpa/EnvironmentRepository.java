package api.poja.io.repository.jpa;

import api.poja.io.endpoint.rest.model.Environment.StatusEnum;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.repository.model.Environment;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, String> {
  @EntityGraph(attributePaths = {"deployments"})
  List<Environment> findAllByApplicationIdAndArchived(String applicationId, boolean isArchived);

  Optional<Environment> findFirstByApplicationIdAndEnvironmentTypeAndArchived(
      String applicationId, EnvironmentType environmentType, boolean isArchived);

  Optional<Environment> findFirstByApplicationId(String applicationId);

  @EntityGraph(attributePaths = {"deployments"})
  @Query(
      "SELECT e FROM Environment e INNER JOIN Application a ON e.applicationId = a.id WHERE "
          + " a.orgId = ?1 AND  e.applicationId = ?2 AND e.id = ?3")
  Optional<Environment> findByCriteria(String orgId, String appId, String id);

  List<Environment> findAllByApplicationIdAndStatusAndArchived(
      String applicationId, StatusEnum status, boolean archived);

  @Modifying
  @Query(
      """
      update Environment e set e.status = ?2 where e.id = ?1
      """)
  void updateStatus(String id, StatusEnum status);

  @Modifying
  @Query(
      "update Environment e set e.status = ?2 where e.id in (select e1.id from Environment e1 where"
          + " e1.applicationId = ?1 and e1.archived = false)")
  void updateUnarchivedStatusByApplicationId(String applicationId, StatusEnum status);

  List<Environment> findAllByApplicationId(String applicationId);

  // 1 we check billing one last time a day after archival to ensure we've billed all use time
  @Query(
      """
select e from Environment e where e.applicationId = ?1 and e.creationDatetime <= ?2 and (e.archived = false or (e.archived = true and (DATE(e.archivedAt)) >= ?3))
""")
  List<Environment> findAllEnvsToComputeBillingForByApplicationId(
      String applicationId, Instant computeDatetime, LocalDate endDate);
}
