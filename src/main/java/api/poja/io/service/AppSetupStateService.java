package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static java.time.Instant.now;

import api.poja.io.model.AppSetupStateMachine;
import api.poja.io.repository.jpa.AppSetupStateRepository;
import api.poja.io.repository.model.AppSetupState;
import api.poja.io.repository.model.enums.AppSetupStateEnum;
import api.poja.io.repository.model.workflows.exception.IllegalStateTransitionException;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppSetupStateService {
  private AppSetupStateRepository applicationSetupStateRepository;

  public List<AppSetupState> getSortedStatesByOrgIdAndAppId(String orgId, String appId) {
    var sortByTimestampAsc = Sort.by("timestamp").descending();
    return applicationSetupStateRepository.findAllByOrgIdAndAppId(orgId, appId, sortByTimestampAsc);
  }

  public AppSetupState save(String orgId, String appId, AppSetupStateEnum status)
      throws IllegalStateTransitionException {
    var states = getSortedStatesByOrgIdAndAppId(orgId, appId);
    var sm = new AppSetupStateMachine(states);
    var newState = toState(orgId, appId, status);
    var toSave = sm.transitionTo(newState);
    return applicationSetupStateRepository.save(toSave);
  }

  public List<AppSetupState> saveAll(
      String orgId, String appId, Collection<AppSetupStateEnum> list) {
    return list.stream().map(s -> save(orgId, appId, s)).toList();
  }

  private static AppSetupState toState(String orgId, String appId, AppSetupStateEnum status) {
    return AppSetupState.builder()
        .timestamp(now())
        .orgId(orgId)
        .appId(appId)
        .progressionStatus(status)
        .executionType(ASYNCHRONOUS)
        .build();
  }
}
