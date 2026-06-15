package api.poja.io.repository.jpa;

import api.poja.io.repository.model.StorageBucketStackResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageBucketStackResourceRepository
    extends JpaRepository<StorageBucketStackResource, String> {
  List<StorageBucketStackResource> findAllByEnvId(String envId);

  Optional<StorageBucketStackResource> findOneByAppEnvDeplId(String appEnvDeplId);
}
