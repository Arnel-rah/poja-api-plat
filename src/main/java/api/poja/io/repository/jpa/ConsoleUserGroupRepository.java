package api.poja.io.repository.jpa;

import api.poja.io.repository.model.ConsoleUserGroup;
import java.util.Optional;
import java.util.Stack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConsoleUserGroupRepository extends JpaRepository<ConsoleUserGroup, String> {
  @Query(
      "select cug from ConsoleUserGroup cug where cug.orgId = ?1 and cug.archived = false and"
          + " cug.available = true")
  Stack<ConsoleUserGroup> findOneByOrgIdAndCurrentIsTrueAndArchivedIsFalse(String orgId);

  @Query(
      "select count(c.id) from ConsoleUserGroup c inner join Organization o on c.orgId = o.id inner"
          + " join User u on o.ownerId=u.id where o.ownerId = ?1 and u.id = ?1 and c.archived ="
          + " false group by u.id")
  Optional<Long> countByUserId(String userId);

  @Query(
      "select count(c.id) from ConsoleUserGroup c inner join Organization o on c.orgId = o.id where"
          + " o.id = ?1 and c.archived = false group by c.orgId")
  Optional<Long> countByOrgId(String orgId);
}
