package api.poja.io.repository.jpa;

import api.poja.io.endpoint.rest.model.LogQueryStatus;
import api.poja.io.repository.model.LogQuery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LogQueryRepository extends JpaRepository<LogQuery, String> {
  Optional<LogQuery> findByQueryId(String queryId);

  @Modifying
  @Query("update LogQuery l set l.queryId = ?2 where l.id = ?1")
  void updateQueryId(String id, String queryId);

  @Modifying
  @Query("update LogQuery l set l.queryStatus = ?2 where l.id = ?1")
  void updateQueryStatus(String id, LogQueryStatus status);
}
