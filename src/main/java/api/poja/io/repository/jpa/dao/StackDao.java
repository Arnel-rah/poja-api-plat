package api.poja.io.repository.jpa.dao;

import static java.util.Optional.empty;

import api.poja.io.endpoint.rest.model.StackType;
import api.poja.io.repository.model.Stack;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class StackDao {
  private final EntityManager entityManager;

  public List<Stack> findAllByCriteria(
      String orgId,
      String applicationId,
      String environmentId,
      String appEnvDeplId,
      Pageable pageable) {
    assert orgId != null;
    assert applicationId != null;
    assert environmentId != null;
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Stack> query = builder.createQuery(Stack.class);
    Root<Stack> root = query.from(Stack.class);
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.equal(root.get("applicationId"), applicationId));
    predicates.add(builder.equal(root.get("environmentId"), environmentId));
    predicates.add(builder.equal(root.get("archived"), false));
    if (appEnvDeplId != null) {
      predicates.add(builder.equal(root.get("appEnvDeplId"), appEnvDeplId));
    }
    query
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder))
        .where(predicates.toArray(new Predicate[0]));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  public Optional<Stack> findLatestByCriteria(
      String applicationId, String environmentId, StackType type, boolean archived) {
    assert applicationId != null;
    assert environmentId != null;
    assert type != null;
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Stack> query = builder.createQuery(Stack.class);
    Root<Stack> root = query.from(Stack.class);
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.equal(root.get("applicationId"), applicationId));
    predicates.add(builder.equal(root.get("environmentId"), environmentId));
    predicates.add(builder.equal(root.get("type"), type));
    predicates.add(builder.equal(root.get("archived"), archived));

    query
        .where(predicates.toArray(new Predicate[0]))
        .orderBy(builder.desc(root.get("creationDatetime")));
    try {
      var stacks = entityManager.createQuery(query).getResultList();
      if (stacks.isEmpty()) {
        return empty();
      }
      return Optional.ofNullable(stacks.getFirst());
    } catch (NoResultException e) {
      return empty();
    }
  }
}
