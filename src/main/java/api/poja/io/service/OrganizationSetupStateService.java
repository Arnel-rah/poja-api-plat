package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static java.time.Instant.now;

import api.poja.io.model.OrganizationSetupStateDTO;
import api.poja.io.repository.jpa.OrganizationSetupStateRepository;
import api.poja.io.repository.model.OrganizationSetupState;
import api.poja.io.repository.model.enums.OrganizationSetupStateStatusEnum;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import api.poja.io.service.organization.OrganizationService;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class OrganizationSetupStateService {
  private final OrganizationService organizationService;
  private final OrganizationSetupStateRepository repository;

  public List<OrganizationSetupState> getSortedStatesByOrgId(String orgId) {
    Sort sortByTimestampDesc = Sort.by("timestamp").descending();
    return repository.findAllByOrgId(orgId, sortByTimestampDesc);
  }

  public Optional<OrganizationSetupState> getLatestStateByOrgId(String orgId) {
    return repository.findTopByOrgIdOrderByTimestampDesc(orgId);
  }

  public void save(String orgId, OrganizationSetupStateStatusEnum status) {
    var org = organizationService.getById(orgId);
    var stateMachine =
        new OrganizationSetupStateDTO(org.getId(), getSortedStatesByOrgId(org.getId()));
    try {
      var newState = toState(orgId, status);
      stateMachine.transitionTo(newState);
    } catch (IllegalStateTransitionException e) {
      throw new RuntimeException(e);
    }
    var latestStateOpt = stateMachine.currentState();
    if (latestStateOpt.isPresent()) {
      repository.save(latestStateOpt.get());
    } else {
      log.error(
          "An error occurred when saving user payment setup state: latestState is empty for {}",
          orgId);
    }
  }

  private static OrganizationSetupState toState(
      String orgId, OrganizationSetupStateStatusEnum status) {
    return OrganizationSetupState.builder()
        .timestamp(now())
        .orgId(orgId)
        .progressionStatus(status)
        .executionType(ASYNCHRONOUS)
        .build();
  }
}
