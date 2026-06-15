package api.poja.io.service;

import api.poja.io.repository.jpa.EventStackResourceRepository;
import api.poja.io.repository.model.EventStackResource;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EventStackResourceService {
  private final EventStackResourceRepository repository;

  public EventStackResource save(EventStackResource eventStackResources) {
    return repository.save(eventStackResources);
  }

  public List<EventStackResource> findAllByEnvironmentId(String environmentId) {
    return repository.findAllByEnvId(environmentId);
  }

  public Optional<EventStackResource> findOneByAppEnvDeplId(String appEnvDeplId) {
    return repository.findOneByAppEnvDeplId(appEnvDeplId);
  }
}
