package api.poja.io.repository.jpa;

import api.poja.io.repository.model.EventStackResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStackResourceRepository extends JpaRepository<EventStackResource, String> {
  List<EventStackResource> findAllByEnvId(String envId);

  Optional<EventStackResource> findOneByAppEnvDeplId(String appEnvDeplId);
}
