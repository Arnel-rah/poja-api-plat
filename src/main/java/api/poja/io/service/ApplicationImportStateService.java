package api.poja.io.service;

import static api.poja.io.endpoint.rest.model.ExecutionType.ASYNCHRONOUS;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobName.POST_TRANSFORMATION_PING_TEST;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobName.POST_TRANSFORMATION_TEST;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobName.PRE_TRANSFORMATION_TEST;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobStatus.FAILURE;
import static api.poja.io.endpoint.rest.model.GhWorkflowJobStatus.SUCCESS;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_FAILED;
import static api.poja.io.repository.model.enums.ApplicationImportStateStatus.TEST_PING_ENDPOINT_SUCCESSFUL;
import static java.time.Instant.now;

import api.poja.io.endpoint.event.EventProducer;
import api.poja.io.endpoint.event.model.AppImportProcessed;
import api.poja.io.endpoint.event.model.BuildToolConversionRequested;
import api.poja.io.endpoint.event.model.PostTransformationPingTestRequested;
import api.poja.io.endpoint.rest.model.UpdateApplicationImportStatesRequestBody;
import api.poja.io.model.exception.BadRequestException;
import api.poja.io.model.importer.model.ApplicationImportLog;
import api.poja.io.repository.jpa.ApplicationImportStateRepository;
import api.poja.io.repository.model.ApplicationImportState;
import api.poja.io.repository.model.enums.ApplicationImportStateStatus;
import api.poja.io.service.workflows.appImportState.ApplicationImportStateMachine;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ApplicationImportStateService {
  private final ApplicationImportStateRepository repository;
  private final ApplicationImportService importService;
  private final ApplicationImportGhaRunService applicationImportGhaRunService;
  private final ApplicationImportLogService logService;
  private final EventProducer eventProducer;

  public List<ApplicationImportState> getStatesByImportId(String importId) {
    var sortByTimestampAsc = Sort.by("timestamp").descending();
    return repository.findAllByImportId(importId, sortByTimestampAsc);
  }

  @Transactional
  public List<ApplicationImportState> updateApplicationImportStates(
      String orgId,
      String importId,
      UpdateApplicationImportStatesRequestBody updateAppImportReqBody) {
    if (PRE_TRANSFORMATION_TEST.equals(updateAppImportReqBody.getJobName())) {
      if (SUCCESS.equals(updateAppImportReqBody.getJobStatus())) {
        updateState(importId, PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL);
        applicationImportGhaRunService.save(importId, updateAppImportReqBody);

        var event = new BuildToolConversionRequested(orgId, importId);
        eventProducer.accept(List.of(event));

        return getStatesByImportId(importId);
      }

      if (FAILURE.equals(updateAppImportReqBody.getJobStatus())) {
        updateState(
            importId,
            PRE_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED,
            List.of(ApplicationImportLog.error("Pre transformation test run failed")));

        return getStatesByImportId(importId);
      }

      throw new BadRequestException("Job status must be either SUCCESS or FAILURE");
    }
    if (POST_TRANSFORMATION_TEST.equals(updateAppImportReqBody.getJobName())) {
      if (SUCCESS.equals(updateAppImportReqBody.getJobStatus())) {
        updateState(importId, POST_TRANSFORMATION_TEST_RUN_VERIFICATION_SUCCESSFUL);
        applicationImportGhaRunService.save(importId, updateAppImportReqBody);

        var event = new PostTransformationPingTestRequested(importId);
        eventProducer.accept(List.of(event));

        return getStatesByImportId(importId);
      }

      if (FAILURE.equals(updateAppImportReqBody.getJobStatus())) {
        updateState(
            importId,
            POST_TRANSFORMATION_TEST_RUN_VERIFICATION_FAILED,
            List.of(ApplicationImportLog.error("Post transformation test run failed")));

        return getStatesByImportId(importId);
      }

      throw new BadRequestException("Job status must be either SUCCESS or FAILURE");
    }

    if (POST_TRANSFORMATION_PING_TEST.equals(updateAppImportReqBody.getJobName())) {
      if (SUCCESS.equals(updateAppImportReqBody.getJobStatus())) {
        updateState(importId, TEST_PING_ENDPOINT_SUCCESSFUL);
        applicationImportGhaRunService.save(importId, updateAppImportReqBody);

        var event = new AppImportProcessed(orgId, importId);
        eventProducer.accept(List.of(event));

        return getStatesByImportId(importId);
      }

      if (FAILURE.equals(updateAppImportReqBody.getJobStatus())) {
        updateState(
            importId,
            TEST_PING_ENDPOINT_FAILED,
            List.of(ApplicationImportLog.error("Ping endpoint test failed")));

        return getStatesByImportId(importId);
      }

      throw new BadRequestException("Job status must be either SUCCESS or FAILURE");
    }

    throw new BadRequestException("Unknown job name: " + updateAppImportReqBody.getJobName());
  }

  @Transactional
  public ApplicationImportState save(
      String importId, ApplicationImportStateStatus status, List<ApplicationImportLog> logs) {
    var states = getStatesByImportId(importId);
    var sm = new ApplicationImportStateMachine(states);
    var newState = toState(importId, status);
    var toSave = sm.transitionTo(newState);
    var savedState = repository.save(toSave);
    if (logs != null && !logs.isEmpty()) {
      logService.saveAll(logs, savedState.getId());
    }
    return savedState;
  }

  private static ApplicationImportState toState(
      String importId, ApplicationImportStateStatus status) {
    return ApplicationImportState.builder()
        .timestamp(now())
        .importId(importId)
        .progressionStatus(status)
        .executionType(ASYNCHRONOUS)
        .build();
  }

  @Transactional
  public void updateState(
      String importId, ApplicationImportStateStatus stateStatus, List<ApplicationImportLog> logs) {
    save(importId, stateStatus, logs);
    var newStatus = stateStatus.toImportStatus();
    importService.updateStatus(importId, newStatus);
  }

  @Transactional
  public void updateState(String importId, ApplicationImportStateStatus stateStatus) {
    updateState(importId, stateStatus, List.of());
  }
}
