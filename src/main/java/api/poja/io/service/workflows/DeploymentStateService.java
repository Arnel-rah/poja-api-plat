package api.poja.io.service.workflows;

import static api.poja.io.endpoint.rest.mapper.ComputeStackResourceMapper.distinctByKey;
import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;

import api.poja.io.endpoint.rest.model.DeploymentStateEnum;
import api.poja.io.model.exception.NotFoundException;
import api.poja.io.repository.jpa.DeploymentStateRepository;
import api.poja.io.repository.model.DeploymentState;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import api.poja.io.service.AppEnvironmentDeploymentService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class DeploymentStateService {
  private final DeploymentStateRepository repository;
  private final AppEnvironmentDeploymentService appEnvironmentDeploymentService;

  /**
   * fixme: progressionStatus duplication causes error when mapping this to rest, which is the
   * correct behaviour. A guard already exists to avoid any incorrect state transitions but one have
   * been discovered: INDEPENDENT_STACK_DEPLOYED. Remove filter(distinct) when it is fixed
   */
  public List<DeploymentState> getSortedDeploymentStatesByDeploymentId(
      String orgId, String appId, String deploymentId) {
    Sort sortByTimestampAsc = Sort.by("timestamp").ascending();
    return repository.findAllByAppEnvDeploymentId(deploymentId, sortByTimestampAsc).stream()
        .filter(distinctByKey(DeploymentState::getProgressionStatus))
        .toList();
  }

  public Optional<DeploymentState> getOptionalLatestDeploymentStateByDeploymentId(
      String appEnvDeploymentId) {
    return repository.findTopByAppEnvDeploymentIdOrderByTimestampDesc(appEnvDeploymentId);
  }

  public DeploymentState getLatestDeploymentStateByDeploymentId(String appEnvDeploymentId) {
    return getOptionalLatestDeploymentStateByDeploymentId(appEnvDeploymentId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "deployment state not found for appEnvDeploymentId: " + appEnvDeploymentId));
  }

  public void save(String appId, String appEnvDeploymentId, DeploymentStateEnum status) {
    var appEnvDepl = appEnvironmentDeploymentService.getByAppIdAndId(appId, appEnvDeploymentId);
    try {
      var newState = fromDepl(appEnvDeploymentId, status);
      appEnvDepl.transitionTo(newState);
    } catch (IllegalStateTransitionException e) {
      throw new RuntimeException(e);
    }
    var latestStateOpt = appEnvDepl.currentState();
    if (latestStateOpt.isPresent()) {
      repository.save(latestStateOpt.get());
    } else {
      log.error(
          "An error occurred when saving deployment state: latestState is empty for {}",
          appEnvDeploymentId);
    }
  }

  private static DeploymentState fromDepl(String appEnvDeplId, DeploymentStateEnum status) {
    return DeploymentState.builder()
        .timestamp(Instant.now())
        .appEnvDeploymentId(appEnvDeplId)
        .progressionStatus(status)
        .executionType(ASYNCHRONOUS)
        .build();
  }
}
