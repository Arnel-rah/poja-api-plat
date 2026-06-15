package api.poja.io.repository.jpa;

import api.poja.io.repository.model.Stack;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StackRepository extends JpaRepository<Stack, String> {
  Optional<Stack> findByApplicationIdAndEnvironmentIdAndId(
      String applicationId, String environmentId, String id);

  List<Stack> findAllByEnvironmentId(String environmentId);

  List<Stack> findAllByApplicationId(String appId);

  boolean existsByNameAndArchived(String name, boolean archived);

  @Query(
      """
    select count(s) > 0
    from Stack s
    join Application a on a.id = s.applicationId
    where s.name = :name
      and a.userId = :userId
      and s.archived = :archived
""")
  boolean existsByNameAndUserIdAndArchived(String name, String userId, boolean archived);
}
