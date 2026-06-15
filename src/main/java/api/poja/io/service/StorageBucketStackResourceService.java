package api.poja.io.service;

import api.poja.io.repository.jpa.StorageBucketStackResourceRepository;
import api.poja.io.repository.model.StorageBucketStackResource;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StorageBucketStackResourceService {
  private final StorageBucketStackResourceRepository repository;

  public StorageBucketStackResource save(StorageBucketStackResource storageBucketStackResource) {
    return repository.save(storageBucketStackResource);
  }

  public List<StorageBucketStackResource> findAllByEnvId(String envId) {
    return repository.findAllByEnvId(envId);
  }

  public Optional<StorageBucketStackResource> findOneByAppEnvDeplId(String appEnvDeplId) {
    return repository.findOneByAppEnvDeplId(appEnvDeplId);
  }
}
