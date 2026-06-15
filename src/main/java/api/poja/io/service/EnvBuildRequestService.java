package api.poja.io.service;

import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.EnvBuildRequestRepository;
import api.poja.io.repository.model.EnvBuildRequest;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EnvBuildRequestService {
  private final EnvBuildRequestRepository repository;

  public EnvBuildRequest save(EnvBuildRequest envBuildRequest) {
    return repository.save(envBuildRequest);
  }

  public Optional<EnvBuildRequest> findById(String id) {
    return repository.findById(id);
  }

  public EnvBuildRequest getByAppEnvDeplId(String appEnvDeplId) {
    return repository
        .findByAppEnvDeplId(appEnvDeplId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "EnvBuildRequest.appEnvDeplId = " + appEnvDeplId + " not found."));
  }

  public boolean existsById(String id) {
    return repository.existsById(id);
  }
}
