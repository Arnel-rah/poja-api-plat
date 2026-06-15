package api.poja.io.repository.jpa;

import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.repository.model.ComputeStackResource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComputeStackResourceRepository
    extends JpaRepository<ComputeStackResource, String> {
  List<ComputeStackResource> findAllByEnvironmentIdOrderByCreationDatetimeDesc(
      String environmentId);

  @Query(
      "select c from ComputeStackResource c inner join Environment e on e.id = c.environmentId"
          + " where e.applicationId = ?1 order by c.creationDatetime desc")
  List<ComputeStackResource> findAllByApplicationIdOrderByCreationDatetimeDesc(
      String applicationId);

  @Query(
      "select c from ComputeStackResource c inner join Environment e on e.id = c.environmentId"
          + " where c.environmentId = ?2 and e.applicationId = ?1 order by c.creationDatetime desc")
  List<ComputeStackResource> findAllByCriteriaOrderByCreationDatetimeDesc(
      String applicationId, String environmentId);

  @Query("select c from ComputeStackResource c where c.appEnvDeplId = ?1")
  Optional<ComputeStackResource> findOneByAppEnvDeplId(String id);

  @Query(
      "SELECT c FROM ComputeStackResource c "
          + "INNER JOIN Environment e ON c.environmentId = e.id "
          + "WHERE e.applicationId = :applicationId "
          + "AND e.environmentType IN :environmentTypes "
          + "AND e.archived = false "
          + "AND c.creationDatetime = ("
          + "    SELECT MAX(c2.creationDatetime) FROM ComputeStackResource c2 "
          + "    WHERE c2.environmentId = e.id"
          + ")")
  List<ComputeStackResource> findLatestByEnvironmentTypes(
      @Param("applicationId") String applicationId,
      @Param("environmentTypes") Set<EnvironmentType> environmentTypes);

  List<ComputeStackResource> findAllByStackIdIn(List<String> stackIds);

  Optional<ComputeStackResource> findTopByEnvironmentIdOrderByCreationDatetimeDesc(String envId);
}
