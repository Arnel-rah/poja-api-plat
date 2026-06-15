package api.poja.io.repository.jpa.dao;

import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.endpoint.rest.model.EnvironmentType;
import api.poja.io.repository.model.AppEnvironmentDeployment;
import api.poja.io.repository.model.DeploymentState;
import api.poja.io.repository.model.Environment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@AllArgsConstructor
public class EnvironmentDeploymentDao {
  private final EntityManager entityManager;

  public List<AppEnvironmentDeployment> findAllByCriteria(
      String appId,
      EnvironmentType envType,
      Instant startDatetime,
      Instant endDatetime,
      Pageable pageable) {
    assert appId != null;
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<AppEnvironmentDeployment> query =
        builder.createQuery(AppEnvironmentDeployment.class);
    Root<AppEnvironmentDeployment> root = query.from(AppEnvironmentDeployment.class);
    Join<AppEnvironmentDeployment, Environment> envDeplEnvJoin = root.join("env");

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.and(builder.equal(root.get("appId"), appId)));
    if (startDatetime != null) {
      predicates.add(
          builder.and(builder.greaterThanOrEqualTo(root.get("creationDatetime"), startDatetime)));
    }
    if (endDatetime != null) {
      predicates.add(
          builder.and(builder.lessThanOrEqualTo(root.get("creationDatetime"), endDatetime)));
    }
    predicates.add(builder.and(builder.equal(envDeplEnvJoin.get("archived"), false)));
    if (envType != null) {
      predicates.add(builder.and(builder.equal(envDeplEnvJoin.get("environmentType"), envType)));
    }
    query
        .where(predicates.toArray(new Predicate[0]))
        .orderBy(builder.desc(root.get("creationDatetime")));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  public List<AppEnvironmentDeployment> findAllByCriteria(
      String appId,
      String envId,
      List<DeploymentStateEnum> progressionStatuses,
      Pageable pageable) {
    assert appId != null;
    assert envId != null;
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<AppEnvironmentDeployment> query = cb.createQuery(AppEnvironmentDeployment.class);
    Root<AppEnvironmentDeployment> root = query.from(AppEnvironmentDeployment.class);
    Join<AppEnvironmentDeployment, Environment> envDeplEnvJoin = root.join("env");
    List<Predicate> predicates = new ArrayList<>();

    if (progressionStatuses != null) {
      // Subquery to find the latest creationDatetime for each AppEnvironmentDeployment
      Subquery<Instant> subquery = query.subquery(Instant.class);
      Root<DeploymentState> subRoot = subquery.from(DeploymentState.class);
      Path<Instant> timestamp = subRoot.get("timestamp");

      subquery
          .select(cb.greatest(timestamp))
          .where(cb.equal(subRoot.get("appEnvDeploymentId"), root.get("id")));

      // Join states in the main query and filter by COMPLETED and latest datetime
      Join<AppEnvironmentDeployment, DeploymentState> states = root.join("states");
      Predicate latestStatePredicate =
          cb.and(
              cb.equal(states.get("timestamp"), subquery),
              states.get("progressionStatus").in(progressionStatuses));
      predicates.add(latestStatePredicate);
    }

    predicates.add(cb.and(cb.equal(root.get("appId"), appId)));
    predicates.add(cb.and(cb.equal(envDeplEnvJoin.get("id"), envId)));
    query
        .where(predicates.toArray(new Predicate[0]))
        .orderBy(cb.desc(root.get("creationDatetime")));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  public List<AppEnvironmentDeployment> findAllByCriteria(
      String appId, String envId, List<DeploymentStateEnum> progressionStatuses) {
    assert appId != null;
    assert envId != null;
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<AppEnvironmentDeployment> query = cb.createQuery(AppEnvironmentDeployment.class);
    Root<AppEnvironmentDeployment> root = query.from(AppEnvironmentDeployment.class);
    Join<AppEnvironmentDeployment, Environment> envDeplEnvJoin = root.join("env");
    List<Predicate> predicates = new ArrayList<>();

    if (progressionStatuses != null) {
      // Subquery to find the latest creationDatetime for each AppEnvironmentDeployment
      Subquery<Instant> subquery = query.subquery(Instant.class);
      Root<DeploymentState> subRoot = subquery.from(DeploymentState.class);
      Path<Instant> timestamp = subRoot.get("timestamp");

      subquery
          .select(cb.greatest(timestamp))
          .where(cb.equal(subRoot.get("appEnvDeploymentId"), root.get("id")));

      // Join states in the main query and filter by COMPLETED and latest datetime
      Join<AppEnvironmentDeployment, DeploymentState> states = root.join("states");
      Predicate latestStatePredicate =
          cb.and(
              cb.equal(states.get("timestamp"), subquery),
              states.get("progressionStatus").in(progressionStatuses));
      predicates.add(latestStatePredicate);
    }

    predicates.add(cb.and(cb.equal(root.get("appId"), appId)));
    predicates.add(cb.and(cb.equal(envDeplEnvJoin.get("id"), envId)));
    query.where(predicates.toArray(new Predicate[0])).orderBy(cb.asc(root.get("creationDatetime")));
    return entityManager.createQuery(query).getResultList();
  }
}
