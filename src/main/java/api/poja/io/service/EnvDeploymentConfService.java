package api.poja.io.service;

import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.EnvDeploymentConfRepository;
import api.poja.io.repository.model.EnvDeploymentConf;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class EnvDeploymentConfService {
  private final EnvDeploymentConfRepository repository;

  public EnvDeploymentConf save(EnvDeploymentConf envDeploymentConf) {
    return repository.save(envDeploymentConf);
  }

  public EnvDeploymentConf getLatestByEnvId(String envId) {
    return repository
        .findTopByEnvIdOrderByCreationDatetimeDesc(envId)
        .orElseThrow(() -> new NotFoundException("No deployment conf found for env id=" + envId));
  }

  public EnvDeploymentConf getById(String id) {
    return findById(id)
        .orElseThrow(
            () -> new NotFoundException("EnvDeploymentConf.Id = " + id + " was not found."));
  }

  public Optional<EnvDeploymentConf> findById(String id) {
    return repository.findById(id);
  }

  public Optional<EnvDeploymentConf> findByAppEnvDeplId(String appEnvDeplId) {
    return repository.findByAppEnvDeplId(appEnvDeplId);
  }

  public EnvDeploymentConf getByAppEnvDeplId(String appEnvDeplId) {
    return repository
        .findByAppEnvDeplId(appEnvDeplId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "no EnvDeploymentConf found for appEnvDeplId=" + appEnvDeplId));
  }
}
